package com.wiom.csp.ui.deposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.repository.DepositRepository
import com.wiom.csp.domain.model.DepositData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DepositViewModel @Inject constructor(
    private val depositRepository: DepositRepository
) : ViewModel() {

    private val _deposit = MutableStateFlow(DepositData())
    val deposit: StateFlow<DepositData> = _deposit.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadDeposit()
    }

    fun loadDeposit() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _deposit.value = depositRepository.getDeposit()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load deposit"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
