package com.wiom.csp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

/** Sealed hierarchy for login flow states. */
sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object SendingOtp : LoginUiState()
    data class OtpSent(val tmpToken: String) : LoginUiState()
    data object Verifying : LoginUiState()
    data class Success(val isProfileComplete: Boolean) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _resendSeconds = MutableStateFlow(0)
    val resendSeconds: StateFlow<Int> = _resendSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var currentTmpToken: String? = null

    /**
     * Send OTP to the given mobile number.
     * Transitions: Idle -> SendingOtp -> OtpSent(tmpToken) or error -> Idle.
     */
    fun sendOtp(mobile: String) {
        if (mobile.length != 10) {
            _error.value = "Please enter a valid 10-digit mobile number"
            return
        }

        viewModelScope.launch {
            Log.d("LoginVM", "sendOtp called for mobile=$mobile")
            _uiState.value = LoginUiState.SendingOtp
            _error.value = null

            val result = authRepo.sendOtp(mobile)
            Log.d("LoginVM", "sendOtp result: isSuccess=${result.isSuccess}, isFailure=${result.isFailure}")
            result.fold(
                onSuccess = { tmpToken ->
                    Log.d("LoginVM", "OTP sent successfully, tmpToken=$tmpToken")
                    currentTmpToken = tmpToken
                    _uiState.value = LoginUiState.OtpSent(tmpToken)
                    startResendTimer()
                },
                onFailure = { throwable ->
                    Log.e("LoginVM", "sendOtp failed", throwable)
                    _error.value = throwable.message ?: "Failed to send OTP. Please try again."
                    _uiState.value = LoginUiState.Idle
                }
            )
        }
    }

    /**
     * Verify OTP against the tmp token from sendOtp.
     * Transitions: OtpSent -> Verifying -> Success or error -> OtpSent.
     */
    fun verifyOtp(mobile: String, otp: String) {
        val tmpToken = currentTmpToken
        if (tmpToken == null) {
            _error.value = "Session expired. Please request a new OTP."
            _uiState.value = LoginUiState.Idle
            return
        }
        if (otp.length != 4) {
            _error.value = "Please enter a valid 4-digit OTP"
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Verifying
            _error.value = null

            val result = authRepo.verifyOtp(mobile, otp, tmpToken)
            result.fold(
                onSuccess = { authResult ->
                    timerJob?.cancel()
                    _uiState.value = LoginUiState.Success(authResult.isProfileComplete)
                },
                onFailure = { throwable ->
                    _error.value = throwable.message ?: "OTP verification failed. Please try again."
                    _uiState.value = LoginUiState.OtpSent(tmpToken)
                }
            )
        }
    }

    /**
     * Resend OTP by calling sendOtp again. Resets the resend timer.
     */
    fun resendOtp(mobile: String) {
        sendOtp(mobile)
    }

    /** Clear current error message. */
    fun clearError() {
        _error.value = null
    }

    /** Reset back to Idle state (for "Change Number" action). */
    fun resetToIdle() {
        timerJob?.cancel()
        currentTmpToken = null
        _resendSeconds.value = 0
        _uiState.value = LoginUiState.Idle
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        _resendSeconds.value = 30
        timerJob = viewModelScope.launch {
            var seconds = 30
            while (seconds > 0) {
                delay(1000L)
                seconds--
                _resendSeconds.value = seconds
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
