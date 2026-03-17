package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.ActionQueueDao
import com.wiom.csp.data.db.entity.ActionQueueEntity
import com.wiom.csp.data.db.entity.TaskEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import com.wiom.csp.domain.model.QueuedAction
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    private val prefs: UserPreferences,
    private val actionQueueDao: ActionQueueDao
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get all tasks: API-first, cache on success, Room fallback.
     * Returns tasks IN RECEIVED ORDER — build rule #5: never sort locally.
     */
    suspend fun getTasks(): List<TaskData> {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSeedTasks()
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.getTasks("Bearer $token")
            val tasksArray = response["tasks"]?.jsonArray ?: JsonArray(emptyList())
            val tasks = tasksArray.mapIndexed { index, element ->
                val taskData = json.decodeFromJsonElement<TaskData>(element)
                // Cache each task to Room, preserving server order via bucketIndex
                db.taskDao().insertTask(
                    TaskEntity(
                        taskId = taskData.taskId,
                        json = element.toString(),
                        taskType = taskData.taskType,
                        currentState = taskData.currentState,
                        priority = taskData.priority,
                        bucketIndex = index
                    )
                )
                taskData
            }
            tasks
        } catch (apiError: Exception) {
            // Fallback to Room cache — returns in cached order
            try {
                db.taskDao().getAllTasks().map { entity ->
                    json.decodeFromString<TaskData>(entity.json)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Get a single task from cache.
     */
    suspend fun getTask(id: String): TaskData? {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSeedTasks().find { it.taskId == id }
        }

        return try {
            val entity = db.taskDao().getTask(id)
            entity?.let { json.decodeFromString<TaskData>(it.json) }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Perform a task action. Tries API; if offline, queues action
     * and returns optimistic update. On success, caches updated task.
     */
    suspend fun performAction(
        taskId: String,
        action: String,
        payload: Map<String, String>
    ): Result<TaskData?> {
        if (BuildConfig.USE_MOCK) {
            // Mock: return the current task unchanged (action is accepted optimistically)
            val current = SeedDataProvider.buildSeedTasks().find { it.taskId == taskId }
            return Result.success(current)
        }

        val body = buildJsonObject {
            put("taskId", JsonPrimitive(taskId))
            put("action", JsonPrimitive(action))
            put("payload", buildJsonObject {
                payload.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
            })
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.performTaskAction("Bearer $token", body)
            val taskJson = response["task"]?.jsonObject ?: response
            val updated = json.decodeFromJsonElement<TaskData>(taskJson)
            // Cache updated task
            db.taskDao().insertTask(
                TaskEntity(
                    taskId = updated.taskId,
                    json = taskJson.toString(),
                    taskType = updated.taskType,
                    currentState = updated.currentState,
                    priority = updated.priority
                )
            )
            Result.success(updated)
        } catch (e: Exception) {
            // Queue action for later retry
            try {
                actionQueueDao.insert(
                    ActionQueueEntity(
                        id = UUID.randomUUID().toString(),
                        endpoint = "/api/tasks/action",
                        payload = body.toString(),
                        createdAt = System.currentTimeMillis()
                    )
                )
                // Return optimistic update from cache
                val cached = getTask(taskId)
                Result.success(cached)
            } catch (queueError: Exception) {
                Result.failure(e)
            }
        }
    }
}
