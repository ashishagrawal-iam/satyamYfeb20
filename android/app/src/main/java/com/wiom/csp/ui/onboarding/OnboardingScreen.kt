package com.wiom.csp.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.R
import com.wiom.csp.ui.theme.WiomCspTheme

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onRegistrationComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val form by viewModel.form.collectAsState()
    val colors = WiomCspTheme.colors
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(state) {
        if (state is OnboardingState.Success) {
            onRegistrationComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .widthIn(max = 420.dp),
        ) {
            // Header
            Text(
                stringResource(R.string.onboarding_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_subtitle),
                fontSize = 14.sp,
                color = colors.textMuted,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(28.dp))

            // Error banner
            if (state is OnboardingState.Error) {
                val errorMsg = (state as OnboardingState.Error).message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.negative.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = errorMsg,
                        fontSize = 14.sp,
                        color = colors.negative,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // Section 1: Business Information
            SectionHeader(stringResource(R.string.onboarding_section_business), colors)
            FormField(
                label = stringResource(R.string.onboarding_business_name),
                value = form.businessName,
                onValueChange = { viewModel.updateForm { copy(businessName = it) }; viewModel.clearError() },
                placeholder = stringResource(R.string.onboarding_business_hint),
                colors = colors,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(12.dp))
            FieldLabel(stringResource(R.string.onboarding_entity_type), colors)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val entityTypes = listOf(
                    "INDIVIDUAL" to stringResource(R.string.onboarding_individual),
                    "FIRM" to stringResource(R.string.onboarding_firm),
                    "COMPANY" to stringResource(R.string.onboarding_company)
                )
                entityTypes.forEach { (type, label) ->
                    val selected = form.entityType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.brandPrimary else colors.bgCard)
                            .border(
                                1.dp,
                                if (selected) colors.brandPrimary else colors.borderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.updateForm { copy(entityType = type) }
                                viewModel.clearError()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else colors.textSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // Section 2: Service Location
            SectionHeader(stringResource(R.string.onboarding_section_location), colors)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    FormField(
                        label = stringResource(R.string.onboarding_state),
                        value = form.state,
                        onValueChange = { viewModel.updateForm { copy(state = it) }; viewModel.clearError() },
                        placeholder = stringResource(R.string.onboarding_state_hint),
                        colors = colors,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                }
                Box(Modifier.weight(1f)) {
                    FormField(
                        label = stringResource(R.string.onboarding_city),
                        value = form.city,
                        onValueChange = { viewModel.updateForm { copy(city = it) }; viewModel.clearError() },
                        placeholder = stringResource(R.string.onboarding_city_hint),
                        colors = colors,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    FormField(
                        label = stringResource(R.string.onboarding_area),
                        value = form.area,
                        onValueChange = { viewModel.updateForm { copy(area = it) }; viewModel.clearError() },
                        placeholder = stringResource(R.string.onboarding_area_hint),
                        colors = colors,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                }
                Box(Modifier.weight(1f)) {
                    FormField(
                        label = stringResource(R.string.onboarding_pincode),
                        value = form.pincode,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() }.take(6)
                            viewModel.updateForm { copy(pincode = filtered) }
                            viewModel.clearError()
                        },
                        placeholder = stringResource(R.string.onboarding_pincode_hint),
                        colors = colors,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // Section 3: Identity Verification
            SectionHeader(stringResource(R.string.onboarding_section_identity), colors)
            FormField(
                label = stringResource(R.string.onboarding_aadhaar),
                value = form.aadhaarNumber,
                onValueChange = {
                    val filtered = it.filter { c -> c.isDigit() }.take(12)
                    viewModel.updateForm { copy(aadhaarNumber = filtered) }
                    viewModel.clearError()
                },
                placeholder = stringResource(R.string.onboarding_aadhaar_hint),
                colors = colors,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(4.dp))
            FormField(
                label = stringResource(R.string.onboarding_pan),
                value = form.panNumber,
                onValueChange = {
                    val filtered = it.filter { c -> c.isLetterOrDigit() }.take(10)
                    viewModel.updateForm { copy(panNumber = filtered) }
                    viewModel.clearError()
                },
                placeholder = stringResource(R.string.onboarding_pan_hint),
                colors = colors,
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(24.dp))

            // Section 4: Bank Details
            SectionHeader(stringResource(R.string.onboarding_section_bank), colors)
            FormField(
                label = stringResource(R.string.onboarding_holder_name),
                value = form.bankAccountName,
                onValueChange = { viewModel.updateForm { copy(bankAccountName = it) }; viewModel.clearError() },
                placeholder = stringResource(R.string.onboarding_holder_hint),
                colors = colors,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )
            Spacer(Modifier.height(4.dp))
            FormField(
                label = stringResource(R.string.onboarding_account_number),
                value = form.bankAccountNumber,
                onValueChange = {
                    val filtered = it.filter { c -> c.isDigit() }.take(18)
                    viewModel.updateForm { copy(bankAccountNumber = filtered) }
                    viewModel.clearError()
                },
                placeholder = stringResource(R.string.onboarding_account_hint),
                colors = colors,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Next) }
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    FormField(
                        label = stringResource(R.string.onboarding_ifsc),
                        value = form.bankIfsc,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isLetterOrDigit() }.take(11)
                            viewModel.updateForm { copy(bankIfsc = filtered) }
                            viewModel.clearError()
                        },
                        placeholder = stringResource(R.string.onboarding_ifsc_hint),
                        colors = colors,
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                }
                Box(Modifier.weight(1f)) {
                    FormField(
                        label = stringResource(R.string.onboarding_bank_name),
                        value = form.bankName,
                        onValueChange = { viewModel.updateForm { copy(bankName = it) }; viewModel.clearError() },
                        placeholder = stringResource(R.string.onboarding_bank_hint),
                        colors = colors,
                        imeAction = ImeAction.Done,
                        onImeAction = { focusManager.clearFocus() }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // Section 5: Agreement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.bgCard)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.updateForm { copy(termsAccepted = !termsAccepted) }
                        viewModel.clearError()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = form.termsAccepted,
                    onCheckedChange = {
                        viewModel.updateForm { copy(termsAccepted = it) }
                        viewModel.clearError()
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.brandPrimary,
                        uncheckedColor = colors.borderSubtle,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    stringResource(R.string.onboarding_terms),
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // Submit button
            Button(
                onClick = { viewModel.submitRegistration() },
                enabled = state !is OnboardingState.Submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandPrimary,
                    disabledContainerColor = colors.brandPrimary.copy(alpha = 0.5f)
                )
            ) {
                if (state is OnboardingState.Submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        stringResource(R.string.onboarding_submit),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.login_footer),
                fontSize = 12.sp,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, colors: com.wiom.csp.ui.theme.WiomColors) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FieldLabel(label: String, colors: com.wiom.csp.ui.theme.WiomColors) {
    Text(
        label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textSecondary,
        letterSpacing = 0.3.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: com.wiom.csp.ui.theme.WiomColors,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(label, colors)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, color = colors.textMuted, fontSize = 14.sp)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalization,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.brandPrimary,
                unfocusedBorderColor = colors.borderSubtle,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.brandPrimary,
                focusedContainerColor = colors.bgCard,
                unfocusedContainerColor = colors.bgCard
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
    }
}
