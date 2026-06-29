<?php
/**
 * Skat Taxi to SMS Agent - REST API Bridge & Dashboard
 * 
 * Bu ko'prik server Skat Taxi serveridan keladigan SMS yuborish so'rovlarini qabul qilib,
 * ma'lumotlar bazasida navbatga qo'yadi va drayverlar telefonidagi SMS Agent ilovasi
 * uchun REST API taqdim etadi.
 * 
 * SQLite ma'lumotlar bazasidan foydalanadi (avtomatik yaratiladi).
 */

error_reporting(E_ALL);
ini_set('display_errors', 0);
date_default_timezone_set('Asia/Tashkent');

define('API_KEY', '7e4cd8f3-72e2-4ed3-afcf-64274679cd86');
define('DB_FILE', __DIR__ . '/sms_agent_gateway.db');

// --- DATABASE INITIALIZATION ---
function getDbConnection() {
    $dbExists = file_exists(DB_FILE);
    $pdo = new PDO('sqlite:' . DB_FILE);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    
    if (!$dbExists) {
        // SMS-lar jadvali
        $pdo->exec("CREATE TABLE IF NOT EXISTS sms_queue (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            external_id TEXT UNIQUE,
            phone_number TEXT,
            message TEXT,
            status TEXT DEFAULT 'PENDING',
            agent_id TEXT,
            error_code INTEGER DEFAULT 0,
            error_message TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )");
        
        // Agentlar (Drayverlar) holati jadvali
        $pdo->exec("CREATE TABLE IF NOT EXISTS agents (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            agent_id TEXT UNIQUE,
            device_name TEXT,
            ip_address TEXT,
            last_heartbeat DATETIME,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )");
    }
    return $pdo;
}

$pdo = getDbConnection();

// --- ROUTING ENGINE ---
$requestUri = $_SERVER['REQUEST_URI'];
$basePath = dirname($_SERVER['SCRIPT_NAME']);
$route = str_replace($basePath, '', $requestUri);
$route = parse_url($route, PHP_URL_PATH);
$route = trim($route, '/');
$method = $_SERVER['REQUEST_METHOD'];

