package com.wiom.csp.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dev Dashboard — floating overlay for triggering flows & phases.
 * Only visible in dev builds (USE_MOCK = true).
 *
 * Sections:
 * - ISP Recharge: Phase 0, 1, 2, 3
 * - Installation: trigger installation flow
 * - (future sections)
 */
@Composable
fun DevDashboard(
    currentRechargePhase: Int = 1,
    onDismiss: () -> Unit,
    onSetRechargePhase: (Int) -> Unit,
    onTriggerInstallation: () -> Unit
) {
    // Semi-transparent backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        // Bottom sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFF1A1A2E))
                .clickable(enabled = false) {} // prevent click-through
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DeveloperMode,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dev Dashboard",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "DEV",
                    color = Color(0xFFFF6B6B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFF6B6B).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── ISP Recharge Section ──
            SectionHeader(
                title = "ISP Recharge  (Active: Phase $currentRechargePhase)",
                color = Color(0xFF00B894)
            )
            Text(
                text = "Switch phase to change the recharge card on home feed",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 11.dp, top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowTriggerCard(
                title = "Phase 0 — Manual Acknowledge",
                description = "Card shows 'Mark as Recharged'. CSP acknowledges recharge done on external portal.",
                icon = Icons.Default.TouchApp,
                accentColor = Color(0xFF636E72),
                phaseLabel = "P0",
                isActive = currentRechargePhase == 0,
                onClick = { onSetRechargePhase(0) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowTriggerCard(
                title = "Phase 1 — Recharge with Wiom",
                description = "Card shows 'Start Recharge'. Opens portal setup → one-by-one recharge with WebView.",
                icon = Icons.Default.Phonelink,
                accentColor = Color(0xFF00B894),
                phaseLabel = "P1",
                isActive = currentRechargePhase == 1,
                onClick = { onSetRechargePhase(1) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowTriggerCard(
                title = "Phase 2 — Batch Recharge",
                description = "Card shows 'Batch Recharge'. All customers pre-selected, one-click to recharge all.",
                icon = Icons.Default.PlaylistAddCheck,
                accentColor = Color(0xFF6C5CE7),
                phaseLabel = "P2",
                isActive = currentRechargePhase == 2,
                onClick = { onSetRechargePhase(2) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowTriggerCard(
                title = "Phase 3 — Fully Automatic",
                description = "Card shows auto-recharge status. No manual action — recharges happen automatically.",
                icon = Icons.Default.AutoMode,
                accentColor = Color(0xFF0984E3),
                phaseLabel = "P3",
                isActive = currentRechargePhase == 3,
                onClick = { onSetRechargePhase(3) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Installation Section ──
            SectionHeader(
                title = "Installation Flow",
                color = Color(0xFF6C5CE7)
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowTriggerCard(
                title = "13-Step Installation",
                description = "Selfie → Aadhaar → Payment → Router → Protocol → VLAN → WiFi → Photos → Speed Test → Happy Code",
                icon = Icons.Default.Construction,
                accentColor = Color(0xFF6C5CE7),
                phaseLabel = "V1",
                onClick = {
                    onTriggerInstallation()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun FlowTriggerCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    phaseLabel: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) accentColor else accentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(accentColor.copy(alpha = if (isActive) 0.15f else 0.06f))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Phase badge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = phaseLabel,
                color = if (isActive) Color.White else accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) accentColor else accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            if (isActive) {
                Text(
                    text = "ACTIVE",
                    color = accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
