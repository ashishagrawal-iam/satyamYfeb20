package com.wiom.csp.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

interface ApiService {

    // ── Schema ──────────────────────────────────────────────────────
    @GET("/api/schema")
    suspend fun getSchema(
        @Header("Authorization") token: String
    ): JsonObject

    // ── Tasks ───────────────────────────────────────────────────────
    @GET("/api/tasks")
    suspend fun getTasks(
        @Header("Authorization") token: String
    ): JsonObject

    @POST("/api/tasks/action")
    suspend fun performTaskAction(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): JsonObject

    // ── Assurance ───────────────────────────────────────────────────
    @GET("/api/assurance")
    suspend fun getAssurance(
        @Header("Authorization") token: String
    ): JsonObject

    // ── Wallet ──────────────────────────────────────────────────────
    @GET("/api/wallet")
    suspend fun getWallet(
        @Header("Authorization") token: String
    ): JsonObject

    @POST("/api/wallet/action")
    suspend fun performWalletAction(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): JsonObject

    // ── SLA ─────────────────────────────────────────────────────────
    @GET("/api/sla")
    suspend fun getSla(
        @Header("Authorization") token: String
    ): JsonObject

    // ── Technicians ─────────────────────────────────────────────────
    @GET("/api/technicians")
    suspend fun getTechnicians(
        @Header("Authorization") token: String
    ): JsonObject

    @POST("/api/technicians")
    suspend fun addTechnician(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): JsonObject

    // ── Support ─────────────────────────────────────────────────────
    @GET("/api/support")
    suspend fun getSupportCases(
        @Header("Authorization") token: String
    ): JsonObject

    @POST("/api/support")
    suspend fun createSupportCase(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): JsonObject

    @POST("/api/support/{id}/message")
    suspend fun replySupportCase(
        @Header("Authorization") token: String,
        @Path("id") caseId: String,
        @Body body: JsonObject
    ): JsonObject

    // ── Deposit ─────────────────────────────────────────────────────
    @GET("/api/deposit")
    suspend fun getDeposit(
        @Header("Authorization") token: String
    ): JsonObject

    // ── Notifications ───────────────────────────────────────────────
    @GET("/api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): JsonObject

    @POST("/api/notifications/dismiss")
    suspend fun dismissNotification(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): JsonObject

    // ── Theme ───────────────────────────────────────────────────────
    @GET("/api/theme")
    suspend fun getTheme(
        @Header("Authorization") token: String
    ): JsonObject

    @POST("/api/theme")
    suspend fun setTheme(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): JsonObject
}
