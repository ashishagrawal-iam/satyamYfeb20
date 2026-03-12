package com.wiom.csp.ui.sla

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiom.csp.R
import com.wiom.csp.data.repository.SLARepository
import com.wiom.csp.domain.model.*
import com.wiom.csp.ui.theme.WiomCspTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// ---- ViewModel ----

@HiltViewModel
class SLAViewModel @Inject constructor(
    private val slaRepo: SLARepository
) : ViewModel() {
    private val _sla = MutableStateFlow<SLAOverallState?>(null)
    val sla: StateFlow<SLAOverallState?> = _sla

    init {
        load()
        startPolling()
    }

    fun load() {
        viewModelScope.launch {
            slaRepo.getSLA().onSuccess { _sla.value = it }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(3000)
                slaRepo.getSLA().onSuccess { _sla.value = it }
            }
        }
    }
}

// ---- Helpers ----

private fun standingColor(s: SlaStanding, colors: com.wiom.csp.ui.theme.WiomColors): Color {
    return when (s) {
        SlaStanding.COMPLIANT -> colors.positive
        SlaStanding.AT_RISK -> colors.warning
        SlaStanding.NON_COMPLIANT -> colors.negative
    }
}

private fun standingBg(s: SlaStanding, colors: com.wiom.csp.ui.theme.WiomColors): Color {
    return when (s) {
        SlaStanding.COMPLIANT -> colors.positive.copy(alpha = 0.12f)
        SlaStanding.AT_RISK -> colors.warning.copy(alpha = 0.12f)
        SlaStanding.NON_COMPLIANT -> colors.negative.copy(alpha = 0.12f)
    }
}

private fun standingLabelRes(s: SlaStanding): Int {
    return when (s) {
        SlaStanding.COMPLIANT -> R.string.sla_compliant
        SlaStanding.AT_RISK -> R.string.sla_at_risk
        SlaStanding.NON_COMPLIANT -> R.string.sla_non_compliant
    }
}

private fun domainIcon(id: SLADomainId): String {
    return when (id) {
        SLADomainId.INSTALLATION -> "\u2692"
        SLADomainId.RESOLUTION -> "\u2699"
        SLADomainId.STABILITY -> "\u2601"
        SLADomainId.EXPERIENCE -> "\u2605"
    }
}

private fun metricColor(m: SLASubMetric, colors: com.wiom.csp.ui.theme.WiomColors): Color {
    if (m.sampleCount < m.minSample) return colors.textMuted
    val breached = if (m.thresholdDirection == "above") m.value < m.threshold else m.value > m.threshold
    val severe = if (m.thresholdDirection == "above") m.value < m.severeThreshold else m.value > m.severeThreshold
    return if (severe) colors.negative else if (breached) colors.warning else colors.positive
}

private fun isBreach(m: SLASubMetric): Boolean {
    if (m.sampleCount < m.minSample) return false
    return if (m.thresholdDirection == "above") m.value < m.threshold else m.value > m.threshold
}

private fun trendIcon(t: SLATrend): String {
    return when (t) {
        SLATrend.IMPROVING -> "\u2197"
        SLATrend.DECLINING -> "\u2198"
        SLATrend.STABLE -> "\u2192"
    }
}

private fun trendColor(t: SLATrend, colors: com.wiom.csp.ui.theme.WiomColors): Color {
    return when (t) {
        SLATrend.IMPROVING -> colors.positive
        SLATrend.DECLINING -> colors.negative
        SLATrend.STABLE -> colors.textMuted
    }
}

private fun daysUntil(iso: String): Int {
    return try {
        val target = java.time.Instant.parse(iso).toEpochMilli()
        max(0, ceil((target - System.currentTimeMillis()) / 86400000.0).toInt())
    } catch (_: Exception) { 0 }
}

// ---- Screen ----

@Composable
fun SLAHubScreen(
    onBack: () -> Unit,
    viewModel: SLAViewModel = hiltViewModel()
) {
    val colors = WiomCspTheme.colors
    val sla by viewModel.sla.collectAsState()
    val data = sla ?: return

    var selectedDomain by remember { mutableStateOf<SLADomain?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
            .statusBarsPadding()
    ) {
        if (selectedDomain != null) {
            DomainDetail(
                domain = selectedDomain!!,
                onBack = { selectedDomain = null }
            )
        } else {
            HubView(
                sla = data,
                onBack = onBack,
                onDomainClick = { selectedDomain = it }
            )
        }
    }
}

