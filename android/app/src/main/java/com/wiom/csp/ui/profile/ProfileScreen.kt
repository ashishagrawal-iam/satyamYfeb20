package com.wiom.csp.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.R
import com.wiom.csp.ui.common.WiomToggle
import com.wiom.csp.ui.theme.WiomCspTheme

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    offersEnabled: Boolean,
    onOffersToggle: (Boolean) -> Unit,
    onLogout: () -> Unit = {}
) {
    val colors = WiomCspTheme.colors
    var taskAlerts by remember { mutableStateOf(true) }
    var slaWarnings by remember { mutableStateOf(true) }
    var settlementUpdates by remember { mutableStateOf(true) }
    var showOfferWarning by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.bgPrimary).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Header
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.general_back),
                    modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.profile_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }

            // Avatar + CSP info
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(colors.brandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Text("CSP-MH-1001", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(stringResource(R.string.profile_band_partner), fontSize = 14.sp, color = colors.brandPrimary)
            }

            Spacer(Modifier.height(24.dp))

            // Language section
            SectionTitle(stringResource(R.string.profile_language))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("EN" to stringResource(R.string.profile_english), "HI" to stringResource(R.string.profile_hindi)).forEach { (code, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (code == "EN") colors.brandSubtle else colors.bgCard)
                            .border(
                                1.dp,
                                if (code == "EN") colors.brandPrimary else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { /* TODO: language switch */ }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Notification Settings
            SectionTitle(stringResource(R.string.profile_notifications))
            ToggleRow(stringResource(R.string.profile_task_alerts), taskAlerts) { taskAlerts = it }
            ToggleRow(stringResource(R.string.profile_sla_warnings), slaWarnings) { slaWarnings = it }
            ToggleRow(stringResource(R.string.profile_settlement_updates), settlementUpdates) { settlementUpdates = it }

            Spacer(Modifier.height(16.dp))

            // Offer Notifications (with confirmation)
            SectionTitle(stringResource(R.string.profile_offer_notifications))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.profile_offer_notifications), fontSize = 14.sp, color = colors.textPrimary)
                WiomToggle(
                    checked = offersEnabled,
                    onCheckedChange = { newValue ->
                        if (!newValue) showOfferWarning = true
                        else onOffersToggle(true)
                    }
                )
            }
            if (!offersEnabled) {
                Text(
                    stringResource(R.string.profile_offer_toggle_consequence),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 12.sp,
                    color = colors.warning,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Account Info
            SectionTitle(stringResource(R.string.profile_account_info))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.bgCard)
                    .padding(16.dp)
            ) {
                InfoRow(stringResource(R.string.profile_csp_id), "CSP-MH-1001")
                InfoRow(stringResource(R.string.profile_zone), "Mumbai West")
                InfoRow(stringResource(R.string.profile_partner_since), "2025-01-15")
                InfoRow(stringResource(R.string.profile_email), "csp.mh1001@wiom.in")
                InfoRow(stringResource(R.string.profile_phone), "+91 98765 00001")
            }

            // Logout
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.negative.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    stringResource(R.string.profile_logout),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.negative
                )
            }

            Spacer(Modifier.height(80.dp))
        }

        // Offer warning dialog
        if (showOfferWarning) {
            AlertDialog(
                onDismissRequest = { showOfferWarning = false },
                title = { Text(stringResource(R.string.profile_offer_toggle_title), color = colors.textPrimary) },
                text = {
                    Text(
                        stringResource(R.string.profile_offer_toggle_consequence),
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onOffersToggle(false)
                        showOfferWarning = false
                    }) { Text(stringResource(R.string.profile_turn_off), color = colors.negative) }
                },
                dismissButton = {
                    TextButton(onClick = { showOfferWarning = false }) {
                        Text(stringResource(R.string.profile_cancel), color = colors.textSecondary)
                    }
                },
                containerColor = colors.bgCard
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    val colors = WiomCspTheme.colors
    Text(
        title.uppercase(),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textSecondary,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = WiomCspTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = colors.textPrimary)
        WiomToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = WiomCspTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = colors.textMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}
