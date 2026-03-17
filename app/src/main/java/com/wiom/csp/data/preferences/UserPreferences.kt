package com.wiom.csp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "wiom_prefs")

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val ctx: Context) {

    companion object {
        private val TOKEN = stringPreferencesKey("jwt_token")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val PARTNER_ID = stringPreferencesKey("partner_id")
        private val PROFILE_COMPLETE = booleanPreferencesKey("profile_complete")
        private val TECH_ID = stringPreferencesKey("tech_id")
        private val THEME = stringPreferencesKey("theme")
        private val LANGUAGE = stringPreferencesKey("language")
        private val OFFERS_ENABLED = booleanPreferencesKey("offers_enabled")
    }

    private val ds = ctx.dataStore

    suspend fun getToken(): String? = ds.data.map { it[TOKEN] }.first()
    suspend fun setToken(token: String) { ds.edit { it[TOKEN] = token } }
    suspend fun clearToken() { ds.edit { it.remove(TOKEN) } }

    suspend fun getUserName(): String? = ds.data.map { it[USER_NAME] }.first()
    suspend fun setUserName(name: String) { ds.edit { it[USER_NAME] = name } }

    suspend fun getPartnerId(): String? = ds.data.map { it[PARTNER_ID] }.first()
    suspend fun setPartnerId(id: String) { ds.edit { it[PARTNER_ID] = id } }

    suspend fun isProfileComplete(): Boolean = ds.data.map { it[PROFILE_COMPLETE] ?: false }.first()
    suspend fun setProfileComplete(v: Boolean) { ds.edit { it[PROFILE_COMPLETE] = v } }

    suspend fun getTechId(): String? = ds.data.map { it[TECH_ID] }.first()
    suspend fun setTechId(id: String?) { ds.edit { if (id != null) it[TECH_ID] = id else it.remove(TECH_ID) } }

    suspend fun getTheme(): String = ds.data.map { it[THEME] ?: "DARK" }.first()
    suspend fun setTheme(theme: String) { ds.edit { it[THEME] = theme } }

    suspend fun getLanguage(): String = ds.data.map { it[LANGUAGE] ?: "en" }.first()
    suspend fun setLanguage(lang: String) { ds.edit { it[LANGUAGE] = lang } }

    suspend fun getOffersEnabled(): Boolean = ds.data.map { it[OFFERS_ENABLED] ?: true }.first()
    suspend fun setOffersEnabled(v: Boolean) { ds.edit { it[OFFERS_ENABLED] = v } }

    suspend fun clearAll() { ds.edit { it.clear() } }
}
