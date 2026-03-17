package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.entity.SlaEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import com.wiom.csp.domain.model.SlaData
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SlaRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    private val prefs: UserPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get SLA data: API-first → Room cache → seed fallback.
     */
    suspend fun getSla(): SlaData {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSeedSla()
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.getSla("Bearer $token")
            val raw = response.toString()
            val data = json.decodeFromString<SlaData>(raw)
            db.singleCacheDao().insertSla(SlaEntity(json = raw))
            data
        } catch (apiError: Exception) {
            try {
                val cached = db.singleCacheDao().getSla()
                if (cached != null) {
                    json.decodeFromString<SlaData>(cached.json)
                } else {
                    SeedDataProvider.buildSeedSla()
                }
            } catch (_: Exception) {
                SeedDataProvider.buildSeedSla()
            }
        }
    }
}
