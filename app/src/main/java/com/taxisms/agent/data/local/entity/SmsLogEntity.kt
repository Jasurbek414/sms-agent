package com.taxisms.agent.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SMS Log Entity - Yuborilgan SMS xabarlar tarixi
 *
 * Har bir SMS xabar haqida to'liq ma'lumot saqlanadi:
 * telefon raqam, xabar matni, holat, vaqt, xatolik kodi va h.k.
 */
@Entity(
    tableName = "sms_logs",
    indices = [
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["request_id"], unique = true)
    ]
)
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Server tomondan kelgan so'rov ID */
    @ColumnInfo(name = "request_id")
    val requestId: String,

    /** Qabul qiluvchi telefon raqam */
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    /** SMS matn */
    @ColumnInfo(name = "message_text")
    val messageText: String,

    /** SMS holati (Constants.SmsStatus) */
    @ColumnInfo(name = "status")
    val status: Int,

    /** Xatolik kodi (agar bo'lsa) */
    @ColumnInfo(name = "error_code")
    val errorCode: Int? = null,

    /** Xatolik xabari (agar bo'lsa) */
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    /** Qayta urinishlar soni */
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    /** Yaratilgan vaqt */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** Yuborilgan vaqt */
    @ColumnInfo(name = "sent_at")
    val sentAt: Long? = null,

    /** Yetkazilgan vaqt */
    @ColumnInfo(name = "delivered_at")
    val deliveredAt: Long? = null
)
