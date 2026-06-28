package com.taxisms.agent.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Agent API Service - Taksi park serveri bilan REST API aloqasi
 *
 * Bu interfeys Retrofit tomonidan implementatsiya qilinadi.
 * Barcha API so'rovlari shu yerda e'lon qilinadi.
 */
interface AgentApiService {

    /**
     * Agentni serverga ro'yxatdan o'tkazish
     */
    @POST("api/v1/agent/register")
    suspend fun registerAgent(
        @Header("Authorization") apiKey: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    /**
     * Agent holatini serverga yuborish (heartbeat)
     */
    @PUT("api/v1/agent/{agentId}/heartbeat")
    suspend fun sendHeartbeat(
        @Header("Authorization") apiKey: String,
        @Path("agentId") agentId: String
    ): Response<Map<String, Any>>

    /**
     * SMS yuborish natijasini serverga xabar berish
     */
    @POST("api/v1/sms/{requestId}/status")
    suspend fun reportSmsStatus(
        @Header("Authorization") apiKey: String,
        @Path("requestId") requestId: String,
        @Body statusReport: Map<String, Any>
    ): Response<Map<String, Any>>

    /**
     * Kutilayotgan SMS so'rovlarini olish (polling uchun)
     */
    @GET("api/v1/agent/{agentId}/pending-sms")
    suspend fun getPendingSmsRequests(
        @Header("Authorization") apiKey: String,
        @Path("agentId") agentId: String
    ): Response<List<Map<String, Any>>>
}
