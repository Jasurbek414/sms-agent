package com.taxisms.agent.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taxisms.agent.data.local.entity.SmsLogEntity
import com.taxisms.agent.ui.main.MainViewModel
import com.taxisms.agent.ui.theme.DarkSurface
import com.taxisms.agent.ui.theme.ErrorColor
import com.taxisms.agent.ui.theme.InfoColor
import com.taxisms.agent.ui.theme.SuccessColor
import com.taxisms.agent.ui.theme.WarningColor
import com.taxisms.agent.util.Constants
import com.taxisms.agent.util.toRelativeTime

enum class DashboardTab {
    DASHBOARD,
    HISTORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel,
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isRunning by mainViewModel.isServiceRunning.collectAsState()
    val isNotificationListenerGranted by mainViewModel.isNotificationListenerGranted.collectAsState()

    val smsLogs by dashboardViewModel.smsLogs.collectAsState()
    val simList by dashboardViewModel.simInfoList.collectAsState()
    val activeConnection by dashboardViewModel.activeConnection.collectAsState()
    val preferredSim by dashboardViewModel.preferredSim.collectAsState()
    val smsTemplate by dashboardViewModel.smsTemplate.collectAsState()

    val notificationReaderEnabled by dashboardViewModel.notificationReaderEnabled.collectAsState()
    val notificationPackageFilter by dashboardViewModel.notificationPackageFilter.collectAsState()
    val notificationKeywordFilter by dashboardViewModel.notificationKeywordFilter.collectAsState()

    val apiPhonePath by dashboardViewModel.apiPhonePath.collectAsState()
    val apiPricePath by dashboardViewModel.apiPricePath.collectAsState()

