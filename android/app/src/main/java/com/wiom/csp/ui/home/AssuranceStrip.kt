package com.wiom.csp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.wiom.csp.R
import com.wiom.csp.domain.model.AssuranceState
import com.wiom.csp.domain.model.ExposureState
import com.wiom.csp.domain.model.SlaStanding
import com.wiom.csp.ui.common.DrillDownSheet
import com.wiom.csp.ui.common.formatCurrency
import com.wiom.csp.ui.common.formatCurrencyCompact
import com.wiom.csp.ui.theme.WiomCspTheme
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun AssuranceStrip(
    assuranceState: AssuranceState,
    lifetimeEarnings: Int?,
    activeDrillDown: String?,
    onDrillDown: (String?) -> Unit,
    onOpenSLA: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = WiomCspTheme.colors

    val slaColor = when (assuranceState.slaStanding) {
        SlaStanding.COMPLIANT -> colors.positive
        SlaStanding.AT_RISK -> colors.warning
        SlaStanding.NON_COMPLIANT -> colors.negative
    }

    val exposureColor = when (assuranceState.exposureState) {
        ExposureState.ELIGIBLE -> colors.positive
        ExposureState.LIMITED -> colors.warning
        ExposureState.INELIGIBLE -> colors.negative
    }

    // Layout: 3 columns matching web gridTemplateColumns: '1fr 1fr auto'
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.stripBg)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Column 1: Active Base card
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgCard)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                .clickable { onDrillDown("activeBase") }
                .semantics { contentDescription = "Active base: ${assuranceState.activeBase} connections. Tap for details." }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.assurance_active_base),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${assuranceState.activeBase}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                lineHeight = 26.sp
            )
        }

        // Column 2: Cycle Earnings card
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgCard)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                .clickable { onDrillDown("earnings") }
                .semantics { contentDescription = "Cycle earnings: ${formatCurrencyCompact(assuranceState.cycleEarned)}. Tap for details." }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.assurance_cycle_earnings),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatCurrencyCompact(assuranceState.cycleEarned),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary, // white, NOT blue — matches web var(--text-primary)
                lineHeight = 24.sp
            )
        }

        // Column 3: SLA + Exposure stacked — fixed width so columns 1 & 2 keep their space
        Column(
            modifier = Modifier.fillMaxHeight().width(110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // SLA indicator
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.bgCard)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                    .clickable { if (onOpenSLA != null) onOpenSLA() else onDrillDown("sla") }
                    .semantics { contentDescription = "SLA standing: ${assuranceState.slaStanding.name}. Tap for details." }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .shadow(6.dp, CircleShape, ambientColor = slaColor, spotColor = slaColor)
                        .clip(CircleShape)
                        .background(slaColor)
                )
                Text(
                    text = stringResource(R.string.assurance_sla_standing),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    letterSpacing = 0.3.sp
                )
                Text("\u203A", fontSize = 14.sp, color = colors.textMuted)
            }

            // Exposure indicator (with direction arrow — design spec)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.bgCard)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                    .clickable { onDrillDown("exposure") }
                    .semantics { contentDescription = "Exposure: ${assuranceState.exposureState.name}, ${assuranceState.exposureDirection}. Tap for details." }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .shadow(6.dp, CircleShape, ambientColor = exposureColor, spotColor = exposureColor)
                        .clip(CircleShape)
                        .background(exposureColor)
                )
                Text(
                    text = stringResource(R.string.assurance_exposure),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    letterSpacing = 0.3.sp
                )
                // Direction indicator arrow
                val directionArrow = when (assuranceState.exposureDirection) {
                    "improving" -> "\u2191"  // ↑
                    "declining" -> "\u2193"  // ↓
                    else -> "\u2192"         // → stable
                }
                val directionColor = when (assuranceState.exposureDirection) {
                    "improving" -> colors.positive
                    "declining" -> colors.negative
                    else -> colors.textMuted
                }
                Text(directionArrow, fontSize = 14.sp, color = directionColor)
                Text("\u203A", fontSize = 14.sp, color = colors.textMuted)
            }
        }
    }

}

