package com.wiom.csp.ui.sla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.repository.SlaRepository
import com.wiom.csp.domain.model.SlaData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlaViewModel @Inject constructor(
    private val slaRepository: SlaRepository
) : ViewModel() {

    private val _sla = MutableStateFlow(SlaData())
    val sla: StateFlow<SlaData> = _sla.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var pollingJob: Job? = null

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L // 30 seconds
    }

    init {
        refresh()
        startPolling()
    }

    /**
     * Refresh SLA data from repository (API-first, cache fallback).
     */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _sla.value = slaRepository.getSla()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load SLA data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Start polling SLA data every 30 seconds.
     * Automatically cancelled when ViewModel is cleared.
     */
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                try {
                    _sla.value = slaRepository.getSla()
                    _error.value = null
                } catch (_: Exception) {
                    // Silent failure on poll — keep showing last known data
                }
            }
        }
    }

    /**
     * Clear current error state.
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
