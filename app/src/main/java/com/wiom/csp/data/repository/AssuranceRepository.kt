package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.entity.AssuranceEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import com.wiom.csp.domain.model.AssuranceData
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssuranceRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    private val prefs: UserPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get assurance data: API-first → Room cache → seed fallback.
     */
    suspend fun getAssurance(): AssuranceData {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSeedAssurance()
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.getAssurance("Bearer $token")
            val raw = response.toString()
            val data = json.decodeFromString<AssuranceData>(raw)
            db.singleCacheDao().insertAssurance(AssuranceEntity(json = raw))
            data
        } catch (apiError: Exception) {
            try {
                val cached = db.singleCacheDao().getAssurance()
                if (cached != null) {
                    json.decodeFromString<AssuranceData>(cached.json)
                } else {
                    SeedDataProvider.buildSeedAssurance()
                }
            } catch (_: Exception) {
                SeedDataProvider.buildSeedAssurance()
            }
        }
    }
}
