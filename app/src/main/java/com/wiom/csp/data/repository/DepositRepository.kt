package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.entity.DepositEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import com.wiom.csp.domain.model.DepositData
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DepositRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    private val prefs: UserPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get deposit ledger: API-first → Room cache → seed fallback.
     */
    suspend fun getDeposit(): DepositData {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSeedDeposit()
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.getDeposit("Bearer $token")
            val raw = response.toString()
            val data = json.decodeFromString<DepositData>(raw)
            db.singleCacheDao().insertDeposit(DepositEntity(json = raw))
            data
        } catch (apiError: Exception) {
            try {
                val cached = db.singleCacheDao().getDeposit()
                if (cached != null) {
                    json.decodeFromString<DepositData>(cached.json)
                } else {
                    SeedDataProvider.buildSeedDeposit()
                }
            } catch (_: Exception) {
                SeedDataProvider.buildSeedDeposit()
            }
        }
    }
}
