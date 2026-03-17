package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.entity.TechnicianEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import com.wiom.csp.domain.model.TechnicianData
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    private val prefs: UserPreferences
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get all technicians: API-first, Room cache fallback.
     */
    suspend fun getTechnicians(): List<TechnicianData> {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSeedTechnicians()
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.getTechnicians("Bearer $token")
            val techArray = response["technicians"]?.jsonArray ?: JsonArray(emptyList())
            val technicians = techArray.map { element ->
                val tech = json.decodeFromJsonElement<TechnicianData>(element)
                db.technicianDao().insert(
                    TechnicianEntity(id = tech.id, json = element.toString())
                )
                tech
            }
            technicians
        } catch (apiError: Exception) {
            try {
                db.technicianDao().getAll().map { entity ->
                    json.decodeFromString<TechnicianData>(entity.json)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Add a new technician. Tries API; on success caches to Room.
     */
    suspend fun addTechnician(name: String, phone: String): Result<TechnicianData> {
        if (BuildConfig.USE_MOCK) {
            // Mock: create a new technician inline
            val tech = TechnicianData(
                id = "TECH-${System.currentTimeMillis()}",
                name = name,
                phone = phone,
                band = "B1",
                available = true,
                joinDate = System.currentTimeMillis()
            )
            return Result.success(tech)
        }

        val body = buildJsonObject {
            put("name", JsonPrimitive(name))
            put("phone", JsonPrimitive(phone))
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.addTechnician("Bearer $token", body)
            val tech = json.decodeFromJsonElement<TechnicianData>(response)
            db.technicianDao().insert(
                TechnicianEntity(id = tech.id, json = json.encodeToString(tech))
            )
            Result.success(tech)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
