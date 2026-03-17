package com.wiom.csp.data.repository

import com.wiom.csp.BuildConfig
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.ActionQueueDao
import com.wiom.csp.data.db.entity.ActionQueueEntity
import com.wiom.csp.data.db.entity.WalletEntity
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.ApiService
import com.wiom.csp.domain.model.WalletData
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.serialization.json.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    private val prefs: UserPreferences,
    private val actionQueueDao: ActionQueueDao
) {

    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Get wallet data: API-first, Room cache fallback.
     */
    suspend fun getWallet(): WalletData {
        if (BuildConfig.USE_MOCK) {
            return SeedDataProvider.buildSeedWallet()
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.getWallet("Bearer $token")
            val raw = response.toString()
            val wallet = json.decodeFromString<WalletData>(raw)
            db.singleCacheDao().insertWallet(WalletEntity(json = raw))
            wallet
        } catch (apiError: Exception) {
            try {
                val cached = db.singleCacheDao().getWallet()
                if (cached != null) {
                    json.decodeFromString<WalletData>(cached.json)
                } else {
                    WalletData()
                }
            } catch (_: Exception) {
                WalletData()
            }
        }
    }

    /**
     * Withdraw from wallet. Validates:
     * - amount > 0 and <= balance
     * - 7-day cooldown since last withdrawal
     * Tries API; if offline, queues action.
     */
    suspend fun withdraw(amount: Double): Result<WalletData> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Withdrawal amount must be positive"))
        }

        val currentWallet = getWallet()
        if (amount > currentWallet.balance) {
            return Result.failure(IllegalArgumentException("Insufficient balance"))
        }

        // Check 7-day cooldown
        val lastWithdrawal = currentWallet.lastWithdrawalAt
        if (lastWithdrawal != null) {
            val elapsed = System.currentTimeMillis() - lastWithdrawal
            if (elapsed < SEVEN_DAYS_MS) {
                val daysRemaining = ((SEVEN_DAYS_MS - elapsed) / (24 * 60 * 60 * 1000)) + 1
                return Result.failure(
                    IllegalStateException("Withdrawal cooldown active. Try again in $daysRemaining days.")
                )
            }
        }

        if (BuildConfig.USE_MOCK) {
            // Mock: return wallet with reduced balance
            val w = SeedDataProvider.buildSeedWallet()
            return Result.success(
                w.copy(
                    balance = w.balance - amount,
                    lastWithdrawalAt = System.currentTimeMillis()
                )
            )
        }

        val body = buildJsonObject {
            put("type", JsonPrimitive("withdraw"))
            put("amount", JsonPrimitive(amount))
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.performWalletAction("Bearer $token", body)
            val raw = response.toString()
            val wallet = json.decodeFromString<WalletData>(raw)
            db.singleCacheDao().insertWallet(WalletEntity(json = raw))
            Result.success(wallet)
        } catch (e: Exception) {
            actionQueueDao.insert(
                ActionQueueEntity(
                    id = UUID.randomUUID().toString(),
                    endpoint = "/api/wallet/action",
                    payload = body.toString(),
                    createdAt = System.currentTimeMillis()
                )
            )
            Result.failure(e)
        }
    }

    /**
     * Add money to wallet via specified method. Tries API; if offline, queues.
     */
    suspend fun addMoney(amount: Double, method: String): Result<WalletData> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }

        if (BuildConfig.USE_MOCK) {
            // Mock: return wallet with increased balance
            val w = SeedDataProvider.buildSeedWallet()
            return Result.success(w.copy(balance = w.balance + amount))
        }

        val body = buildJsonObject {
            put("type", JsonPrimitive("top_up"))
            put("amount", JsonPrimitive(amount))
            put("method", JsonPrimitive(method))
        }

        return try {
            val token = prefs.getToken() ?: throw IllegalStateException("No auth token")
            val response = api.performWalletAction("Bearer $token", body)
            val raw = response.toString()
            val wallet = json.decodeFromString<WalletData>(raw)
            db.singleCacheDao().insertWallet(WalletEntity(json = raw))
            Result.success(wallet)
        } catch (e: Exception) {
            actionQueueDao.insert(
                ActionQueueEntity(
                    id = UUID.randomUUID().toString(),
                    endpoint = "/api/wallet/action",
                    payload = body.toString(),
                    createdAt = System.currentTimeMillis()
                )
            )
            Result.failure(e)
        }
    }
}
