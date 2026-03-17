package com.wiom.csp.ui.technician

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.preferences.UserPreferences
import com.wiom.csp.data.repository.TaskRepository
import com.wiom.csp.data.repository.TeamRepository
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.domain.model.TechnicianData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Navigation views within the technician sub-app. */
enum class TechView { LOGIN, DASHBOARD, TASK_DETAIL, PROFILE }

@HiltViewModel
class TechViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val teamRepo: TeamRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _view = MutableStateFlow(TechView.LOGIN)
    val view: StateFlow<TechView> = _view.asStateFlow()

    private val _technicians = MutableStateFlow<List<TechnicianData>>(emptyList())
    val technicians: StateFlow<List<TechnicianData>> = _technicians.asStateFlow()

    private val _tech = MutableStateFlow<TechnicianData?>(null)
    val tech: StateFlow<TechnicianData?> = _tech.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskData>>(emptyList())
    val tasks: StateFlow<List<TaskData>> = _tasks.asStateFlow()

    private val _selectedTaskId = MutableStateFlow<String?>(null)
    val selectedTaskId: StateFlow<String?> = _selectedTaskId.asStateFlow()

    private val _confirmMessage = MutableStateFlow<String?>(null)
    val confirmMessage: StateFlow<String?> = _confirmMessage.asStateFlow()

    private var pollJob: Job? = null

    init {
        checkStoredTechId()
        loadTechnicians()
    }

    /**
     * Check if there is a stored tech ID for auto-login.
     */
    private fun checkStoredTechId() {
        viewModelScope.launch {
            val storedId = prefs.getTechId()
            if (storedId != null) {
                // Wait for technicians list to load, then auto-login
                loadTechnicians()
                // Attempt auto-login after technicians load
                val techs = teamRepo.getTechnicians()
                val found = techs.find { it.id == storedId }
                if (found != null) {
                    _tech.value = found
                    _view.value = TechView.DASHBOARD
                    startTaskPolling(storedId)
                }
            }
        }
    }

    /**
     * Load the list of technicians from the CSP.
     */
    private fun loadTechnicians() {
        viewModelScope.launch {
            try {
                val techs = teamRepo.getTechnicians()
                _technicians.value = techs
            } catch (_: Exception) {
                // Keep existing list or empty
            }
        }
    }

    /**
     * Login as a specific technician. No password required.
     */
    fun loginAs(technician: TechnicianData) {
        viewModelScope.launch {
            _tech.value = technician
            prefs.setTechId(technician.id)
            _view.value = TechView.DASHBOARD
            startTaskPolling(technician.id)
        }
    }

    /**
     * Navigate to a specific view.
     */
    fun navigateTo(target: TechView) {
        _view.value = target
    }

    /**
     * Select a task to view its detail.
     */
    fun selectTask(taskId: String) {
        _selectedTaskId.value = taskId
        _view.value = TechView.TASK_DETAIL
    }

    /**
     * Handle a task action: call the task repository and show confirmation.
     */
    fun handleAction(taskId: String, actionId: String, params: Map<String, String>) {
        viewModelScope.launch {
            val result = taskRepo.performAction(taskId, actionId, params)
            result.fold(
                onSuccess = { updatedTask ->
                    _confirmMessage.value = "Action completed"
                    // Refresh tasks
                    refreshTasks()
                },
                onFailure = { error ->
                    _confirmMessage.value = error.message ?: "Action failed"
                }
            )
        }
    }

    /**
     * Toggle the current technician's availability.
     * Updates local state immediately (optimistic); a real API call would follow.
     */
    fun toggleAvailability() {
        val current = _tech.value ?: return
        val updated = current.copy(available = !current.available)
        _tech.value = updated
        // Update in the technicians list as well
        _technicians.value = _technicians.value.map {
            if (it.id == current.id) updated else it
        }
    }

    /**
     * Dismiss the confirmation message.
     */
    fun dismissConfirmation() {
        _confirmMessage.value = null
    }

    /**
     * Logout: clear tech ID from preferences and reset to LOGIN view.
     */
    fun logout() {
        viewModelScope.launch {
            pollJob?.cancel()
            prefs.setTechId(null)
            _tech.value = null
            _tasks.value = emptyList()
            _selectedTaskId.value = null
            _view.value = TechView.LOGIN
        }
    }

    /**
     * Start polling for tasks every 30 seconds.
     * Only loads tasks assigned to the current technician.
     */
    private fun startTaskPolling(techId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val allTasks = taskRepo.getTasks()
                    _tasks.value = allTasks.filter { it.assignedTo == techId }
                } catch (_: Exception) {
                    // Keep last known list on failure
                }
                delay(30_000L)
            }
        }
    }

    /**
     * Single refresh of tasks (after action).
     */
    private suspend fun refreshTasks() {
        val techId = _tech.value?.id ?: return
        try {
            val allTasks = taskRepo.getTasks()
            _tasks.value = allTasks.filter { it.assignedTo == techId }
        } catch (_: Exception) {
            // Keep last known list
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
