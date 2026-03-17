package com.wiom.csp.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("v1/Authentication/SendOTP")
    suspend fun sendOtp(
        @Body body: JsonObject
    ): JsonObject

    @GET("v1/Authentication/VerifyOTP")
    suspend fun verifyOtp(
        @Query("appName") appName: String,
        @Query("otp") otp: String,
        @Query("username") username: String,
        @Query("guid") guid: String,
        @Query("fcmToken") fcmToken: String
    ): Response<JsonObject>
}
