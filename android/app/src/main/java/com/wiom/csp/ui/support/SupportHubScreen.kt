package com.wiom.csp.ui.support

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiom.csp.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.data.remote.dto.SupportCaseCreateRequest
import com.wiom.csp.data.repository.SupportRepository
import com.wiom.csp.domain.model.SupportCase
import com.wiom.csp.domain.model.SupportCaseStatus
import com.wiom.csp.domain.model.SupportMessage
import com.wiom.csp.ui.common.formatTimeAgo
import com.wiom.csp.ui.theme.WiomCspTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val supportRepo: SupportRepository
) : ViewModel() {
    private val _cases = MutableStateFlow<List<SupportCase>>(emptyList())
    val cases: StateFlow<List<SupportCase>> = _cases

    init { load() }

    fun load() {
        viewModelScope.launch {
            supportRepo.getCases().onSuccess { _cases.value = it }
        }
    }

    fun createCase(subject: String, message: String) {
        val now = Instant.now().toString()
        viewModelScope.launch {
            supportRepo.createCase(SupportCaseCreateRequest(
                caseId = "SUP-${System.currentTimeMillis().toString().takeLast(4)}",
                subject = subject,
                createdAt = now,
                updatedAt = now,
                messages = listOf(SupportMessage("CSP-MH-1001", message, now))
            ))
            load()
        }
    }
}

@Composable
fun SupportHubScreen(
    onBack: () -> Unit,
    viewModel: SupportViewModel = hiltViewModel()
) {
    val colors = WiomCspTheme.colors
    val cases by viewModel.cases.collectAsState()
    var showCreateForm by remember { mutableStateOf(false) }
    var selectedCase by remember { mutableStateOf<SupportCase?>(null) }
    var showReceipt by remember { mutableStateOf(false) }

    if (showReceipt) {
        Box(modifier = Modifier.fillMaxSize().background(colors.bgPrimary), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text("\u2705", fontSize = 40.sp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.support_receipt_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.support_receipt_desc), fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp)
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(colors.brandPrimary).clickable { showReceipt = false; showCreateForm = false }.height(48.dp).padding(horizontal = 24.dp, vertical = 12.dp)
                ) { Text(stringResource(R.string.support_back_to_support), fontWeight = FontWeight.Bold, color = colors.bgPrimary) }
            }
        }
        return
    }

    if (showCreateForm) {
        CreateCaseForm(
            onSubmit = { subject, msg ->
                viewModel.createCase(subject, msg)
                showReceipt = true
            },
            onCancel = { showCreateForm = false }
        )
        return
    }

    if (selectedCase != null) {
        CaseDetailScreen(supportCase = selectedCase!!, onBack = { selectedCase = null })
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.bgPrimary).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.general_back),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp)
                    )
                    IconButton(onClick = { showCreateForm = true }) { Icon(Icons.Default.Add, stringResource(R.string.support_new_case), tint = colors.brandPrimary) }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.support_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }

            if (cases.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.support_no_cases), fontSize = 14.sp, color = colors.textMuted)
                }
            }

            cases.forEach { c ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedCase = c }.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(c.subject, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1)
                        Text("${c.caseId} \u2022 ${formatTimeAgo(c.updatedAt)}", fontSize = 12.sp, color = colors.textMuted)
                    }
                    val statusColor = when (c.status) {
                        SupportCaseStatus.OPEN -> colors.warning
                        SupportCaseStatus.IN_PROGRESS -> colors.brandPrimary
                        SupportCaseStatus.RESOLVED -> colors.positive
                        SupportCaseStatus.CLOSED -> colors.textMuted
                    }
                    Text(c.status.name.replace("_", " "), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }
        }
    }
}

@Composable
private fun CreateCaseForm(onSubmit: (String, String) -> Unit, onCancel: () -> Unit) {
    val colors = WiomCspTheme.colors
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(colors.bgPrimary)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                stringResource(R.string.general_back),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.clickable { onCancel() }.padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.support_new_case), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(stringResource(R.string.support_subject)) }, modifier = Modifier.fillMaxWidth().height(56.dp), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.brandPrimary, unfocusedBorderColor = colors.borderSubtle, focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary, cursorColor = colors.brandPrimary, focusedLabelColor = colors.brandPrimary, unfocusedLabelColor = colors.textMuted))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text(stringResource(R.string.support_message)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).height(120.dp), maxLines = 5, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.brandPrimary, unfocusedBorderColor = colors.borderSubtle, focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary, cursorColor = colors.brandPrimary, focusedLabelColor = colors.brandPrimary, unfocusedLabelColor = colors.textMuted))
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(if (subject.isNotBlank() && message.isNotBlank()) colors.brandPrimary else colors.ctaDisabledBg)
                    .clickable(enabled = subject.isNotBlank() && message.isNotBlank()) { onSubmit(subject.trim(), message.trim()) }
                    .height(48.dp)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.support_submit), fontWeight = FontWeight.Bold, color = if (subject.isNotBlank() && message.isNotBlank()) colors.bgPrimary else colors.textMuted) }
        }
    }
}

@Composable
private fun CaseDetailScreen(supportCase: SupportCase, onBack: () -> Unit) {
    val colors = WiomCspTheme.colors
    Box(modifier = Modifier.fillMaxSize().background(colors.bgPrimary)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    stringResource(R.string.general_back),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(supportCase.caseId, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(colors.bgCard).padding(16.dp)) {
                Text(supportCase.subject, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.support_status, supportCase.status.name), fontSize = 14.sp, color = colors.textSecondary)
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.support_messages), modifier = Modifier.padding(horizontal = 16.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            supportCase.messages.forEach { msg ->
                val isCSP = msg.sender.startsWith("CSP")
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCSP) colors.brandSubtle else colors.bgCard)
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(msg.sender, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isCSP) colors.brandPrimary else colors.textSecondary)
                        Text(formatTimeAgo(msg.timestamp), fontSize = 12.sp, color = colors.textMuted)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(msg.text, fontSize = 14.sp, color = colors.textPrimary, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(84.dp))
        }
    }
}
