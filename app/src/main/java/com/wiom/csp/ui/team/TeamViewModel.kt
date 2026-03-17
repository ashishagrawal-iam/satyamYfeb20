package com.wiom.csp.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.repository.TeamRepository
import com.wiom.csp.domain.model.TechnicianData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _technicians = MutableStateFlow<List<TechnicianData>>(emptyList())
    val technicians: StateFlow<List<TechnicianData>> = _technicians.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTechnicians()
    }

    fun loadTechnicians() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _technicians.value = teamRepository.getTechnicians()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load technicians"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTechnician(name: String, phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = teamRepository.addTechnician(name, phone)
            result.onSuccess {
                // Reload full list to stay in sync
                loadTechnicians()
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to add technician"
                _isLoading.value = false
            }
        }
    }
}
