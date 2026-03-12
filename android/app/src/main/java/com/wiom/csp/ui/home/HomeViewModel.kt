package com.wiom.csp.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.remote.dto.AssuranceUpdateRequest
import com.wiom.csp.data.remote.dto.NotificationPostRequest
import com.wiom.csp.data.repository.*
import com.wiom.csp.domain.model.*
import com.wiom.csp.domain.usecase.GetBucketUseCase
import com.wiom.csp.domain.usecase.SortTasksUseCase
import android.content.Context
import com.wiom.csp.R
import com.wiom.csp.feedback.AudioFeedback
import com.wiom.csp.feedback.HapticFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.time.Instant
import javax.inject.Inject

data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val assurance: AssuranceState? = null,
    val wallet: WalletState? = null,
    val technicians: List<Technician> = emptyList(),
    val activeNotification: AppNotification? = null,
    val confirmMessage: String? = null,
    val selectedTaskId: String? = null,
    val menuOpen: Boolean = false,
    val assignPickerOpen: Boolean = false,
    val assignTaskId: String? = null,
    val activeSection: String? = null,
    val offersEnabled: Boolean = true,
    val currentTheme: AppTheme = AppTheme.DARK,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
    val fadingTasks: Map<String, Task> = emptyMap(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val assuranceRepo: AssuranceRepository,
    private val walletRepo: WalletRepository,
    private val notificationRepo: NotificationRepository,
    private val themeRepo: ThemeRepository,
    private val sortTasks: SortTasksUseCase,
    val getBucket: GetBucketUseCase,
    private val prefs: UserPreferences,
    @ApplicationContext private val appContext: Context,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // Restore navigation state from SavedStateHandle (process death recovery)
        savedStateHandle.get<String>("selectedTaskId")?.let { taskId ->
            _state.update { it.copy(selectedTaskId = taskId) }
        }
        savedStateHandle.get<String>("activeSection")?.let { section ->
            _state.update { it.copy(activeSection = section) }
        }
        savedStateHandle.get<Boolean>("menuOpen")?.let { open ->
            _state.update { it.copy(menuOpen = open) }
        }

        loadInitialData()
        startPolling()
        observePreferences()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val tasksResult = taskRepo.getTasks()
            val assuranceResult = assuranceRepo.getAssurance()
            val walletResult = walletRepo.getWallet()

            val tasksFailed = tasksResult.isFailure
            val assuranceFailed = assuranceResult.isFailure
            val walletFailed = walletResult.isFailure
            val allFailed = tasksFailed && assuranceFailed && walletFailed

            _state.update { s ->
                s.copy(
                    tasks = tasksResult.getOrNull()?.let { sortTasks(it) } ?: emptyList(),
                    assurance = assuranceResult.getOrNull(),
                    wallet = walletResult.getOrNull(),
                    isLoading = false,
                    isOffline = allFailed,
                    error = if (allFailed) "Cannot reach server. Showing cached data." else null
                )
            }
        }
    }

    private fun startPolling() {
        // Assurance polling -- 2s
        viewModelScope.launch {
            while (true) {
                delay(2000)
                assuranceRepo.getAssurance().onSuccess { data ->
                    _state.update { it.copy(assurance = data, isOffline = false) }
                }
            }
        }

        // Notification polling -- 2s
        viewModelScope.launch {
            while (true) {
                delay(2000)
                notificationRepo.getNotifications().onSuccess { notifications ->
                    val current = _state.value
                    if (current.activeNotification == null) {
                        val offersOn = current.offersEnabled
                        val undismissed = notifications.firstOrNull { n ->
                            !n.dismissed && (offersOn || n.type != NotificationType.NEW_OFFER)
                        }
                        if (undismissed != null) {
                            _state.update { it.copy(activeNotification = undismissed) }
                            // Trigger audio + haptic feedback
                            when (undismissed.type) {
                                NotificationType.HIGH_RESTORE_ALERT -> {
                                    AudioFeedback.playUrgentSound()
                                    HapticFeedback.notifyUrgentAlert(appContext)
                                }
                                else -> {
                                    AudioFeedback.playNotificationSound()
                                    HapticFeedback.notifyNewConnection(appContext)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Theme polling -- 5s (reads from server, persisted to disk so hot-reloads don't reset)
        viewModelScope.launch {
            while (true) {
                delay(5000)
                themeRepo.getTheme().onSuccess { theme ->
                    _state.update { it.copy(currentTheme = theme) }
                    prefs.setTheme(theme)
                }
            }
        }

        // Wallet polling -- 5s
        viewModelScope.launch {
            while (true) {
                delay(5000)
                walletRepo.getWallet().onSuccess { data ->
                    _state.update { it.copy(wallet = data) }
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefs.offersEnabled.collect { enabled ->
                _state.update { it.copy(offersEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.theme.collect { theme ->
                _state.update { it.copy(currentTheme = theme) }
            }
        }
    }

    fun retryLoad() {
        _state.update { it.copy(isLoading = true, error = null) }
        loadInitialData()
    }

    /** Pull-to-refresh: reload tasks + assurance + wallet */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val tasksResult = taskRepo.getTasks()
            val assuranceResult = assuranceRepo.getAssurance()
            val walletResult = walletRepo.getWallet()
            _state.update { s ->
                s.copy(
                    tasks = tasksResult.getOrNull()?.let { sortTasks(it) } ?: s.tasks,
                    assurance = assuranceResult.getOrNull() ?: s.assurance,
                    wallet = walletResult.getOrNull() ?: s.wallet,
                    isRefreshing = false,
                    isOffline = tasksResult.isFailure && assuranceResult.isFailure && walletResult.isFailure,
                )
            }
        }
    }

    fun refreshTasks() {
        viewModelScope.launch {
            taskRepo.getTasks().onSuccess { data ->
                _state.update { it.copy(tasks = sortTasks(data)) }
            }
        }
    }

    val lifetimeEarnings: Int?
        get() {
            val wallet = _state.value.wallet ?: return null
            return wallet.transactions
                .filter { (it.type == WalletTransactionType.SETTLEMENT || it.type == WalletTransactionType.BONUS) && it.status == TransactionStatus.COMPLETED }
                .sumOf { it.amount }
        }

    // ---- UI Actions ----

    fun selectTask(taskId: String) {
        _state.update { it.copy(selectedTaskId = taskId) }
        savedStateHandle["selectedTaskId"] = taskId
    }

    fun clearSelectedTask() {
        _state.update { it.copy(selectedTaskId = null) }
        savedStateHandle["selectedTaskId"] = null
        refreshTasks()
    }

    fun openMenu() {
        _state.update { it.copy(menuOpen = true) }
        savedStateHandle["menuOpen"] = true
    }

    fun closeMenu() {
        _state.update { it.copy(menuOpen = false) }
        savedStateHandle["menuOpen"] = false
    }

    fun navigate(section: String) {
        _state.update { it.copy(activeSection = section) }
        savedStateHandle["activeSection"] = section
    }

    fun backToHome() {
        _state.update { it.copy(activeSection = null) }
        savedStateHandle["activeSection"] = null
        refreshTasks()
    }

    fun openAssignPicker(taskId: String) {
        _state.update { it.copy(assignPickerOpen = true, assignTaskId = taskId) }
    }

    fun closeAssignPicker() {
        _state.update { it.copy(assignPickerOpen = false, assignTaskId = null) }
    }

    fun dismissNotification() {
        val notification = _state.value.activeNotification ?: return
        viewModelScope.launch {
            notificationRepo.dismiss(notification.id)
            _state.update { it.copy(activeNotification = null) }
        }
    }

    fun showConfirmation(msg: String) {
        _state.update { it.copy(confirmMessage = msg) }
        viewModelScope.launch {
            delay(3000)
            _state.update { it.copy(confirmMessage = null) }
        }
    }

    fun setOffersEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setOffersEnabled(enabled) }
    }

    val currentLanguage: StateFlow<String> = prefs.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun setLanguage(lang: String) {
        viewModelScope.launch { prefs.setLanguage(lang) }
    }

    fun logout() {
        viewModelScope.launch { prefs.clearAuth() }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // ---- Task actions (port from page.tsx handleAction) ----

    fun handleAction(taskId: String, action: String, extra: Map<String, String> = emptyMap()) {
        val task = _state.value.tasks.find { it.taskId == taskId } ?: return
        val now = Instant.now().toString()

        fun newEvent(type: String, detail: String) = TimelineEvent(
            timestamp = now,
            eventType = type,
            actor = "CSP-MH-1001",
            actorType = ActorType.CSP,
            detail = detail
        )

        viewModelScope.launch {
            when (action) {
                "CLAIM" -> {
                    val acceptExpires = Instant.now().plusSeconds(900).toString()
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("CLAIMED"),
                        "offer_expires_at" to JsonNull,
                        "queue_escalation_flag" to JsonNull,
                        "accept_expires_at" to JsonPrimitive(acceptExpires),
                    ), newEvent("CLAIMED", appContext.getString(R.string.event_detail_claimed)))
                    AudioFeedback.playNotificationSound()
                    HapticFeedback.notifyNewConnection(appContext)
                    showConfirmation(appContext.getString(R.string.action_claimed, taskId))
                }
                "CLAIM_AND_ASSIGN" -> {
                    val slot = extra["preferred_slot"] ?: "Today"
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("ACCEPTED"),
                        "offer_expires_at" to JsonNull,
                        "accept_expires_at" to JsonNull,
                        "queue_escalation_flag" to JsonNull,
                    ), newEvent("CLAIMED", appContext.getString(R.string.event_detail_claimed_slot, slot)),
                        newEvent("ACCEPTED", appContext.getString(R.string.event_detail_accepted_slot, slot))
                    )
                    AudioFeedback.playNotificationSound()
                    HapticFeedback.notifyNewConnection(appContext)
                    showConfirmation(appContext.getString(R.string.action_claimed_and_assigned, taskId, slot))
                    _state.update { it.copy(selectedTaskId = null) }
                    refreshTasks()
                    openAssignPicker(taskId)
                    return@launch
                }
                "DECLINE" -> {
                    val reason = extra["reason"] ?: "No reason provided"
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("FAILED"),
                        "queue_escalation_flag" to JsonNull,
                    ), newEvent("DECLINED", appContext.getString(R.string.event_detail_declined, reason)))
                    showConfirmation(appContext.getString(R.string.action_declined, taskId))
                    _state.update { it.copy(selectedTaskId = null) }
                }
                "ACCEPT" -> {
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("ACCEPTED"),
                        "accept_expires_at" to JsonNull,
                        "queue_escalation_flag" to JsonNull,
                    ), newEvent("ACCEPTED", appContext.getString(R.string.event_detail_accepted)))
                    showConfirmation(appContext.getString(R.string.action_accepted, taskId))
                }
                "SCHEDULE", "ASSIGN" -> {
                    openAssignPicker(taskId)
                    return@launch
                }
                "START_WORK" -> {
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("IN_PROGRESS"),
                        "delegation_state" to JsonPrimitive("IN_PROGRESS"),
                    ), newEvent("IN_PROGRESS", appContext.getString(R.string.event_detail_work_started)))
                    showConfirmation(appContext.getString(R.string.action_work_started, taskId))
                }
                "RESOLVE" -> {
                    addFadingTask(taskId)
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("RESOLVED"),
                        "queue_escalation_flag" to JsonNull,
                        "delegation_state" to JsonPrimitive("DONE"),
                    ), newEvent("RESOLVED", appContext.getString(R.string.event_detail_resolved)))
                    showConfirmation(appContext.getString(R.string.action_resolved, taskId))
                }
                "RESOLVE_BLOCKED" -> {
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("IN_PROGRESS"),
                        "queue_escalation_flag" to JsonNull,
                        "blocked_reason" to JsonNull,
                    ), newEvent("UNBLOCKED", appContext.getString(R.string.event_detail_unblocked)))
                    showConfirmation(appContext.getString(R.string.action_unblocked, taskId))
                }
                "COLLECTED" -> {
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("COLLECTED"),
                    ), newEvent("COLLECTED", appContext.getString(R.string.event_detail_collected)))
                    showConfirmation(appContext.getString(R.string.action_collected, taskId))
                }
                "CONFIRM_RETURN" -> {
                    addFadingTask(taskId)
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("RETURN_CONFIRMED"),
                        "queue_escalation_flag" to JsonNull,
                        "delegation_state" to JsonPrimitive("DONE"),
                    ), newEvent("RETURN_CONFIRMED", appContext.getString(R.string.event_detail_return_confirmed)))
                    showConfirmation(appContext.getString(R.string.action_return_confirmed, taskId))
                }
                "VERIFY" -> {
                    addFadingTask(taskId)
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("ACTIVATION_VERIFIED"),
                        "queue_escalation_flag" to JsonNull,
                    ), newEvent("ACTIVATION_VERIFIED", appContext.getString(R.string.event_detail_verified)))
                    AudioFeedback.playNotificationSound()
                    HapticFeedback.notifyNewConnection(appContext)
                    showConfirmation(appContext.getString(R.string.action_verified, taskId))
                    // Credit earnings
                    assuranceRepo.updateAssurance(
                        AssuranceUpdateRequest(incrementCycleEarned = 300, incrementNextSettlement = 300)
                    )
                    notificationRepo.post(NotificationPostRequest(
                        id = "NOTIF-${System.currentTimeMillis()}",
                        type = "SETTLEMENT_CREDIT",
                        title = appContext.getString(R.string.action_install_earning_title),
                        message = appContext.getString(R.string.action_install_earning_message, task.connectionId ?: taskId),
                        timestamp = now,
                    ))
                }
                "INSTALL" -> {
                    postUpdate(taskId, task, mapOf(
                        "state" to JsonPrimitive("INSTALLED"),
                        "queue_escalation_flag" to JsonPrimitive("VERIFICATION_PENDING"),
                    ), newEvent("INSTALLED", appContext.getString(R.string.event_detail_installed)))
                    AudioFeedback.playNotificationSound()
                    HapticFeedback.notifyNewConnection(appContext)
                    showConfirmation(appContext.getString(R.string.action_installed, taskId))
                }
                "VIEW" -> {
                    selectTask(taskId)
                    return@launch
                }
            }

            // Check terminal state
            val updated = _state.value.tasks.find { it.taskId == taskId }
            if (updated != null && updated.isTerminal) {
                _state.update { it.copy(selectedTaskId = null) }
            }
            refreshTasks()
        }
    }

    fun doAssign(taskId: String, tech: Technician) {
        val task = _state.value.tasks.find { it.taskId == taskId } ?: return
        val now = Instant.now().toString()

        val nextState = when (task.taskType) {
            TaskType.RESTORE -> if (task.state == "ALERTED") "ASSIGNED" else "IN_PROGRESS"
            TaskType.NETBOX -> if (task.state == "PICKUP_REQUIRED") "ASSIGNED" else "IN_PROGRESS"
            TaskType.INSTALL -> if (task.state == "ACCEPTED") "SCHEDULED" else task.state
        }

        val event = TimelineEvent(
            timestamp = now,
            eventType = "ASSIGNED",
            actor = "CSP-MH-1001",
            actorType = ActorType.CSP,
            detail = appContext.getString(R.string.event_detail_assigned, tech.name, tech.id)
        )

        viewModelScope.launch {
            postUpdate(taskId, task, mapOf(
                "state" to JsonPrimitive(nextState),
                "delegation_state" to JsonPrimitive("ASSIGNED"),
                "assigned_to" to JsonPrimitive(tech.name),
            ), event)
            showConfirmation(appContext.getString(R.string.action_assigned, taskId, tech.name))
            _state.update { it.copy(selectedTaskId = null) }
            closeAssignPicker()
            refreshTasks()
        }
    }

    private suspend fun postUpdate(
        taskId: String,
        task: Task,
        fields: Map<String, JsonElement>,
        vararg newEvents: TimelineEvent
    ) {
        val eventLog = task.eventLog + newEvents.toList()
        val eventLogJson = Json.encodeToJsonElement(eventLog)
        val updates = fields.toMutableMap()
        updates["event_log"] = eventLogJson

        taskRepo.updateTask(taskId, updates)
    }

    private fun addFadingTask(taskId: String) {
        val task = _state.value.tasks.find { it.taskId == taskId } ?: return
        _state.update { it.copy(fadingTasks = it.fadingTasks + (taskId to task)) }
        viewModelScope.launch {
            delay(1500)
            _state.update { it.copy(fadingTasks = it.fadingTasks - taskId) }
        }
    }
}
