const smpp = require('smpp');
const http = require('http');

const PORT = process.env.PORT || 2775;
const PHP_BACKEND_URL = process.env.PHP_BACKEND_URL || 'http://sms-agent-bridge-container/send';
const API_KEY = process.env.API_KEY || '7e4cd8f3-72e2-4ed3-afcf-64274679cd86';

console.log('SMPP Serveri ishga tushmoqda...');

const server = smpp.createServer((session) => {
    session.on('bind_transmitter', (pdu) => {
        console.log(`[SMPP] Transmitter bog'landi: SystemID=${pdu.system_id}`);
        session.send(pdu.getResponse());
    });

    session.on('bind_transceiver', (pdu) => {
        console.log(`[SMPP] Transceiver bog'landi: SystemID=${pdu.system_id}`);
        session.send(pdu.getResponse());
    });

    session.on('unbind', (pdu) => {
        console.log('[SMPP] Aloqa uzildi');
        session.send(pdu.getResponse());
        session.close();
    });

    session.on('enquire_link', (pdu) => {
        session.send(pdu.getResponse());
    });

    session.on('submit_sm', (pdu) => {
        const dest = pdu.destination_addr;
        let message = '';

        // SMS Kodirovkasini aniqlash va formatlash (Cyrillic uchun UCS2/UTF16 ko'p ishlatiladi)
        if (pdu.short_message && pdu.short_message.buffer) {
            const buffer = pdu.short_message.buffer;
            if (pdu.data_coding === 8) {
                // UCS2 (UTF-16BE) encoding
                message = buffer.toString('utf16be');
            } else {
                // ASCII / GSM 7-bit or UTF-8
                message = buffer.toString('utf8');
            }
        }

        console.log(`[SMPP] Yangi SMS so'rovi: Raqam=${dest}, Matn="${message}"`);

        // HTTP Proxy orqali PHP backendimizga navbatga qo'shish so'rovini jo'natamiz
        const postData = new URLSearchParams({
            phone: dest,
            text: message,
            key: API_KEY
        }).toString();

        const reqUrl = `${PHP_BACKEND_URL}?${postData}`;
        
        console.log(`[SMPP Proxy] Veb-ko'prikka yuborilmoqda: ${PHP_BACKEND_URL}`);

        http.get(reqUrl, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
                console.log(`[SMPP Proxy] Server javobi: ${data.trim()}`);
                
                // Skat-ga muvaffaqiyatli qabul qilinganlik javobini yuboramiz
                const response = pdu.getResponse();
                response.message_id = 'smpp_' + Math.floor(Math.random() * 1000000);
                session.send(response);
            });
        }).on('error', (err) => {
            console.error('[SMPP Proxy] Xatolik:', err.message);
            // Xatolik yuz bersa ham Skat ulanishini buzmaslik uchun javob qaytaramiz
            session.send(pdu.getResponse());
        });
    });

    session.on('error', (err) => {
        console.error('[SMPP Session] Xato:', err.message);
    });
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`[SMPP] Server port ${PORT} da muvaffaqiyatli ishlamoqda.`);
});
