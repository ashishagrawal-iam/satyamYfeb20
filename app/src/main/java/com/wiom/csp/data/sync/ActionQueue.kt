package com.wiom.csp.data.sync

import com.wiom.csp.data.db.ActionQueueDao
import com.wiom.csp.data.db.entity.ActionQueueEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper that wraps ActionQueueDao for queueing and replaying
 * offline actions with JSON payloads.
 */
@Singleton
class ActionQueue @Inject constructor(
    private val actionQueueDao: ActionQueueDao,
    private val api: ApiService,
    private val prefs: UserPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        const val MAX_RETRIES = 5
    }

    /**
     * Enqueue an action for later retry when the device comes back online.
     */
    suspend fun enqueue(endpoint: String, payload: String) {
        actionQueueDao.insert(
            ActionQueueEntity(
                id = UUID.randomUUID().toString(),
                endpoint = endpoint,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Get all pending actions in queue order (oldest first).
     */
    suspend fun getPending(): List<ActionQueueEntity> {
        return actionQueueDao.getPending()
    }

    /**
     * Process all queued actions. Each action is attempted once:
     * - On success, removed from queue.
     * - On failure, retry count incremented. Dropped after MAX_RETRIES.
     */
    suspend fun processQueue() {
        val token = prefs.getToken() ?: return
        val bearerToken = "Bearer $token"
        val pending = actionQueueDao.getPending()

        for (action in pending) {
            if (action.retryCount >= MAX_RETRIES) {
                // Exceeded max retries — drop the action
                actionQueueDao.remove(action.id)
                continue
            }

            try {
                val payload = json.decodeFromString<JsonObject>(action.payload)
                dispatchAction(bearerToken, action.endpoint, payload)
                // Success — remove from queue
                actionQueueDao.remove(action.id)
            } catch (_: Exception) {
                // Failed — increment retry count for next attempt
                actionQueueDao.incrementRetry(action.id)
            }
        }
    }

    /**
     * Dispatch a queued action to its original API endpoint.
     */
    private suspend fun dispatchAction(
        token: String,
        endpoint: String,
        payload: JsonObject
    ) {
        when (endpoint) {
            "/api/tasks/action" -> api.performTaskAction(token, payload)
            "/api/wallet/action" -> api.performWalletAction(token, payload)
            "/api/support" -> api.createSupportCase(token, payload)
            "/api/technicians" -> api.addTechnician(token, payload)
            "/api/notifications/dismiss" -> api.dismissNotification(token, payload)
            "/api/theme" -> api.setTheme(token, payload)
            else -> throw IllegalArgumentException("Unknown queued endpoint: $endpoint")
        }
    }
}
