package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.AuthApiService
import kotlinx.serialization.json.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class AuthResult(
    val token: String,
    val userName: String,
    val isProfileComplete: Boolean
)

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApiService,
    private val prefs: UserPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Send OTP to given mobile number. Returns a temporary token (guid)
     * that must be passed to verifyOtp.
     * In mock mode, returns a fake guid immediately.
     */
    suspend fun sendOtp(mobile: String): Result<String> {
        if (BuildConfig.USE_MOCK) {
            return Result.success(UUID.randomUUID().toString())
        }

        return try {
            val body = buildJsonObject {
                put("appName", JsonPrimitive("WIOM_SALES"))
                put("mobile", JsonPrimitive(mobile))
            }
            val response = authApi.sendOtp(body)
            val guid = response["guid"]?.jsonPrimitive?.content
                ?: response["data"]?.jsonObject?.get("guid")?.jsonPrimitive?.content
                ?: throw IllegalStateException("No guid in OTP response")
            Result.success(guid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify OTP and obtain auth token + user info.
     * In mock mode, accepts any 4-digit OTP and returns fake credentials.
     */
    suspend fun verifyOtp(
        mobile: String,
        otp: String,
        guid: String
    ): Result<AuthResult> {
        if (BuildConfig.USE_MOCK) {
            if (otp.length != 4 || !otp.all { it.isDigit() }) {
                return Result.failure(IllegalArgumentException("OTP must be 4 digits"))
            }
            val mockToken = "mock_token_${UUID.randomUUID()}"
            prefs.setToken(mockToken)
            prefs.setUserName("Mock CSP Partner")
            prefs.setProfileComplete(true)
            return Result.success(
                AuthResult(
                    token = mockToken,
                    userName = "Mock CSP Partner",
                    isProfileComplete = true
                )
            )
        }

        return try {
            val response = authApi.verifyOtp(
                appName = "WIOM_SALES",
                otp = otp,
                username = mobile,
                guid = guid,
                fcmToken = "" // FCM token populated later if available
            )

            if (!response.isSuccessful) {
                return Result.failure(
                    IllegalStateException("OTP verification failed: ${response.code()}")
                )
            }

            // Extract token from response header or body
            val responseBody = response.body()
                ?: return Result.failure(IllegalStateException("Empty response body"))

            val token = response.headers()["Authorization"]
                ?: response.headers()["authorization"]
                ?: responseBody["token"]?.jsonPrimitive?.content
                ?: responseBody["data"]?.jsonObject?.get("token")?.jsonPrimitive?.content
                ?: return Result.failure(IllegalStateException("No token in response"))

            val userName = responseBody["userName"]?.jsonPrimitive?.content
                ?: responseBody["data"]?.jsonObject?.get("userName")?.jsonPrimitive?.content
                ?: mobile

            val isProfileComplete = responseBody["isProfileComplete"]?.jsonPrimitive?.boolean
                ?: responseBody["data"]?.jsonObject?.get("isProfileComplete")?.jsonPrimitive?.boolean
                ?: false

            // Persist credentials
            prefs.setToken(token)
            prefs.setUserName(userName)
            prefs.setProfileComplete(isProfileComplete)

            Result.success(
                AuthResult(
                    token = token,
                    userName = userName,
                    isProfileComplete = isProfileComplete
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
