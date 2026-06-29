# 🚖 Skat Taxi - Android SMS Agent Integration

Ushbu loyiha **Skat Taxi** dasturiy majmuasidan chiqadigan SMS-larni haydovchilarning shaxsiy telefonlari (SIM-kartalari) orqali mijozlarga avtomatik ravishda yuborish uchun mo'ljallangan.

Tizim zanjiri quyidagi ko'rinishda ishlaydi:
`Skat Server` ➔ `SMPP Server (Docker)` ➔ `SQLite DB (Docker)` ➔ `PHP API & Dashboard` ➔ `Android SMS Agent (Ilova)` ➔ `Mijoz (SIM orqali)`

---

## 📋 1. Skat Taxi panelida sozlash

Skat admin panelidagi SMS provayder qo'shish bo'limiga kiring va quyidagi sozlamalarni kiriting:

*   **Название (Nomi):** `Ecos SMS`
*   **Протокол (Protokol):** `SMPP`
*   **Использовать международный формат номера:** `Да`
*   **Использовать ASCII кодировку:** `Нет`
*   **Использовать транслит:** `Нет`
*   **Хост (Host):** `93.188.80.101` *(Serveringizning tashqi IP manzili)*
*   **Порт (Port):** `2775` *(Docker orqali ochilgan SMPP porti)*
*   **System ID (Логин):** `ecos`
*   **Пароль (Parol):** `ecos123`
*   **Тип соединения (Bind Type):** `Transceiver` (yoki `Transmitter`)

---

## 🐳 2. Serverda Docker-ni ishga tushirish

Barcha server xizmatlari (PHP Dashboard, SQLite, SMPP va Cloudflare Tunnel) bitta docker-compose konfiguratsiyasiga birlashtirilgan.

### Ishga tushirish buyrug'i:
```bash
docker compose up -d --build
```
Ushbu buyruq barcha konteynerlarni orqa fonda (background) qayta yig'ib ishga tushiradi.

### Ishlayotgan xizmatlar portlari:
*   **Dashboard & API:** `http://localhost:8082` (Cloudflare tunnel orqali `https://taxsi.ecos.uz` domeniga bog'langan)
*   **SMPP Server:** `0.0.0.0:2775` (Skat ulanishi uchun ochiq)

---

## 📱 3. Android Ilovani (SMS Agent) sozlash

1.  Telefoningizga yangi yig'ilgan APK-ni yuklab oling va o'rnating:
    *   Yuklab olish havolasi: **[https://taxsi.ecos.uz/app-universal-debug.apk](https://taxsi.ecos.uz/app-universal-debug.apk)**
2.  Ilovani oching va undagi sozlamalar tugmasini bosib, quyidagi ruxsatlarni yoqing:
    *   **SMS yuborish ruxsati** (Sms Permission).
    *   **Bildirishnomalarni o'qish ruxsati** (Notification Listener Permission) — *bu rejim hozir ishlatilmasa ham, fon rejimida ishlashni ta'minlash uchun yoqilgani ma'qul*.
3.  Ilova birinchi marta ulanganda **avtomatik ravishda** `https://taxsi.ecos.uz/backend/` manziliga va xavfsiz API kalitiga (`7e4cd8f3-72e2-4ed3-afcf-64274679cd86`) ulanadi.
4.  Ilova ishga tushganda telefonga mos keladigan unikal ID generatsiya qiladi (Masalan: `Samsung_A52_1234`) va serverda online drayverlar ro'yxatida yashil chiroq bilan ko'rinadi.

---

## 💻 4. Nazorat va Statistika (Dashboard)

Brauzeringiz orqali **`https://taxsi.ecos.uz`** domeniga kiring:
*   Bu yerda tizimga ulangan barcha haydovchilar (online/offline holatda va aniq vaqt zonalari bilan) ko'rinadi.
*   Yuborilgan, kutilayotgan va xatolik bergan SMS-lar jurnalini jonli (real-time) kuzatishingiz mumkin.
*   **Test rejimida** SMS yozib, tizimni tekshirib ko'rishingiz mumkin.

---

## 🔒 5. Muhim xavfsizlik va vaqt zonalari
*   Barcha so'rovlar xavfsiz JWT API kaliti bilan shifrlangan.
*   Vaqt zonalari muammosi hal qilingan: bazada Grinvich (UTC) vaqti saqlansa-da, dashboardda barchasi avtomatik ravishda **O'zbekiston (`Asia/Tashkent`)** vaqtiga o'girilib to'g'ri ko'rsatiladi.
