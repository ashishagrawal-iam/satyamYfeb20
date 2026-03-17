package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.entity.NotificationEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import com.wiom.csp.domain.model.AppNotification
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    private val prefs: UserPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get active (non-dismissed) notifications: API-first, Room cache fallback.
     */
    suspend fun getNotifications(): List<AppNotification> {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSeedNotifications()
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.getNotifications("Bearer $token")
            val notifArray = response["notifications"]?.jsonArray ?: JsonArray(emptyList())
            val notifications = notifArray.map { element ->
                val notif = json.decodeFromJsonElement<AppNotification>(element)
                db.notificationDao().insert(
                    NotificationEntity(
                        id = notif.id,
                        json = element.toString(),
                        dismissed = notif.dismissed,
                        timestamp = notif.timestamp
                    )
                )
                notif
            }
            notifications.filter { !it.dismissed }
        } catch (apiError: Exception) {
            try {
                db.notificationDao().getActive().map { entity ->
                    json.decodeFromString<AppNotification>(entity.json)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Dismiss a notification locally and via API.
     */
    suspend fun dismiss(id: String) {
        // Always dismiss locally first
        try {
            db.notificationDao().dismiss(id)
        } catch (_: Exception) { }

        if (BuildConfig.USE_MOCK) return

        try {
            val token = prefs.getToken() ?: return
            val body = buildJsonObject {
                put("id", JsonPrimitive(id))
            }
            api.dismissNotification("Bearer $token", body)
        } catch (_: Exception) {
            // Dismiss is best-effort — local state is already updated
        }
    }
}
