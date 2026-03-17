package com.wiom.csp.data.sync

import com.wiom.csp.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates all data sync operations. Each sync is independent —
 * one failure never blocks others.
 */
@Singleton
class SyncOrchestrator @Inject constructor(
    private val schemaRepo: SchemaRepository,
    private val taskRepo: TaskRepository,
    private val walletRepo: WalletRepository,
    private val assuranceRepo: AssuranceRepository,
    private val slaRepo: SlaRepository,
    private val teamRepo: TeamRepository,
    private val supportRepo: SupportRepository,
    private val depositRepo: DepositRepository,
    private val notificationRepo: NotificationRepository,
    private val themeRepo: ThemeRepository,
    private val actionQueue: ActionQueue
) {

    private companion object {
        const val PERIODIC_SYNC_INTERVAL_MS = 30_000L // 30 seconds
    }

    /**
     * Start periodic delta sync. Launches a coroutine that runs every 30s.
     * Each data type syncs independently — one failure won't block others.
     */
    fun startPeriodicSync(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(PERIODIC_SYNC_INTERVAL_MS)
                runDeltaSync()
            }
        }
    }

    /**
     * Full refresh on app open. Syncs all data types and processes
     * any queued offline actions.
     */
    fun syncAll(scope: CoroutineScope) {
        scope.launch {
            // Process queued actions first — they may affect server state
            processQueueInternal()
            // Then pull fresh data
            runFullSync()
        }
    }

    /**
     * Process offline action queue. Retries queued actions that were
     * created while offline.
     */
    fun processActionQueue(scope: CoroutineScope) {
        scope.launch {
            processQueueInternal()
        }
    }

    // ── Internal sync methods ───────────────────────────────────────

    private suspend fun runDeltaSync() {
        // Delta sync: lightweight, frequent updates
        safeLaunch { taskRepo.getTasks() }
        safeLaunch { notificationRepo.getNotifications() }
        safeLaunch { walletRepo.getWallet() }
        safeLaunch { assuranceRepo.getAssurance() }
    }

    private suspend fun runFullSync() {
        safeLaunch { schemaRepo.refreshSchema() }
        safeLaunch { taskRepo.getTasks() }
        safeLaunch { walletRepo.getWallet() }
        safeLaunch { assuranceRepo.getAssurance() }
        safeLaunch { slaRepo.getSla() }
        safeLaunch { teamRepo.getTechnicians() }
        safeLaunch { supportRepo.getCases() }
        safeLaunch { depositRepo.getDeposit() }
        safeLaunch { notificationRepo.getNotifications() }
        safeLaunch { themeRepo.getTheme() }
    }

    private suspend fun processQueueInternal() {
        try {
            actionQueue.processQueue()
        } catch (_: Exception) {
            // Queue processing is best-effort
        }
    }

    /**
     * Execute a suspend block, swallowing any exception so one sync
     * failure never cascades to others.
     */
    private suspend fun safeLaunch(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            // Individual sync failure — silently continue
        }
    }
}