    var currentTab by remember { mutableStateOf(DashboardTab.DASHBOARD) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showTestSmsDialog by remember { mutableStateOf(false) }

    // Statistikalarni hisoblash
    val totalPending = smsLogs.count { it.status == Constants.SmsStatus.PENDING || it.status == Constants.SmsStatus.SENDING }
    val totalSent = smsLogs.count { it.status == Constants.SmsStatus.SENT }
    val totalDelivered = smsLogs.count { it.status == Constants.SmsStatus.DELIVERED }
    val totalFailed = smsLogs.count { it.status == Constants.SmsStatus.FAILED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (currentTab == DashboardTab.DASHBOARD) "Universal SMS Agent" else "Xabarlar Tarixi", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    if (currentTab == DashboardTab.DASHBOARD) {
                        IconButton(onClick = { showTestSmsDialog = true }) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Test SMS", tint = InfoColor)
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Sozlamalar")
                        }
                    } else {
                        // Tarixni tozalash tugmasi
                        if (smsLogs.isNotEmpty()) {
                            IconButton(onClick = { dashboardViewModel.clearAllLogs() }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Tarixni tozalash", tint = ErrorColor)
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = currentTab == DashboardTab.DASHBOARD,
                    onClick = { currentTab = DashboardTab.DASHBOARD },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Boshqaruv") },
                    label = { Text("Boshqaruv") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SuccessColor,
                        selectedTextColor = SuccessColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = DarkSurface
                    )
                )
                NavigationBarItem(
                    selected = currentTab == DashboardTab.HISTORY,
                    onClick = { currentTab = DashboardTab.HISTORY },
                    icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Tarix") },
                    label = { Text("Xabarlar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SuccessColor,
                        selectedTextColor = SuccessColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = DarkSurface
                    )
                )
            }
        }
    ) { innerPadding ->
        if (currentTab == DashboardTab.DASHBOARD) {
            // =========================================================================
            // TAB 1: DASHBOARD / BOSHQARUV PANELI
            // =========================================================================
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Info Header (if connected)
                if (activeConnection != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Ulangan Server", fontSize = 11.sp, color = Color.Gray)
                                    Text(activeConnection?.parkName ?: "", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(activeConnection?.serverUrl ?: "", fontSize = 11.sp, color = Color.LightGray)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SuccessColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("ULANGAN", color = SuccessColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Status Card (Foreground Service Control)
                item {
                    StatusCard(isRunning = isRunning, onToggle = { mainViewModel.toggleService() })
                }

                // Notification Reader Permission & Switch Card (ZERO-INTEGRATION FALLBACK)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tepadagi Bildirishnomalarni O'qish", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Text(
                                        text = "Taksi dasturi bildirishnomalaridan narx va raqamni tutib yuborish (Ratsiya usuli).",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        lineHeight = 15.sp
                                    )
                                }
                                Switch(
                                    checked = notificationReaderEnabled,
                                    onCheckedChange = { dashboardViewModel.setNotificationReaderEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SuccessColor)
                                )
                            }

                            if (notificationReaderEnabled) {
                                if (!isNotificationListenerGranted) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(WarningColor.copy(alpha = 0.15f))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "DIQQAT: Tizim sozlamalaridan bildirishnomalarni o'qish ruxsati berilmagan!",
                                                color = WarningColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Button(
                                                onClick = {
                                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = WarningColor),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Ruxsat berish", fontSize = 11.sp, color = Color.Black)
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SuccessColor.copy(alpha = 0.1f))
                                            .padding(10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text("✓ Bildirishnoma ruxsati faol", color = SuccessColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // SIM Info Card (SIM Selector built-in)
                item {
                    SimCard(
                        simList = simList,
                        selectedSim = preferredSim,
                        onSimSelected = { dashboardViewModel.setPreferredSim(it) },
                        onRefresh = { dashboardViewModel.loadSimInfo() }
                    )
                }

                // Grid counters (Pending, Sent, Delivered, Failed)
                item {
                    StatsGrid(pending = totalPending, sent = totalSent, delivered = totalDelivered, failed = totalFailed)
                }
            }
        } else {
            // =========================================================================
            // TAB 2: XABARLAR TARIXI SCREEN (SEPARATE MENU)
            // =========================================================================
            var searchQuery by remember { mutableStateOf("") }
            var selectedFilter by remember { mutableStateOf<Int?>(null) } // null = All, 0 = Pending, 2 = Sent, 3 = Delivered, 4 = Failed

            val filteredLogs = smsLogs.filter { log ->
                val matchesSearch = log.phoneNumber.contains(searchQuery, ignoreCase = true) || 
                                    log.messageText.contains(searchQuery, ignoreCase = true)
                val matchesFilter = if (selectedFilter == null) {
                    true
                } else if (selectedFilter == 0) {
                    log.status == Constants.SmsStatus.PENDING || log.status == Constants.SmsStatus.SENDING
                } else {
                    log.status == selectedFilter
                }
                matchesSearch && matchesFilter
            }

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Qidirish (telefon, matn)...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SuccessColor,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }

                // Filter Chips Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("Barchasi") }
                        )
                        FilterChip(
                            selected = selectedFilter == 0,
                            onClick = { selectedFilter = 0 },
                            label = { Text("Kutilmoqda") }
                        )
                        FilterChip(
                            selected = selectedFilter == Constants.SmsStatus.DELIVERED,
                            onClick = { selectedFilter = Constants.SmsStatus.DELIVERED },
                            label = { Text("Etkazildi") }
                        )
                        FilterChip(
                            selected = selectedFilter == Constants.SmsStatus.FAILED,
                            onClick = { selectedFilter = Constants.SmsStatus.FAILED },
                            label = { Text("Xatolik") }
                        )
                    }
                }

                if (filteredLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Mos keluvchi SMS loglari topilmadi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(filteredLogs) { log ->
                        SmsLogRow(log = log, onRetry = { dashboardViewModel.retrySms(log.id) })
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // =========================================================================
    // SOZLAMALAR DIALOGI (BACKEND, TEMPLATES & DYNAMIC JSON PATHS)
    // =========================================================================
    if (showSettingsDialog) {
        var serverUrl by remember { mutableStateOf(activeConnection?.serverUrl ?: "") }
        var apiKey by remember { mutableStateOf(activeConnection?.apiKey ?: "") }
        var parkName by remember { mutableStateOf(activeConnection?.parkName ?: "") }
        var templateText by remember { mutableStateOf(smsTemplate) }

        var pkgFilter by remember { mutableStateOf(notificationPackageFilter) }
        var keyFilter by remember { mutableStateOf(notificationKeywordFilter) }

        var phonePath by remember { mutableStateOf(apiPhonePath) }
        var pricePath by remember { mutableStateOf(apiPricePath) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Agent Sozlamalari") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    item {
                        Text("Backend ulanish ma'lumotlari:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    item {
                        OutlinedTextField(
                            value = parkName,
                            onValueChange = { parkName = it },
                            label = { Text("Taksi Park Nomi") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Backend Server URL") },
                            placeholder = { Text("https://api.taxiservice.uz/sms") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Kalit (Key) yoki JWT Token") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.DarkGray)
                        Text("Dinamik Server JSON Kalitlari (API Parser):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    item {
                        OutlinedTextField(
                            value = phonePath,
                            onValueChange = { phonePath = it },
                            label = { Text("Telefon raqami kaliti (JSON key)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = pricePath,
                            onValueChange = { pricePath = it },
                            label = { Text("Yo'l haqi summasi kaliti (JSON key)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.DarkGray)
                        Text("SMS Shablon Sozlamalari:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    item {
                        OutlinedTextField(
                            value = templateText,
                            onValueChange = { templateText = it },
                            label = { Text("SMS Shablon matni") },
                            supportingText = { Text("{price} kalit so'zi narx bilan almashadi.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.DarkGray)
                        Text("Bildirishnomalar Filtri (Notification Reader):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    item {
                        OutlinedTextField(
                            value = pkgFilter,
                            onValueChange = { pkgFilter = it },
                            label = { Text("Taksi dasturi paket nomi (com.taxi.driver)") },
                            placeholder = { Text("com.yandex.taximeter") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = keyFilter,
                            onValueChange = { keyFilter = it },
                            label = { Text("Kalit so'z filtri (safar, tugadi)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverUrl.isNotBlank() && parkName.isNotBlank()) {
                            dashboardViewModel.connectToServer(serverUrl, apiKey, parkName)
                        }
                        dashboardViewModel.setSmsTemplate(templateText)
                        dashboardViewModel.setNotificationPackageFilter(pkgFilter)
                        dashboardViewModel.setNotificationKeywordFilter(keyFilter)
                        dashboardViewModel.setApiPaths(phonePath, pricePath)
                        showSettingsDialog = false
                    }
                ) {
                    Text("Saqlash")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Bekor qilish")
                    }
                    if (activeConnection != null) {
                        TextButton(
                            onClick = {
                                dashboardViewModel.disconnectFromServer()
                                showSettingsDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorColor)
                        ) {
                            Text("Uzilish")
                        }
                    }
                }
            }
        )
    }

    // =========================================================================
    // SINOV SMS YUBORISH DIALOGI (TEST & MANUAL TEMPLATE OPTIONS)
    // =========================================================================
    if (showTestSmsDialog) {
        var testPhone by remember { mutableStateOf("") }
        var testMessage by remember { mutableStateOf("") }
        var isTemplateMode by remember { mutableStateOf(false) }
        var testPrice by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTestSmsDialog = false },
            title = { Text("Sinov SMS Yuborish") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = testPhone,
                        onValueChange = { testPhone = it },
                        label = { Text("Mijoz Telefon Raqami") },
                        placeholder = { Text("+998901234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTemplateMode,
                            onCheckedChange = { isTemplateMode = it }
                        )
                        Text("Yo'l haqi shablonidan foydalanish", fontSize = 14.sp)
                    }

                    if (isTemplateMode) {
                        OutlinedTextField(
                            value = testPrice,
                            onValueChange = { testPrice = it },
                            label = { Text("Yo'l haqi summasi (so'mda)") },
                            placeholder = { Text("15000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Matn shunday bo'ladi:\n" + smsTemplate.replace("{price}", testPrice.ifEmpty { "..." }),
                            fontSize = 11.sp,
                            color = InfoColor,
                            lineHeight = 16.sp
                        )
                    } else {
                        OutlinedTextField(
                            value = testMessage,
                            onValueChange = { testMessage = it },
                            label = { Text("SMS Xabar Matni") },
                            placeholder = { Text("Assalomu alaykum! Siz kutayotgan taksi yetib keldi.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (testPhone.isNotBlank()) {
                            if (isTemplateMode && testPrice.isNotBlank()) {
                                val formattedMessage = smsTemplate.replace("{price}", testPrice)
                                dashboardViewModel.sendTestSms(testPhone, formattedMessage)
                            } else if (!isTemplateMode && testMessage.isNotBlank()) {
                                dashboardViewModel.sendTestSms(testPhone, testMessage)
                            }
                            showTestSmsDialog = false
                        }
                    }
                ) {
                    Text("SMS Yuborish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestSmsDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }
}

@Composable
fun StatusCard(isRunning: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Xizmat Holati", fontWeight = FontWeight.Medium, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) SuccessColor else ErrorColor)
                    )
                    Text(
                        text = if (isRunning) "AKTIV (FONDA)" else "FAOL EMAS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) ErrorColor else SuccessColor
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Warning else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isRunning) "To'xtatish" else "Boshlash")
            }
        }
    }
}

@Composable
fun SimCard(
    simList: List<String>,
    selectedSim: String,
    onSimSelected: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SMS Jo'natish SIM-Kartasi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                IconButton(onClick = onRefresh) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Yangilash", tint = Color.Gray)
                }
            }
            
            simList.forEachIndexed { index, sim ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSimSelected(index.toString()) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedSim == index.toString(),
                        onClick = { onSimSelected(index.toString()) },
                        colors = RadioButtonDefaults.colors(selectedColor = SuccessColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(sim, color = Color.LightGray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun StatsGrid(pending: Int, sent: Int, delivered: Int, failed: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBox(title = "Kutilmoqda", value = pending.toString(), color = WarningColor, modifier = Modifier.weight(1f))
        StatBox(title = "Yuborildi", value = sent.toString(), color = InfoColor, modifier = Modifier.weight(1f))
        StatBox(title = "Etkazildi", value = delivered.toString(), color = SuccessColor, modifier = Modifier.weight(1f))
        StatBox(title = "Xatolik", value = failed.toString(), color = ErrorColor, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun SmsLogRow(log: SmsLogEntity, onRetry: () -> Unit) {
    val statusText = when (log.status) {
        Constants.SmsStatus.PENDING -> "Kutilmoqda"
        Constants.SmsStatus.SENDING -> "Yuborilmoqda"
        Constants.SmsStatus.SENT -> "Yuborildi"
        Constants.SmsStatus.DELIVERED -> "Yetkazildi"
        Constants.SmsStatus.FAILED -> "Xato"
        else -> "Bekor qilindi"
    }

    val statusColor = when (log.status) {
        Constants.SmsStatus.PENDING, Constants.SmsStatus.SENDING -> WarningColor
        Constants.SmsStatus.SENT -> InfoColor
        Constants.SmsStatus.DELIVERED -> SuccessColor
        else -> ErrorColor
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(log.phoneNumber, fontWeight = FontWeight.Bold, color = Color.White)
                Text(log.messageText, fontSize = 13.sp, color = Color.LightGray, maxLines = 2)
                Text(
                    text = log.createdAt.toRelativeTime(),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (log.status == Constants.SmsStatus.FAILED) {
                    TextButton(onClick = onRetry) {
                        Text("Qayta", color = SuccessColor, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
