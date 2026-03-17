package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.entity.SchemaEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import com.wiom.csp.domain.model.AppSchema
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemaRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    private val prefs: UserPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Primary schema getter: API-first → Room cache → seed fallback.
     * In mock mode, always returns seed data directly.
     */
    suspend fun getSchema(): AppSchema {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSchema()
        }

        // Try API first
        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.getSchema("Bearer $token")
            val raw = response.toString()
            val schema = json.decodeFromString<AppSchema>(raw)
            // Cache to Room on success
            db.schemaDao().insertSchema(
                SchemaEntity(
                    json = raw,
                    version = schema.version
                )
            )
            schema
        } catch (apiError: Exception) {
            // Fallback to Room cache
            try {
                val cached = db.schemaDao().getSchema()
                if (cached != null) {
                    json.decodeFromString<AppSchema>(cached.json)
                } else {
                    SeedDataProvider.buildSchema()
                }
            } catch (cacheError: Exception) {
                SeedDataProvider.buildSchema()
            }
        }
    }

    /**
     * Force-refresh schema from API or seed. Used on pull-to-refresh.
     */
    suspend fun refreshSchema() {
        if (BuildConfig.USE_MOCK) return

        try {
            val token = prefs.getToken() ?: return
            val response = api.getSchema("Bearer $token")
            val raw = response.toString()
            val schema = json.decodeFromString<AppSchema>(raw)
            db.schemaDao().insertSchema(
                SchemaEntity(
                    json = raw,
                    version = schema.version
                )
            )
        } catch (_: Exception) {
            // Refresh failed — existing cache remains valid
        }
    }
}
