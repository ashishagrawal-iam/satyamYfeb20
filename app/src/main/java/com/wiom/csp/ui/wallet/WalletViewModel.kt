package com.wiom.csp.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.repository.WalletRepository
import com.wiom.csp.domain.model.WalletData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _wallet = MutableStateFlow(WalletData())
    val wallet: StateFlow<WalletData> = _wallet.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    /**
     * Refresh wallet data from repository (API-first, cache fallback).
     */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _wallet.value = walletRepository.getWallet()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load wallet"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Withdraw from wallet.
     * Validates amount > 0 && <= balance via repository.
     * On success, updates local wallet state.
     */
    fun withdraw(amount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = walletRepository.withdraw(amount)
                result.fold(
                    onSuccess = { updatedWallet ->
                        _wallet.value = updatedWallet
                    },
                    onFailure = { throwable ->
                        _error.value = throwable.message ?: "Withdrawal failed"
                        // Refresh to get latest state even on failure
                        try {
                            _wallet.value = walletRepository.getWallet()
                        } catch (_: Exception) { /* keep current state */ }
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Withdrawal failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add money to wallet via specified payment method.
     * On success, updates local wallet state.
     */
    fun addMoney(amount: Double, method: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = walletRepository.addMoney(amount, method)
                result.fold(
                    onSuccess = { updatedWallet ->
                        _wallet.value = updatedWallet
                    },
                    onFailure = { throwable ->
                        _error.value = throwable.message ?: "Add money failed"
                        // Refresh to get latest state even on failure
                        try {
                            _wallet.value = walletRepository.getWallet()
                        } catch (_: Exception) { /* keep current state */ }
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Add money failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear current error state.
     */
    fun clearError() {
        _error.value = null
    }
}
