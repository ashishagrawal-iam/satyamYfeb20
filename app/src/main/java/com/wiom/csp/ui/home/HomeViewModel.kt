package com.wiom.csp.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.repository.AssuranceRepository
import com.wiom.csp.data.repository.NotificationRepository
import com.wiom.csp.data.repository.TaskRepository
import com.wiom.csp.data.repository.WalletRepository
import com.wiom.csp.data.sync.SyncOrchestrator
import com.wiom.csp.domain.model.AppNotification
import com.wiom.csp.domain.model.AssuranceData
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.domain.model.WalletData
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.feedback.AudioFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen. Manages task feed, assurance data,
 * wallet state, notifications, and user preferences.
 *
 * Tasks are never sorted locally — server order is preserved (build rule #5).
 * All display resolution flows through [SchemaResolver].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val taskRepo: TaskRepository,
    private val assuranceRepo: AssuranceRepository,
    private val walletRepo: WalletRepository,
    private val notificationRepo: NotificationRepository,
    private val schemaResolver: SchemaResolver,
    private val syncOrchestrator: SyncOrchestrator,
    private val preferences: UserPreferences
) : ViewModel() {

    data class AutoRechargePopup(
        val customerCount: Int,
        val totalEarned: Double
    )

    data class HomeUiState(
        val tasks: List<TaskData> = emptyList(),
        val assurance: AssuranceData? = null,
        val wallet: WalletData? = null,
        val notifications: List<AppNotification> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val isOffline: Boolean = false,
        val selectedTaskId: String? = null,
        val activeSection: String? = null,
        val menuOpen: Boolean = false,
        val activeFilter: String = "All",
        val confirmMessage: String? = null,
        val urgentNotification: AppNotification? = null,
        val fadingTasks: Map<String, Boolean> = emptyMap(),
        val offersEnabled: Boolean = true,
        val hindi: Boolean = false,
        val darkTheme: Boolean = true, // default dark
        val capabilityResetActive: Boolean = false,
        val installationTaskId: String? = null,
        val rechargeTaskId: String? = null,
        val rechargePhase: Int = 1,
        val phase0TaskId: String? = null,
        val dashboardOpen: Boolean = false,
        val autoRechargePopup: AutoRechargePopup? = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private const val KEY_SELECTED_TASK = "selectedTaskId"
        private const val KEY_ACTIVE_SECTION = "activeSection"
        private const val KEY_MENU_OPEN = "menuOpen"
        private const val NOTIFICATION_POLL_INTERVAL_MS = 30_000L
    }

    init {
        // Restore saved state
        _uiState.update { current ->
            current.copy(
                selectedTaskId = savedStateHandle.get<String>(KEY_SELECTED_TASK),
                activeSection = savedStateHandle.get<String>(KEY_ACTIVE_SECTION),
                menuOpen = savedStateHandle.get<Boolean>(KEY_MENU_OPEN) ?: false
            )
        }

        // Load preferences
        viewModelScope.launch {
            val lang = preferences.getLanguage()
            val theme = preferences.getTheme()
            val offers = preferences.getOffersEnabled()
            _uiState.update { current ->
                current.copy(
                    hindi = lang == "hi",
                    darkTheme = theme == "DARK",
                    offersEnabled = offers
                )
            }
        }

        // Load initial data
        loadInitialData()

        // Start periodic sync (30s interval)
        syncOrchestrator.startPeriodicSync(viewModelScope)

        // Start notification polling (30s interval)
        startNotificationPolling()
    }

    /**
     * Fetches tasks, assurance, and wallet in parallel.
     * If ALL fail, sets isOffline = true.
     */
    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isOffline = false) }

            var tasksResult: List<TaskData>? = null
            var assuranceResult: AssuranceData? = null
            var walletResult: WalletData? = null
            var tasksFailed = false
            var assuranceFailed = false
            var walletFailed = false

            coroutineScope {
                val tasksDeferred = async {
                    try {
                        taskRepo.getTasks()
                    } catch (_: Exception) {
                        tasksFailed = true
                        null
                    }
                }
                val assuranceDeferred = async {
                    try {
                        assuranceRepo.getAssurance()
                    } catch (_: Exception) {
                        assuranceFailed = true
                        null
                    }
                }
                val walletDeferred = async {
                    try {
                        walletRepo.getWallet()
                    } catch (_: Exception) {
                        walletFailed = true
                        null
                    }
                }

                tasksResult = tasksDeferred.await()
                assuranceResult = assuranceDeferred.await()
                walletResult = walletDeferred.await()
            }

            val allFailed = tasksFailed && assuranceFailed && walletFailed

            _uiState.update { current ->
                current.copy(
                    tasks = tasksResult ?: current.tasks,
                    assurance = assuranceResult ?: current.assurance,
                    wallet = walletResult ?: current.wallet,
                    isLoading = false,
                    isOffline = allFailed,
                    capabilityResetActive = assuranceResult?.capabilityResetActive
                        ?: current.capabilityResetActive
                )
            }
        }
    }

    /**
     * Pull-to-refresh: reloads all data, then clears the refreshing indicator.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            var tasksResult: List<TaskData>? = null
            var assuranceResult: AssuranceData? = null
            var walletResult: WalletData? = null

            coroutineScope {
                val tasksDeferred = async {
                    try { taskRepo.getTasks() } catch (_: Exception) { null }
                }
                val assuranceDeferred = async {
                    try { assuranceRepo.getAssurance() } catch (_: Exception) { null }
                }
                val walletDeferred = async {
                    try { walletRepo.getWallet() } catch (_: Exception) { null }
                }

                tasksResult = tasksDeferred.await()
                assuranceResult = assuranceDeferred.await()
                walletResult = walletDeferred.await()
            }

            _uiState.update { current ->
                current.copy(
                    tasks = tasksResult ?: current.tasks,
                    assurance = assuranceResult ?: current.assurance,
                    wallet = walletResult ?: current.wallet,
                    isRefreshing = false,
                    isOffline = false,
                    capabilityResetActive = assuranceResult?.capabilityResetActive
                        ?: current.capabilityResetActive
                )
            }
        }
    }

    /**
     * Perform a task action (accept, decline, complete, etc.).
     * Shows a confirmation message on success.
     * For terminal states, triggers the fade-out animation (1.5s) then removes the task.
     */
    fun handleTaskAction(
        taskId: String,
        action: String,
        payload: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            val result = taskRepo.performAction(taskId, action, payload)

            result.onSuccess { updatedTask ->
                // Find the task's type and current state for terminal check
                val task = updatedTask
                    ?: _uiState.value.tasks.find { it.taskId == taskId }

                val confirmText = if (task != null) {
                    val actionSchema = schemaResolver.resolveActions(task.taskType, task.currentState)
                        .find { it.id == action }
                    val isHindi = _uiState.value.hindi
                    if (isHindi) actionSchema?.confirmMessageHi ?: actionSchema?.confirmMessage
                    else actionSchema?.confirmMessage
                } else null

                _uiState.update { current ->
                    current.copy(
                        confirmMessage = confirmText ?: "Action completed",
                        tasks = if (updatedTask != null) {
                            current.tasks.map { if (it.taskId == taskId) updatedTask else it }
                        } else {
                            current.tasks
                        }
                    )
                }

                // Check if the resulting state is terminal → trigger fade
                if (updatedTask != null) {
                    val isTerminal = schemaResolver.isTerminalState(
                        updatedTask.taskType,
                        updatedTask.currentState
                    )
                    if (isTerminal) {
                        // Mark as fading
                        _uiState.update { current ->
                            current.copy(
                                fadingTasks = current.fadingTasks + (taskId to true)
                            )
                        }
                        // After 1.5s, remove from task list and fading map
                        delay(1500L)
                        _uiState.update { current ->
                            current.copy(
                                tasks = current.tasks.filter { it.taskId != taskId },
                                fadingTasks = current.fadingTasks - taskId
                            )
                        }
                    }
                }
            }

            result.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        confirmMessage = "Action failed: ${error.message ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    /**
     * Select a task by ID (or null to deselect). Persisted to savedStateHandle.
     */
    fun selectTask(taskId: String?) {
        _uiState.update { it.copy(selectedTaskId = taskId) }
        savedStateHandle[KEY_SELECTED_TASK] = taskId
    }

    /**
     * Set the active navigation section. Persisted to savedStateHandle.
     */
    fun setActiveSection(section: String?) {
        _uiState.update { it.copy(activeSection = section) }
        savedStateHandle[KEY_ACTIVE_SECTION] = section
    }

    /**
     * Toggle the navigation drawer. Persisted to savedStateHandle.
     */
    fun toggleMenu() {
        val newValue = !_uiState.value.menuOpen
        _uiState.update { it.copy(menuOpen = newValue) }
        savedStateHandle[KEY_MENU_OPEN] = newValue
    }

    /**
     * Set the active task type filter.
     */
    fun setFilter(filter: String) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    /**
     * Dismiss the confirmation toast.
     */
    fun dismissConfirmation() {
        _uiState.update { it.copy(confirmMessage = null) }
    }

    /**
     * Dismiss the urgent notification modal and notify the repository.
     */
    fun dismissUrgentNotification() {
        val notif = _uiState.value.urgentNotification ?: return
        _uiState.update { it.copy(urgentNotification = null) }
        viewModelScope.launch {
            notificationRepo.dismiss(notif.id)
        }
    }

    /**
     * Toggle dark/light theme and persist the preference.
     */
    fun toggleTheme() {
        val newDark = !_uiState.value.darkTheme
        _uiState.update { it.copy(darkTheme = newDark) }
        viewModelScope.launch {
            preferences.setTheme(if (newDark) "DARK" else "LIGHT")
        }
    }

    /**
     * Toggle Hindi/English language and persist the preference.
     */
    fun toggleLanguage() {
        val newHindi = !_uiState.value.hindi
        _uiState.update { it.copy(hindi = newHindi) }
        viewModelScope.launch {
            preferences.setLanguage(if (newHindi) "hi" else "en")
        }
    }

    /**
     * Expose the schema resolver for composables that need it.
     */
    fun getSchemaResolver(): SchemaResolver = schemaResolver

    // ── Installation Flow ───────────────────────────────────────────

    /**
     * Open the 13-step installation flow for a task.
     * Clears selectedTask so detail overlay closes.
     */
    fun startInstallation(taskId: String) {
        _uiState.update { it.copy(installationTaskId = taskId, selectedTaskId = null) }
    }

    /**
     * Complete the installation flow — fires the INSTALL action to move
     * the task to INSTALLED state, then closes the flow.
     */
    fun finishInstallation(taskId: String) {
        _uiState.update { it.copy(installationTaskId = null) }
        handleTaskAction(taskId, "INSTALL")
    }

    /**
     * Cancel the installation flow without completing.
     */
    fun cancelInstallation() {
        _uiState.update { it.copy(installationTaskId = null) }
    }

    // ── ISP Recharge Flow ───────────────────────────────────────────

    /**
     * Open the ISP Recharge flow for a task.
     * Clears selectedTask so detail overlay closes.
     */
    fun startRecharge(taskId: String) {
        _uiState.update { it.copy(rechargeTaskId = taskId, selectedTaskId = null) }
    }

    /**
     * Complete the recharge flow — fires the ACKNOWLEDGE_RECHARGE action to move
     * the task to COMPLETED state, then closes the flow.
     */
    fun finishRecharge(taskId: String) {
        _uiState.update { it.copy(rechargeTaskId = null) }
        handleTaskAction(taskId, "ACKNOWLEDGE_RECHARGE")
    }

    /**
     * Cancel the recharge flow without completing.
     */
    fun cancelRecharge() {
        _uiState.update { it.copy(rechargeTaskId = null) }
    }

    // ── Phase 0: Acknowledge Flow ───────────────────────────────────

    fun startPhase0Acknowledge(taskId: String) {
        _uiState.update { it.copy(phase0TaskId = taskId, selectedTaskId = null) }
    }

    fun finishPhase0Acknowledge(taskId: String) {
        _uiState.update { it.copy(phase0TaskId = null) }
        handleTaskAction(taskId, "ACKNOWLEDGE_RECHARGE")
    }

    fun cancelPhase0Acknowledge() {
        _uiState.update { it.copy(phase0TaskId = null) }
    }

    // ── Dev Dashboard ───────────────────────────────────────────────────

    fun openDashboard() {
        _uiState.update { it.copy(dashboardOpen = true) }
    }

    fun closeDashboard() {
        _uiState.update { it.copy(dashboardOpen = false) }
    }

    /**
     * Set the ISP recharge phase. This changes the recharge card's behavior
     * on the home feed — the user then interacts with the card naturally.
     * Phase 0: Card shows "Mark as Recharged" only
     * Phase 1: Card shows "Start Recharge" → opens Phase 1 flow
     * Phase 2: Card shows "Batch Recharge" → opens Phase 2 flow
     * Phase 3: Card shows "Auto Recharge" status (automatic)
     */
    fun setRechargePhase(phase: Int) {
        if (phase == 3) {
            // Phase 3: No card — remove RECHARGE tasks from feed, show auto-recharge popup
            val customers = com.wiom.csp.mock.SeedDataProvider.buildSeedRechargeCustomers()
            val totalShare = customers.sumOf { it.shareAmount }
            _uiState.update {
                it.copy(
                    rechargePhase = phase,
                    dashboardOpen = false,
                    tasks = it.tasks.filter { task -> task.taskType != "RECHARGE" },
                    autoRechargePopup = AutoRechargePopup(
                        customerCount = customers.size,
                        totalEarned = totalShare
                    )
                )
            }
            return
        }

        // Phase 0/1/2: update schema actions and restore RECHARGE tasks
        val freshSchema = com.wiom.csp.mock.SeedDataProvider.buildSchema()
        val rechargeType = freshSchema.taskTypes["RECHARGE"] ?: return
        val pendingState = rechargeType.states["PENDING_RECHARGE"] ?: return

        val newActions = when (phase) {
            0 -> listOf(
                pendingState.actions.find { it.id == "ACKNOWLEDGE_RECHARGE" }!!
            )
            1 -> pendingState.actions.sortedBy { if (it.id == "START_RECHARGE") 0 else 1 }
            2 -> listOf(
                pendingState.actions.find { it.id == "START_RECHARGE" }!!.copy(
                    label = "Batch Recharge", labelHi = "\u092C\u0948\u091A \u0930\u093F\u091A\u093E\u0930\u094D\u091C"
                )
            )
            else -> pendingState.actions
        }

        // Rebuild full schema with only the RECHARGE actions changed
        val currentSchema = schemaResolver.getSchema()

        val updatedPendingState = pendingState.copy(actions = newActions)
        val updatedRechargeType = rechargeType.copy(
            states = rechargeType.states + ("PENDING_RECHARGE" to updatedPendingState)
        )
        val updatedSchema = currentSchema.copy(
            taskTypes = currentSchema.taskTypes + ("RECHARGE" to updatedRechargeType)
        )
        schemaResolver.updateSchema(updatedSchema)

        // Restore RECHARGE tasks if they were removed by Phase 3
        val hasRechargeTask = _uiState.value.tasks.any { it.taskType == "RECHARGE" }
        val restoredTasks = if (!hasRechargeTask) {
            _uiState.value.tasks + com.wiom.csp.mock.SeedDataProvider.buildSeedTasks().filter { it.taskType == "RECHARGE" }
        } else {
            _uiState.value.tasks.toList()
        }

        _uiState.update {
            it.copy(
                rechargePhase = phase,
                dashboardOpen = false,
                tasks = restoredTasks,
                confirmMessage = "Switched to ISP Recharge Phase $phase"
            )
        }
    }

    fun dismissAutoRechargePopup() {
        _uiState.update { it.copy(autoRechargePopup = null) }
    }

    // ── Private: Notification Polling ───────────────────────────────────

    /**
     * Polls for notifications every 30 seconds. If any notification is urgent
     * (per schema), surfaces it as an EventModal and triggers audio + haptic feedback.
     */
    private fun startNotificationPolling() {
        viewModelScope.launch {
            while (true) {
                delay(NOTIFICATION_POLL_INTERVAL_MS)
                try {
                    val notifications = notificationRepo.getNotifications()
                    _uiState.update { it.copy(notifications = notifications) }

                    // Check for urgent notifications that haven't been dismissed
                    val urgent = notifications.firstOrNull { notif ->
                        val typeSchema = schemaResolver.resolveNotificationType(notif.type)
                        typeSchema?.isUrgent == true && !notif.dismissed
                    }

                    if (urgent != null && _uiState.value.urgentNotification == null) {
                        _uiState.update { it.copy(urgentNotification = urgent) }
                        // Trigger audio and haptic feedback for urgent notifications
                        AudioFeedback.playUrgent()
                        // Note: HapticFeedback requires a View reference which is not
                        // available in ViewModel. The composable layer should observe
                        // urgentNotification and trigger haptics via LocalView.
                    }
                } catch (_: Exception) {
                    // Notification polling is best-effort
                }
            }
        }
    }
}
