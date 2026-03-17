package com.wiom.csp.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.repository.SupportRepository
import com.wiom.csp.domain.model.SupportCaseData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val supportRepository: SupportRepository
) : ViewModel() {

    private val _cases = MutableStateFlow<List<SupportCaseData>>(emptyList())
    val cases: StateFlow<List<SupportCaseData>> = _cases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _createdCaseId = MutableStateFlow<String?>(null)
    val createdCaseId: StateFlow<String?> = _createdCaseId.asStateFlow()

    init {
        loadCases()
    }

    fun loadCases() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _cases.value = supportRepository.getCases()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load cases"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCase(subject: String, description: String, linkedTaskId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = supportRepository.createCase(subject, description, linkedTaskId)
            result.onSuccess { caseData ->
                _createdCaseId.value = caseData.id
                loadCases()
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to create case"
                _isLoading.value = false
            }
        }
    }

    fun replyToCase(caseId: String, text: String) {
        viewModelScope.launch {
            _error.value = null
            val result = supportRepository.replyToCase(caseId, text)
            result.onSuccess {
                loadCases()
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to send reply"
            }
        }
    }

    fun clearCreatedCaseId() {
        _createdCaseId.value = null
    }
}
