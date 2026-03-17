package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    private val api: ApiService,
    private val prefs: UserPreferences
) {

    /**
     * Get the current theme. Local preference is the source of truth;
     * API sync is best-effort on read.
     */
    suspend fun getTheme(): String {
        val local = prefs.getTheme()

        if (BuildConfig.USE_MOCK) return local

        // Attempt to pull server theme — non-blocking, local wins on failure
        try {
            val token = prefs.getToken() ?: return local
            val response = api.getTheme("Bearer $token")
            val serverTheme = response["theme"]?.jsonPrimitive?.content
            if (serverTheme != null && serverTheme != local) {
                prefs.setTheme(serverTheme)
                return serverTheme
            }
        } catch (_: Exception) {
            // API unavailable — use local preference
        }

        return local
    }

    /**
     * Set the theme. Saves locally immediately, syncs to API in background.
     */
    suspend fun setTheme(theme: String) {
        prefs.setTheme(theme)

        if (BuildConfig.USE_MOCK) return

        try {
            val token = prefs.getToken() ?: return
            val body = buildJsonObject {
                put("theme", JsonPrimitive(theme))
            }
            api.setTheme("Bearer $token", body)
        } catch (_: Exception) {
            // Theme will sync on next successful API call
        }
    }
}
