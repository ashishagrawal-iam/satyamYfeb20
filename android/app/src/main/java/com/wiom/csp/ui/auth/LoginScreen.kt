package com.wiom.csp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.R
import com.wiom.csp.ui.theme.WiomCspTheme
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val colors = WiomCspTheme.colors

    var mobile by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var resendTimer by remember { mutableIntStateOf(0) }

    // Navigate on success
    LaunchedEffect(state) {
        if (state is LoginState.Success) {
            onLoginSuccess()
        }
    }

    // Resend timer countdown
    LaunchedEffect(resendTimer) {
        if (resendTimer > 0) {
            delay(1000)
            resendTimer--
        }
    }

    // Start timer when OTP is sent
    LaunchedEffect(state) {
        if (state is LoginState.OtpSent) {
            resendTimer = 30
        }
    }

    val isOtpStep = state is LoginState.OtpSent || state is LoginState.Verifying ||
            (state is LoginState.Error && (state as LoginState.Error).step == "otp")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scrollable content area — takes available space above the sticky CTA
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 380.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo — centered
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.brandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wiom_logo),
                        contentDescription = "Wiom",
                        modifier = Modifier.size(32.dp),
                        tint = colors.bgPrimary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Title — left-aligned for F-pattern
            Text(stringResource(R.string.login_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)

            Spacer(Modifier.height(8.dp))

            // Subtitle — left-aligned for F-pattern
            Text(
                text = if (isOtpStep) stringResource(R.string.login_otp_sent, mobile) else stringResource(R.string.login_subtitle),
                fontSize = 14.sp,
                color = colors.textSecondary
            )

            Spacer(Modifier.height(40.dp))

            // Error message — Wiom: warning amber, not alarming red
            if (state is LoginState.Error) {
                val errorMsg = (state as LoginState.Error).message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.warning.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = errorMsg,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            if (!isOtpStep) {
                // Step 1: Mobile Number
                Text(
                    stringResource(R.string.login_mobile_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bgCard),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(colors.bgSecondary)
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text(stringResource(R.string.login_country_code), fontSize = 16.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { v ->
                            val filtered = v.filter { it.isDigit() }.take(10)
                            mobile = filtered
                            if (state is LoginState.Error) viewModel.clearError()
                        },
                        placeholder = { Text(stringResource(R.string.login_mobile_placeholder), color = colors.textMuted) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (mobile.length == 10) viewModel.sendOtp(mobile) }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.brandPrimary
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    )
                }
            } else {
                // Step 2: OTP Verification
                Text(
                    stringResource(R.string.login_otp_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = otp,
                    onValueChange = { v ->
                        val filtered = v.filter { it.isDigit() }.take(4)
                        otp = filtered
                        if (state is LoginState.Error) viewModel.clearError()
                    },
                    placeholder = { Text("----", color = colors.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (otp.length == 4) viewModel.verifyOtp(otp) }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brandPrimary,
                        unfocusedBorderColor = colors.bgCard,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.brandPrimary,
                        focusedContainerColor = colors.bgCard,
                        unfocusedContainerColor = colors.bgCard
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 12.sp
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        otp = ""
                        viewModel.goBackToMobile()
                    }) {
                        Text(
                            stringResource(R.string.login_change_number),
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = { viewModel.resendOtp() },
                        enabled = resendTimer == 0 && state !is LoginState.SendingOtp
                    ) {
                        Text(
                            text = if (resendTimer > 0) stringResource(R.string.login_resend_timer, resendTimer) else stringResource(R.string.login_resend_otp),
                            fontSize = 14.sp,
                            color = if (resendTimer > 0) colors.textMuted else colors.brandPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Help link — Wiom: "हमसे बात करें" always accessible
            Text(
                stringResource(R.string.login_help),
                fontSize = 14.sp,
                color = colors.brandPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(16.dp))

            // Trust footer — company name as trust signal
            Text(
                stringResource(R.string.login_footer),
                fontSize = 12.sp,
                color = colors.textMuted
            )

            Spacer(Modifier.height(16.dp))
        }

        // Sticky CTA button — pinned at bottom, outside scroll
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isOtpStep) {
                Button(
                    onClick = { viewModel.sendOtp(mobile) },
                    enabled = mobile.length == 10 && state !is LoginState.SendingOtp,
                    modifier = Modifier
                        .widthIn(max = 328.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brandPrimary,
                        disabledContainerColor = colors.ctaDisabledBg
                    )
                ) {
                    if (state is LoginState.SendingOtp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.bgPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.login_send_otp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.verifyOtp(otp) },
                    enabled = otp.length == 4 && state !is LoginState.Verifying,
                    modifier = Modifier
                        .widthIn(max = 328.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brandPrimary,
                        disabledContainerColor = colors.ctaDisabledBg
                    )
                ) {
                    if (state is LoginState.Verifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.bgPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.login_verify_otp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