@Composable
private fun HubView(
    sla: SLAOverallState,
    onBack: () -> Unit,
    onDomainClick: (SLADomain) -> Unit
) {
    val colors = WiomCspTheme.colors
    val sc = standingColor(sla.overallStanding, colors)
    val breached = sla.domains.filter { it.state != SlaStanding.COMPLIANT }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(colors.borderSubtle, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                }
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.general_back),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.sla_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Overall State Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.bgCard)
                    .border(1.dp, sc.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(standingBg(sla.overallStanding, colors))
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        stringResource(standingLabelRes(sla.overallStanding)),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = sc
                    )
                }

                if (sla.overallStanding != SlaStanding.COMPLIANT && breached.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        breached.joinToString(", ") { it.name } + " " + stringResource(R.string.sla_needs_attention),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${daysUntil(sla.nextEvaluation)}d",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(stringResource(R.string.sla_next_eval), fontSize = 12.sp, color = colors.textMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (sla.consequence.routing == "Full") stringResource(R.string.sla_routing_full) else stringResource(R.string.sla_routing_tapered),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sla.consequence.routing == "Full") colors.positive else colors.warning
                        )
                        Text(stringResource(R.string.sla_routing), fontSize = 12.sp, color = colors.textMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (sla.consequence.bonusEligibility == "Eligible") stringResource(R.string.sla_bonus_active) else stringResource(R.string.sla_bonus_paused),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sla.consequence.bonusEligibility == "Eligible") colors.positive else colors.warning
                        )
                        Text(stringResource(R.string.sla_bonus), fontSize = 12.sp, color = colors.textMuted)
                    }
                }
            }

            // Fix to recover
            if (breached.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.warning.copy(alpha = 0.06f))
                        .border(1.dp, colors.warning.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(12.dp, 16.dp)
                ) {
                    Text(
                        stringResource(R.string.sla_fix_to_recover),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.warning
                    )
                    Spacer(Modifier.height(8.dp))
                    breached.forEach { d ->
                        val dc = standingColor(d.state, colors)
                        val failingMetrics = d.subMetrics.filter { isBreach(it) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(standingBg(d.state, colors))
                                .clickable { onDomainClick(d) }
                                .padding(12.dp, 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "${domainIcon(d.id)} ${d.name}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = dc
                                    )
                                    if (failingMetrics.isNotEmpty()) {
                                        Text(
                                            failingMetrics.joinToString(", ") { it.name.split("(")[0].trim() },
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                Text("\u203A", fontSize = 14.sp, color = dc)
                            }
                        }
                    }
                    if (sla.hysteresis.requiredCleanWindows > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(
                                R.string.sla_clean_windows,
                                sla.hysteresis.requiredCleanWindows,
                                if (sla.hysteresis.requiredCleanWindows > 1) "s" else "",
                                sla.hysteresis.currentCleanWindows
                            ),
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            // 4 Domain Cards
            Spacer(Modifier.height(12.dp))
            sla.domains.forEach { domain ->
                val dc = standingColor(domain.state, colors)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.bgCard)
                        .drawBehind {
                            drawLine(dc, Offset(0f, 0f), Offset(0f, size.height), 4.dp.toPx())
                        }
                        .clickable { onDomainClick(domain) }
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${domainIcon(domain.id)} ${domain.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                domain.subMetrics.forEach { m ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(metricColor(m, colors))
                                        )
                                        Text(
                                            "${m.value}${if (m.unit == "%") "%" else ""}",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(standingBg(domain.state, colors))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    stringResource(standingLabelRes(domain.state)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = dc
                                )
                            }
                            Text("\u203A", fontSize = 14.sp, color = colors.textMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DomainDetail(
    domain: SLADomain,
    onBack: () -> Unit
) {
    val colors = WiomCspTheme.colors
    val dc = standingColor(domain.state, colors)

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(colors.borderSubtle, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                }
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.general_back),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${domainIcon(domain.id)} ${domain.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(standingBg(domain.state, colors))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(standingLabelRes(domain.state)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = dc
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            domain.subMetrics.forEach { m ->
                val mc = metricColor(m, colors)
                val breached = isBreach(m)
                val isAbove = m.thresholdDirection == "above"

                // Bar math
                val barPct: Float
                val thPct: Float
                val sevPct: Float
                if (isAbove) {
                    barPct = min(1f, (m.value / 100.0).toFloat())
                    thPct = (m.threshold / 100.0).toFloat()
                    sevPct = (m.severeThreshold / 100.0).toFloat()
                } else {
                    val maxVal = m.severeThreshold * 1.5
                    barPct = min(1f, max(0f, (1f - (m.value / maxVal)).toFloat()))
                    thPct = (1f - (m.threshold / maxVal)).toFloat()
                    sevPct = (1f - (m.severeThreshold / maxVal)).toFloat()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.bgCard)
                        .drawBehind {
                            drawLine(mc, Offset(0f, 0f), Offset(0f, size.height), 4.dp.toPx())
                        }
                        .padding(16.dp)
                ) {
                    // Name + value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            m.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${m.value}${if (m.unit == "%") "%" else ""}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = mc
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Visual bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.bgSecondary)
                    ) {
                        // Value bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barPct)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(mc)
                        )
                        // Threshold marker
                        Box(
                            modifier = Modifier
                                .offset(x = (thPct * 300).dp) // approximate
                                .width(2.dp)
                                .height(10.dp)
                                .offset(y = (-2).dp)
                                .background(colors.warning)
                        )
                        // Severe threshold marker
                        Box(
                            modifier = Modifier
                                .offset(x = (sevPct * 300).dp)
                                .width(2.dp)
                                .height(10.dp)
                                .offset(y = (-2).dp)
                                .background(colors.negative)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Threshold + Trend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val pctSuffix = if (m.unit == "%") "%" else ""
                        Text(
                            buildString {
                                append(stringResource(R.string.sla_threshold_min))
                                append(if (isAbove) "\u2265" else "\u2264")
                                append(m.threshold.toInt())
                                append(pctSuffix)
                                append(" \u00B7 ")
                                append(stringResource(R.string.sla_threshold_severe))
                                append(if (isAbove) "<" else ">")
                                append(m.severeThreshold.toInt())
                                append(pctSuffix)
                            },
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                        Text(
                            "${trendIcon(m.trend)} ${m.trend.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = trendColor(m.trend, colors)
                        )
                    }

                    // Action needed
                    if (breached) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.warning.copy(alpha = 0.06f))
                                .border(1.dp, colors.warning.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(8.dp, 12.dp)
                        ) {
                            val diff = if (isAbove)
                                "%.1f".format(m.threshold - m.value)
                            else
                                "%.1f".format(m.value - m.threshold)
                            val pctSuffix = if (m.unit == "%") "%" else ""
                            Text(
                                if (isAbove)
                                    stringResource(R.string.sla_needs_above, m.threshold.toInt(), pctSuffix, diff)
                                else
                                    stringResource(R.string.sla_needs_below, m.threshold.toInt(), pctSuffix, diff),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.warning
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