/** Drill-down sheets for assurance metrics — rendered at screen-level so overlay covers full screen */
@Composable
fun AssuranceDrillDowns(
    assuranceState: AssuranceState,
    lifetimeEarnings: Int?,
    activeDrillDown: String?,
    onDismiss: () -> Unit
) {
    val colors = WiomCspTheme.colors

    val slaColor = when (assuranceState.slaStanding) {
        SlaStanding.COMPLIANT -> colors.positive
        SlaStanding.AT_RISK -> colors.warning
        SlaStanding.NON_COMPLIANT -> colors.negative
    }

    val exposureColor = when (assuranceState.exposureState) {
        ExposureState.ELIGIBLE -> colors.positive
        ExposureState.LIMITED -> colors.warning
        ExposureState.INELIGIBLE -> colors.negative
    }

    // Active Base
    DrillDownSheet(
        visible = activeDrillDown == "activeBase",
        onDismiss = onDismiss
    ) {
        Text(stringResource(R.string.assurance_active_base), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.assurance_current_count), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text("${assuranceState.activeBase} ${stringResource(R.string.assurance_connections)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.assurance_recent_changes), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(8.dp))
        assuranceState.activeBaseEvents.take(5).forEach { evt ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(colors.borderSubtle, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(evt.connectionId, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(evt.reason, fontSize = 12.sp, color = colors.textMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (evt.change > 0) "+1" else "-1",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (evt.change > 0) colors.positive else colors.negative
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(evt.date, fontSize = 12.sp, color = colors.textMuted)
                }
            }
        }
    }

    // Earnings
    DrillDownSheet(
        visible = activeDrillDown == "earnings",
        onDismiss = onDismiss
    ) {
        Text(stringResource(R.string.assurance_cycle_earnings), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.assurance_cycle_earned), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(formatCurrency(assuranceState.cycleEarned), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.assurance_active_base), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text("${assuranceState.activeBase} ${stringResource(R.string.assurance_connections)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.assurance_next_settlement), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            "${formatCurrency(assuranceState.nextSettlementAmount)} on ${assuranceState.nextSettlementDate}",
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary
        )
        if (lifetimeEarnings != null) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.assurance_lifetime_earned), fontSize = 12.sp, color = colors.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(formatCurrency(lifetimeEarnings), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.money)
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colors.bgPrimary).padding(12.dp)
        ) {
            Text(
                "${stringResource(R.string.assurance_cycle_note)} ${stringResource(R.string.assurance_wallet)} ${stringResource(R.string.assurance_from_menu)}",
                fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp
            )
        }
    }

    // SLA Standing
    DrillDownSheet(
        visible = activeDrillDown == "sla",
        onDismiss = onDismiss
    ) {
        Text(stringResource(R.string.assurance_sla_standing), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.assurance_current_status), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            when (assuranceState.slaStanding) {
                SlaStanding.COMPLIANT -> stringResource(R.string.sla_compliant)
                SlaStanding.AT_RISK -> stringResource(R.string.sla_at_risk)
                SlaStanding.NON_COMPLIANT -> stringResource(R.string.sla_non_compliant)
            },
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = slaColor
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.assurance_active_restores), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text("${assuranceState.activeRestores}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.assurance_unresolved_count), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text("${assuranceState.unresolvedCount}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colors.bgPrimary).padding(12.dp)
        ) {
            Text(
                stringResource(R.string.assurance_sla_note),
                fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp
            )
        }
    }

    // Exposure
    DrillDownSheet(
        visible = activeDrillDown == "exposure",
        onDismiss = onDismiss
    ) {
        Text(stringResource(R.string.assurance_exposure), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.assurance_current_status), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            when (assuranceState.exposureState) {
                ExposureState.ELIGIBLE -> stringResource(R.string.exposure_eligible)
                ExposureState.LIMITED -> stringResource(R.string.exposure_limited)
                ExposureState.INELIGIBLE -> stringResource(R.string.exposure_ineligible)
            },
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = exposureColor
        )
        Spacer(Modifier.height(16.dp))
        Text("Direction", fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = assuranceState.exposureDirection.replaceFirstChar { it.uppercase() },
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = when (assuranceState.exposureDirection) {
                "improving" -> colors.positive
                "declining" -> colors.negative
                else -> colors.textPrimary
            }
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.assurance_reason_code), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(assuranceState.exposureReason, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.assurance_effective_since), fontSize = 12.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(assuranceState.exposureSince, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colors.bgPrimary).padding(12.dp)
        ) {
            Column {
                Text(stringResource(R.string.assurance_qual_signal), fontSize = 14.sp, color = colors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    when (assuranceState.exposureState) {
                        ExposureState.ELIGIBLE -> stringResource(R.string.assurance_exposure_ok)
                        ExposureState.LIMITED -> stringResource(R.string.assurance_exposure_limited)
                        ExposureState.INELIGIBLE -> stringResource(R.string.assurance_exposure_critical)
                    },
                    fontSize = 14.sp, color = colors.textPrimary, lineHeight = 20.sp
                )
            }
        }
    }
}
