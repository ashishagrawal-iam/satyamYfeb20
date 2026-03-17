package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.entity.SupportCaseEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import com.wiom.csp.domain.model.SupportCaseData
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    private val prefs: UserPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get all support cases: API-first, Room cache fallback.
     */
    suspend fun getCases(): List<SupportCaseData> {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSeedSupportCases()
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.getSupportCases("Bearer $token")
            val casesArray = response["cases"]?.jsonArray ?: JsonArray(emptyList())
            val cases = casesArray.map { element ->
                val caseData = json.decodeFromJsonElement<SupportCaseData>(element)
                db.supportCaseDao().insert(
                    SupportCaseEntity(id = caseData.id, json = element.toString())
                )
                caseData
            }
            cases
        } catch (apiError: Exception) {
            try {
                db.supportCaseDao().getAll().map { entity ->
                    json.decodeFromString<SupportCaseData>(entity.json)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Create a new support case. Returns the created case on success.
     */
    suspend fun createCase(
        subject: String,
        description: String,
        linkedTaskId: String?
    ): Result<SupportCaseData> {
        if (BuildConfig.USE_MOCK) {
            // Mock: create a new case inline
            val case = SupportCaseData(
                id = "CASE-${System.currentTimeMillis()}",
                subject = subject,
                status = "open",
                linkedTaskId = linkedTaskId,
                messages = emptyList(),
                createdAt = System.currentTimeMillis()
            )
            return Result.success(case)
        }

        val body = buildJsonObject {
            put("subject", JsonPrimitive(subject))
            put("description", JsonPrimitive(description))
            linkedTaskId?.let { put("linkedTaskId", JsonPrimitive(it)) }
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.createSupportCase("Bearer $token", body)
            val caseData = json.decodeFromJsonElement<SupportCaseData>(response)
            db.supportCaseDao().insert(
                SupportCaseEntity(id = caseData.id, json = json.encodeToString(caseData))
            )
            Result.success(caseData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reply to an existing support case.
     */
    suspend fun replyToCase(caseId: String, text: String): Result<SupportCaseData> {
        if (BuildConfig.USE_MOCK) {
            // Mock: find the case and return it (reply accepted optimistically)
            val existing = SeedDataProvider.buildSeedSupportCases().find { it.id == caseId }
            return if (existing != null) {
                Result.success(existing)
            } else {
                Result.failure(IllegalArgumentException("Case not found: $caseId"))
            }
        }

        val body = buildJsonObject {
            put("text", JsonPrimitive(text))
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.replySupportCase("Bearer $token", caseId, body)
            val caseData = json.decodeFromJsonElement<SupportCaseData>(response)
            db.supportCaseDao().insert(
                SupportCaseEntity(id = caseData.id, json = json.encodeToString(caseData))
            )
            Result.success(caseData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
