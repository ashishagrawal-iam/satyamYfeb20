package com.wiom.csp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Two-step OTP login screen.
 * Step 1: Mobile number entry with +91 prefix.
 * Step 2: OTP verification with resend timer.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (isProfileComplete: Boolean) -> Unit
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()
    val resendSeconds by viewModel.resendSeconds.collectAsState()

    var mobile by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }

    // Navigate on success (must be in LaunchedEffect, not during composition)
    val currentState = uiState
    LaunchedEffect(currentState) {
        if (currentState is LoginUiState.Success) {
            onLoginSuccess(currentState.isProfileComplete)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Wiom CSP",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Partner Login",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Error banner
        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (currentState) {
            is LoginUiState.Idle,
            is LoginUiState.SendingOtp -> {
                // Step 1: Mobile input
                MobileInputStep(
                    mobile = mobile,
                    onMobileChange = { value ->
                        if (value.length <= 10 && value.all { it.isDigit() }) {
                            mobile = value
                            viewModel.clearError()
                        }
                    },
                    loading = currentState is LoginUiState.SendingOtp,
                    onSendOtp = { viewModel.sendOtp(mobile) }
                )
            }

            is LoginUiState.OtpSent,
            is LoginUiState.Verifying -> {
                // Step 2: OTP input
                OtpInputStep(
                    mobile = mobile,
                    otp = otp,
                    onOtpChange = { value ->
                        if (value.length <= 4 && value.all { it.isDigit() }) {
                            otp = value
                            viewModel.clearError()
                        }
                    },
                    loading = currentState is LoginUiState.Verifying,
                    resendSeconds = resendSeconds,
                    onVerify = { viewModel.verifyOtp(mobile, otp) },
                    onResend = {
                        otp = ""
                        viewModel.resendOtp(mobile)
                    },
                    onChangeNumber = {
                        otp = ""
                        viewModel.clearError()
                        viewModel.resetToIdle()
                    }
                )
            }

            is LoginUiState.Success -> {
                // Already handled above via navigation callback
            }
        }
    }
}

@Composable
private fun MobileInputStep(
    mobile: String,
    onMobileChange: (String) -> Unit,
    loading: Boolean,
    onSendOtp: () -> Unit
) {
    OutlinedTextField(
        value = mobile,
        onValueChange = onMobileChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Mobile Number") },
        prefix = {
            Text(
                text = "+91 ",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        placeholder = { Text("10-digit number") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { if (mobile.length == 10) onSendOtp() }
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onSendOtp,
        enabled = mobile.length == 10 && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sending...")
        } else {
            Text(
                text = "Send OTP",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun OtpInputStep(
    mobile: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    loading: Boolean,
    resendSeconds: Int,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit
) {
    Text(
        text = "OTP sent to +91 $mobile",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = otp,
        onValueChange = onOtpChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Enter OTP") },
        placeholder = { Text("4-digit OTP") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { if (otp.length == 4) onVerify() }
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onVerify,
        enabled = otp.length == 4 && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Verifying...")
        } else {
            Text(
                text = "Verify",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Resend timer + change number
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onChangeNumber) {
            Text(
                text = "Change Number",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (resendSeconds > 0) {
            Text(
                text = "Resend in ${resendSeconds}s",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TextButton(onClick = onResend, enabled = !loading) {
                Text(
                    text = "Resend OTP",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (loading) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