// Helper: JSON javob yuborish
function sendJsonResponse($data, $statusCode = 200) {
    header('Content-Type: application/json; charset=utf-8');
    http_response_code($statusCode);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// Helper: API kalitni tekshirish
function checkAuthorization() {
    $headers = getallheaders();
    $authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : '';
    if (empty($authHeader) && isset($_GET['api_key'])) {
        $authHeader = 'Bearer ' . $_GET['api_key'];
    }
    
    $expected = 'Bearer ' . API_KEY;
    if ($authHeader !== $expected) {
        sendJsonResponse(['status' => 'ERROR', 'message' => 'Ruxsat berilmagan (Unauthorized)'], 401);
    }
}

// =========================================================================
// API ENDPOINTS (MOBILE APP & SKAT CALLS)
// =========================================================================

// 1. Skat Taxi uchun SMS qabul qilish Gateway API
// GET/POST /send (e.g. /send?phone=998901234567&text=Message&api_key=...)
if ($route === 'send') {
    $phone = isset($_REQUEST['phone']) ? $_REQUEST['phone'] : '';
    $text = isset($_REQUEST['text']) ? $_REQUEST['text'] : '';
    $key = isset($_REQUEST['key']) ? $_REQUEST['key'] : (isset($_REQUEST['api_key']) ? $_REQUEST['api_key'] : '');
    
    if ($key !== API_KEY) {
        sendJsonResponse(['status' => 'ERROR', 'message' => 'API Key noto\'g\'ri'], 401);
    }
    
    if (empty($phone) || empty($text)) {
        sendJsonResponse(['status' => 'ERROR', 'message' => 'Telefon raqami yoki xabar matni bo\'sh'], 400);
    }
    
    $externalId = 'skat_' . uniqid() . '_' . rand(100, 999);
    
    try {
        $stmt = $pdo->prepare("INSERT INTO sms_queue (external_id, phone_number, message) VALUES (?, ?, ?)");
        $stmt->execute([$externalId, $phone, $text]);
        sendJsonResponse([
            'status' => 'DONE', 
            'response' => [
                'uuid' => $externalId, 
                'message' => 'SMS muvaffaqiyatli navbatga qo\'shildi'
            ]
        ]);
    } catch (Exception $e) {
        sendJsonResponse(['status' => 'ERROR', 'message' => $e->getMessage()], 500);
    }
}

// 2. Mobile App: Agentni ro'yxatdan o'tkazish
// POST /api/v1/agent/register
if ($route === 'api/v1/agent/register' && $method === 'POST') {
    checkAuthorization();
    $input = json_decode(file_get_contents('php://input'), true);
    $agentId = isset($input['agent_id']) ? $input['agent_id'] : 'unknown_agent';
    $deviceName = isset($input['device_name']) ? $input['device_name'] : 'Android Device';
    $ip = $_SERVER['REMOTE_ADDR'];
    
    $stmt = $pdo->prepare("INSERT OR REPLACE INTO agents (agent_id, device_name, ip_address, last_heartbeat) VALUES (?, ?, ?, datetime('now'))");
    $stmt->execute([$agentId, $deviceName, $ip]);
    
    sendJsonResponse(['status' => 'DONE', 'message' => 'Agent muvaffaqiyatli ro\'yxatdan o\'tdi']);
}

// 3. Mobile App: Heartbeat yuborish
// PUT /api/v1/agent/{agentId}/heartbeat
if (preg_match('/^api\/v1\/agent\/([^\/]+)\/heartbeat$/', $route, $matches) && $method === 'PUT') {
    checkAuthorization();
    $agentId = $matches[1];
    $ip = $_SERVER['REMOTE_ADDR'];
    
    $stmt = $pdo->prepare("UPDATE agents SET last_heartbeat = datetime('now'), ip_address = ? WHERE agent_id = ?");
    $stmt->execute([$ip, $agentId]);
    
    sendJsonResponse(['status' => 'DONE', 'message' => 'Heartbeat qabul qilindi']);
}

// 4. Mobile App: Kutilayotgan SMS-larni olish
// GET /api/v1/agent/{agentId}/pending-sms
if (preg_match('/^api\/v1\/agent\/([^\/]+)\/pending-sms$/', $route, $matches) && $method === 'GET') {
    checkAuthorization();
    $agentId = $matches[1];
    
    // Agent statusini avtomatik ro'yxatdan o'tkazish/yangilash (Heartbeat)
    $ip = $_SERVER['REMOTE_ADDR'];
    $checkAgent = $pdo->prepare("SELECT id FROM agents WHERE agent_id = ?");
    $checkAgent->execute([$agentId]);
    if ($checkAgent->fetch()) {
        $updateAgent = $pdo->prepare("UPDATE agents SET last_heartbeat = datetime('now'), ip_address = ? WHERE agent_id = ?");
        $updateAgent->execute([$ip, $agentId]);
    } else {
        $insertAgent = $pdo->prepare("INSERT INTO agents (agent_id, device_name, ip_address, last_heartbeat) VALUES (?, 'Android Phone', ?, datetime('now'))");
        $insertAgent->execute([$agentId, $ip]);
    }
    
    // PENDING bo'lgan SMS-larni olish va ularni SENDING-ga o'tkazish
    $pdo->beginTransaction();
    try {
        $stmt = $pdo->prepare("SELECT id, external_id as id, phone_number, message FROM sms_queue WHERE status = 'PENDING' LIMIT 5");
        $stmt->execute();
        $smsList = $stmt->fetchAll();
        
        foreach ($smsList as $sms) {
            $updateStmt = $pdo->prepare("UPDATE sms_queue SET status = 'SENDING', agent_id = ?, updated_at = datetime('now') WHERE id = ?");
            $updateStmt->execute([$agentId, $sms['id']]);
        }
        $pdo->commit();
        
        sendJsonResponse($smsList);
    } catch (Exception $e) {
        $pdo->rollBack();
        sendJsonResponse(['status' => 'ERROR', 'message' => $e->getMessage()], 500);
    }
}

// 5. Mobile App: SMS statusini xabar qilish
// POST /api/v1/sms/{requestId}/status
if (preg_match('/^api\/v1\/sms\/([^\/]+)\/status$/', $route, $matches) && $method === 'POST') {
    checkAuthorization();
    $requestId = $matches[1];
    $input = json_decode(file_get_contents('php://input'), true);
    
    $status = isset($input['status']) ? $input['status'] : 'UNKNOWN';
    $errorCode = isset($input['error_code']) ? $input['error_code'] : 0;
    $errorMessage = isset($input['error_message']) ? $input['error_message'] : '';
    
    $stmt = $pdo->prepare("UPDATE sms_queue SET status = ?, error_code = ?, error_message = ?, updated_at = datetime('now') WHERE external_id = ?");
    $stmt->execute([$status, $errorCode, $errorMessage, $requestId]);
    
    sendJsonResponse(['status' => 'DONE', 'message' => 'Status yangilandi']);
}

// =========================================================================
// DASHBOARD WEB INTERFACE (HTML/CSS)
// =========================================================================
if ($route === '' || $route === 'dashboard') {
    // SMS-larni test yuborish formasini qayta ishlash
    $alert = '';
    if (isset($_POST['send_test'])) {
        $testPhone = $_POST['test_phone'] ?? '';
        $testMessage = $_POST['test_message'] ?? '';
        if (!empty($testPhone) && !empty($testMessage)) {
            $extId = 'test_' . uniqid();
            $stmt = $pdo->prepare("INSERT INTO sms_queue (external_id, phone_number, message) VALUES (?, ?, ?)");
            $stmt->execute([$extId, $testPhone, $testMessage]);
            $alert = '<div class="alert success">Test SMS muvaffaqiyatli navbatga qo\'shildi!</div>';
        } else {
            $alert = '<div class="alert error">Xatolik: Barcha maydonlarni to\'ldiring!</div>';
        }
    }
    
    // SMS loglarini olish
    $stmt = $pdo->query("SELECT * FROM sms_queue ORDER BY id DESC LIMIT 50");
    $smsLogs = $stmt->fetchAll();
    
    // Online Agentlarni olish
    $stmt = $pdo->query("SELECT * FROM agents ORDER BY last_heartbeat DESC");
    $agents = $stmt->fetchAll();
    
    // SMS statistikasini hisoblash
    $stats = [
        'pending' => $pdo->query("SELECT COUNT(*) FROM sms_queue WHERE status = 'PENDING'")->fetchColumn(),
        'sent' => $pdo->query("SELECT COUNT(*) FROM sms_queue WHERE status = 'SENT' OR status = 'DELIVERED'")->fetchColumn(),
        'failed' => $pdo->query("SELECT COUNT(*) FROM sms_queue WHERE status = 'FAILED'")->fetchColumn(),
    ];
    ?>
    <!DOCTYPE html>
    <html lang="uz">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Skat SMS Agent - Dashboard</title>
        <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap" rel="stylesheet">
        <style>
            :root {
                --bg-gradient: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%);
                --card-bg: rgba(30, 41, 59, 0.7);
                --accent-color: #fbbf24;
                --text-main: #f8fafc;
                --text-muted: #94a3b8;
                --success: #10b981;
                --warning: #f59e0b;
                --danger: #ef4444;
            }
            * {
                box-sizing: border-box;
                margin: 0;
                padding: 0;
            }
            body {
                font-family: 'Outfit', sans-serif;
                background: var(--bg-gradient);
                color: var(--text-main);
                min-height: 100vh;
                padding: 24px;
            }
            header {
                max-width: 1200px;
                margin: 0 auto 30px auto;
                display: flex;
                justify-content: space-between;
                align-items: center;
                border-bottom: 1px solid rgba(255,255,255,0.1);
                padding-bottom: 20px;
            }
            h1 {
                font-size: 28px;
                font-weight: 700;
                display: flex;
                align-items: center;
                gap: 10px;
            }
            h1 span {
                color: var(--accent-color);
            }
            .api-key-badge {
                background: rgba(251, 191, 36, 0.15);
                border: 1px solid var(--accent-color);
                color: var(--accent-color);
                padding: 6px 12px;
                border-radius: 20px;
                font-size: 13px;
                font-family: monospace;
            }
            .grid {
                max-width: 1200px;
                margin: 0 auto;
                display: grid;
                grid-template-columns: 1fr 1fr 1fr;
                gap: 20px;
                margin-bottom: 30px;
            }
            @media (max-width: 900px) {
                .grid { grid-template-columns: 1fr; }
            }
            .card {
                background: var(--card-bg);
                backdrop-filter: blur(10px);
                border: 1px solid rgba(255,255,255,0.05);
                border-radius: 16px;
                padding: 20px;
            }
            .stat-card {
                text-align: center;
                display: flex;
                flex-direction: column;
                justify-content: center;
            }
            .stat-value {
                font-size: 36px;
                font-weight: 700;
                margin-top: 10px;
            }
            .stat-label {
                font-size: 13px;
                color: var(--text-muted);
                text-transform: uppercase;
                letter-spacing: 1px;
            }
            .form-group {
                margin-bottom: 15px;
            }
            label {
                display: block;
                font-size: 13px;
                color: var(--text-muted);
                margin-bottom: 6px;
            }
            input, textarea {
                width: 100%;
                background: rgba(15, 23, 42, 0.6);
                border: 1px solid rgba(255,255,255,0.1);
                border-radius: 8px;
                padding: 10px 14px;
                color: var(--text-main);
                font-family: inherit;
                font-size: 14px;
            }
            input:focus, textarea:focus {
                border-color: var(--accent-color);
                outline: none;
            }
            button {
                width: 100%;
                background: var(--accent-color);
                color: #1e1b4b;
                border: none;
                border-radius: 8px;
                padding: 12px;
                font-size: 15px;
                font-weight: 600;
                cursor: pointer;
                transition: opacity 0.2s;
            }
            button:hover {
                opacity: 0.9;
            }
            .alert {
                padding: 12px;
                border-radius: 8px;
                margin-bottom: 15px;
                font-size: 14px;
            }
            .alert.success {
                background: rgba(16, 185, 129, 0.15);
                border: 1px solid var(--success);
                color: var(--success);
            }
            .alert.error {
                background: rgba(239, 68, 68, 0.15);
                border: 1px solid var(--danger);
                color: var(--danger);
            }
            .dashboard-layout {
                max-width: 1200px;
                margin: 0 auto;
                display: grid;
                grid-template-columns: 2fr 1fr;
                gap: 20px;
            }
            @media (max-width: 900px) {
                .dashboard-layout { grid-template-columns: 1fr; }
            }
            h2 {
                font-size: 18px;
                font-weight: 600;
                margin-bottom: 15px;
                border-left: 3px solid var(--accent-color);
                padding-left: 10px;
            }
            .table-container {
                overflow-x: auto;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                font-size: 14px;
            }
            th, td {
                padding: 12px;
                text-align: left;
                border-bottom: 1px solid rgba(255,255,255,0.05);
            }
            th {
                color: var(--text-muted);
                font-weight: 500;
            }
            .status-badge {
                display: inline-block;
                padding: 4px 8px;
                border-radius: 4px;
                font-size: 11px;
                font-weight: 700;
            }
            .status-PENDING { background: rgba(245, 158, 11, 0.15); color: var(--warning); }
            .status-SENDING { background: rgba(59, 130, 246, 0.15); color: #3b82f6; }
            .status-SENT { background: rgba(16, 185, 129, 0.15); color: var(--success); }
            .status-DELIVERED { background: rgba(16, 185, 129, 0.25); color: #059669; }
            .status-FAILED { background: rgba(239, 68, 68, 0.15); color: var(--danger); }
            
            .agent-list {
                list-style: none;
            }
            .agent-item {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 12px;
                border-bottom: 1px solid rgba(255,255,255,0.05);
            }
            .agent-name {
                font-weight: 600;
            }
            .agent-ip {
                font-size: 11px;
                color: var(--text-muted);
                font-family: monospace;
            }
            .agent-time {
                font-size: 12px;
                color: var(--text-muted);
            }
            .indicator {
                width: 8px;
                height: 8px;
                border-radius: 50%;
                display: inline-block;
                margin-right: 6px;
            }
            .indicator.online { background: var(--success); }
            .indicator.offline { background: var(--text-muted); }
        </style>
    </head>
    <body>
        <header>
            <h1>🚖 Skat SMS <span>Agent Bridge</span></h1>
            <div class="api-key-badge">API Key: <?php echo API_KEY; ?></div>
        </header>

        <div class="grid">
            <div class="card stat-card" style="border-top: 4px solid var(--warning);">
                <div class="stat-label">Kutilmoqda (Pending)</div>
                <div class="stat-value" style="color: var(--warning);"><?php echo $stats['pending']; ?></div>
            </div>
            <div class="card stat-card" style="border-top: 4px solid var(--success);">
                <div class="stat-label">Yuborildi (Sent)</div>
                <div class="stat-value" style="color: var(--success);"><?php echo $stats['sent']; ?></div>
            </div>
            <div class="card stat-card" style="border-top: 4px solid var(--danger);">
                <div class="stat-label">Xatoliklar (Failed)</div>
                <div class="stat-value" style="color: var(--danger);"><?php echo $stats['failed']; ?></div>
            </div>
        </div>

        <div class="dashboard-layout">
            <!-- Left Panel: SMS Logs -->
            <div class="card">
                <h2>SMS Jo'natish Jurnali</h2>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Raqam</th>
                                <th>Matn</th>
                                <th>Holat</th>
                                <th>Sana</th>
                                <th>Tafsilot</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php if (empty($smsLogs)): ?>
                                <tr>
                                    <td colspan="5" style="text-align: center; color: var(--text-muted);">Hozircha yozuvlar yo'q</td>
                                </tr>
                            <?php else: ?>
                                <?php foreach ($smsLogs as $log): ?>
                                    <tr>
                                        <td><strong><?php echo htmlspecialchars($log['phone_number']); ?></strong></td>
                                        <td><?php echo htmlspecialchars($log['message']); ?></td>
                                        <td><span class="status-badge status-<?php echo $log['status']; ?>"><?php echo $log['status']; ?></span></td>
                                        <td style="font-size: 12px; color: var(--text-muted);"><?php echo $log['created_at']; ?></td>
                                        <td style="font-size: 11px; color: var(--text-muted);">
                                            <?php 
                                            if ($log['status'] === 'FAILED') {
                                                echo htmlspecialchars($log['error_message']);
                                            } else {
                                                echo $log['agent_id'] ? 'Agent: ' . htmlspecialchars($log['agent_id']) : '-';
                                            }
                                            ?>
                                        </td>
                                    </tr>
                                <?php endforeach; ?>
                            <?php endif; ?>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Right Panel: Online Agents & Test Form -->
            <div style="display: flex; flex-direction: column; gap: 20px;">
                <!-- Test Send Card -->
                <div class="card">
                    <h2>Test SMS Navbatiga Qo'shish</h2>
                    <?php echo $alert; ?>
                    <form method="POST">
                        <div class="form-group">
                            <label>Telefon raqami</label>
                            <input type="text" name="test_phone" placeholder="+998901234567" required>
                        </div>
                        <div class="form-group">
                            <label>SMS matni</label>
                            <textarea name="test_message" rows="3" placeholder="Safar narxi: 12 000 so'm. Rahmat!" required></textarea>
                        </div>
                        <button type="submit" name="send_test">Navbatga Qo'shish</button>
                    </form>
                </div>

                <!-- Online Agents Card -->
                <div class="card">
                    <h2>Faol Agentlar (Drayverlar)</h2>
                    <ul class="agent-list">
                        <?php if (empty($agents)): ?>
                            <li style="padding: 12px; text-align: center; color: var(--text-muted);">Faol telefonlar mavjud emas</li>
                        <?php else: ?>
                            <?php foreach ($agents as $agent): ?>
                                <?php 
                                $isOnline = (time() - strtotime($agent['last_heartbeat'])) < 60; // 60 soniya faollik
                                ?>
                                <li class="agent-item">
                                    <div>
                                        <div class="agent-name">
                                            <span class="indicator <?php echo $isOnline ? 'online' : 'offline'; ?>"></span>
                                            <?php echo htmlspecialchars($agent['device_name']); ?>
                                        </div>
                                        <div class="agent-ip">ID: <?php echo htmlspecialchars($agent['agent_id']); ?></div>
                                    </div>
                                    <div class="agent-time" style="text-align: right;">
                                        <div style="font-size: 11px; color: var(--text-muted);">Oxirgi faollik:</div>
                                        <div><?php echo date('H:i:s', strtotime($agent['last_heartbeat'])); ?></div>
                                    </div>
                                </li>
                            <?php endforeach; ?>
                        <?php endif; ?>
                    </ul>
                </div>
            </div>
        </div>
    </body>
    </html>
    <?php
    exit;
}

// 404 Route Fallback
sendJsonResponse(['status' => 'ERROR', 'message' => 'API Endpoint topilmadi'], 404);
?>
