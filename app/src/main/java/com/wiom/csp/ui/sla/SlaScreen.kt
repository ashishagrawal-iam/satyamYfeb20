package com.wiom.csp.ui.sla

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wiom.csp.domain.model.SlaDomainData
import com.wiom.csp.domain.model.SlaData
import com.wiom.csp.domain.model.SlaMetricData
import com.wiom.csp.domain.model.SlaMetricSchema
import com.wiom.csp.domain.model.SlaDomainSchema
import com.wiom.csp.domain.schema.SchemaResolver

/**
 * SlaScreen: displays overall SLA standing, domain-level detail cards with
 * sub-metric bars, breach indicators, trend arrows, and hysteresis recovery info.
 *
 * All domain names, metric names, and direction rules come from schema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlaScreen(
    sla: SlaData,
    schema: SchemaResolver,
    hindi: Boolean,
    onBack: () -> Unit
) {
    val domainSchemas = remember(schema) { schema.resolveSlaDomainsSchema() }
    val breachedDomains = remember(sla, domainSchemas) {
        sla.domains.filter { domainData ->
            domainData.standing != "COMPLIANT" && domainData.metrics.any { metric ->
                isBreach(metric, schema, domainData.id)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = if (hindi) "SLA \u0938\u094D\u0925\u093F\u0924\u093F" else "SLA Standing",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. Overall state card
            OverallStandingCard(
                standing = sla.overallStanding,
                hindi = hindi
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Stats row
            StatsRow(
                nextEvalDays = sla.nextEvalDays,
                routing = sla.routing,
                bonusStatus = sla.bonusStatus,
                hindi = hindi
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. "Fix to recover" section (only if any domain is breached)
            if (breachedDomains.isNotEmpty()) {
                FixToRecoverSection(
                    breachedDomains = breachedDomains,
                    domainSchemas = domainSchemas,
                    schema = schema,
                    hindi = hindi
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Domain cards
            domainSchemas.forEach { domainSchema ->
                val domainData = sla.domains.find { it.id == domainSchema.id }
                if (domainData != null) {
                    DomainCard(
                        domainData = domainData,
                        domainSchema = domainSchema,
                        schema = schema,
                        hindi = hindi
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Overall Standing Card ───────────────────────────────────────────────

@Composable
private fun OverallStandingCard(
    standing: String,
    hindi: Boolean
) {
    val (badgeColor, badgeBg, standingLabel) = resolveStandingDisplay(standing, hindi)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = badgeBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = standingLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = badgeColor
            )
        }
    }
}

// ── Stats Row ───────────────────────────────────────────────────────────

@Composable
private fun StatsRow(
    nextEvalDays: Int,
    routing: String,
    bonusStatus: String,
    hindi: Boolean
) {
    val routingLabel = if (routing == "FULL") {
        if (hindi) "\u092A\u0942\u0930\u094D\u0923" else "Full"
    } else {
        if (hindi) "\u0915\u092E" else "Tapered"
    }

    val bonusLabel = if (bonusStatus == "ACTIVE") {
        if (hindi) "\u0938\u0915\u094D\u0930\u093F\u092F" else "Active"
    } else {
        if (hindi) "\u0930\u0941\u0915\u093E" else "Paused"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatChip(
            label = if (hindi) "\u0905\u0917\u0932\u093E \u092E\u0942\u0932\u094D\u092F\u093E\u0902\u0915\u0928" else "Next eval",
            value = "$nextEvalDays ${if (hindi) "\u0926\u093F\u0928" else "days"}"
        )
        StatChip(
            label = if (hindi) "\u0930\u093E\u0909\u091F\u093F\u0902\u0917" else "Routing",
            value = routingLabel
        )
        StatChip(
            label = if (hindi) "\u092C\u094B\u0928\u0938" else "Bonus",
            value = bonusLabel
        )
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Fix to Recover Section ──────────────────────────────────────────────

@Composable
private fun FixToRecoverSection(
    breachedDomains: List<SlaDomainData>,
    domainSchemas: List<SlaDomainSchema>,
    schema: SchemaResolver,
    hindi: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF3B30).copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (hindi) "\u0930\u093F\u0915\u0935\u0930\u0940 \u0915\u0947 \u0932\u093F\u090F \u0920\u0940\u0915 \u0915\u0930\u0947\u0902" else "Fix to recover",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFFF3B30)
            )
            Spacer(modifier = Modifier.height(8.dp))

            breachedDomains.forEach { domainData ->
                val domainSchema = domainSchemas.find { it.id == domainData.id }
                val domainLabel = if (domainSchema != null) {
                    if (hindi) domainSchema.labelHi else domainSchema.label
                } else {
                    domainData.id
                }

                val breachedMetrics = domainData.metrics.filter { metric ->
                    isBreach(metric, schema, domainData.id)
                }

                breachedMetrics.forEach { metric ->
                    val metricSchema = schema.resolveSlaMetricSchema(domainData.id, metric.id)
                    val metricLabel = if (metricSchema != null) {
                        if (hindi) metricSchema.labelHi else metricSchema.label
                    } else {
                        metric.id
                    }
                    val direction = metricSchema?.direction ?: "above"
                    val delta = kotlin.math.abs(metric.value - metric.threshold)
                    val deltaFormatted = String.format("%.1f", delta)

                    Text(
                        text = "\u2022 $domainLabel \u2014 $metricLabel: ${deltaFormatted}${metricSchema?.unit ?: ""} ${
                            if (direction == "above") {
                                if (hindi) "\u0915\u092E" else "below threshold"
                            } else {
                                if (hindi) "\u0905\u0927\u093F\u0915" else "above threshold"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ── Domain Card ─────────────────────────────────────────────────────────

@Composable
private fun DomainCard(
    domainData: SlaDomainData,
    domainSchema: SlaDomainSchema,
    schema: SchemaResolver,
    hindi: Boolean
) {
    var expanded by rememberSaveable(domainData.id) { mutableStateOf(false) }

    val domainLabel = if (hindi) domainSchema.labelHi else domainSchema.label

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Domain header row: label + metric dots + expand icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = domainLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Sub-metric status dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    domainData.metrics.forEach { metric ->
                        val dotColor = resolveMetricDotColor(metric, schema, domainData.id)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 5. Hysteresis display (always visible if breached)
            if (domainData.hysteresis != null && domainData.standing != "COMPLIANT") {
                Spacer(modifier = Modifier.height(8.dp))
                val current = domainData.hysteresis.currentCleanWindows
                val required = domainData.hysteresis.requiredCleanWindows
                Text(
                    text = if (hindi)
                        "\u0930\u093F\u0915\u0935\u0930\u0940: $current/$required \u0938\u094D\u0935\u091A\u094D\u091B \u092E\u0942\u0932\u094D\u092F\u093E\u0902\u0915\u0928 \u0935\u093F\u0902\u0921\u094B"
                    else
                        "Recovery: $current/$required clean evaluation windows",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF8000)
                )
            }

            // Expandable detail
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    domainSchema.metrics.forEach { metricSchema ->
                        val metricData = domainData.metrics.find { it.id == metricSchema.id }
                        if (metricData != null) {
                            MetricDetail(
                                metricData = metricData,
                                metricSchema = metricSchema,
                                hindi = hindi
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Metric Detail (expanded view) ───────────────────────────────────────

@Composable
private fun MetricDetail(
    metricData: SlaMetricData,
    metricSchema: SlaMetricSchema,
    hindi: Boolean
) {
    val metricLabel = if (hindi) metricSchema.labelHi else metricSchema.label
    val direction = metricSchema.direction
    val insufficientData = metricData.sampleCount < metricData.minSample

    // Bar scale calculation
    val barPct: Float
    val thresholdPct: Float
    val severePct: Float

    if (direction == "above") {
        // For "above" direction: higher is better, scale to 100
        barPct = (metricData.value / 100.0).toFloat().coerceIn(0f, 1f)
        thresholdPct = (metricData.threshold / 100.0).toFloat().coerceIn(0f, 1f)
        severePct = if (metricData.severeThreshold != null) {
            (metricData.severeThreshold / 100.0).toFloat().coerceIn(0f, 1f)
        } else 0f
    } else {
        // For "below" direction: lower is better, max = severeThreshold * 1.5
        val maxScale = (metricData.severeThreshold ?: metricData.threshold * 2.0) * 1.5
        barPct = (metricData.value / maxScale).toFloat().coerceIn(0f, 1f)
        thresholdPct = (metricData.threshold / maxScale).toFloat().coerceIn(0f, 1f)
        severePct = if (metricData.severeThreshold != null) {
            (metricData.severeThreshold / maxScale).toFloat().coerceIn(0f, 1f)
        } else 0f
    }

    // Determine if breached
    val isBreach = if (insufficientData) false else {
        if (direction == "above") metricData.value < metricData.threshold
        else metricData.value > metricData.threshold
    }

    // Bar color
    val barColor = when {
        insufficientData -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isBreach -> Color(0xFFFF3B30)
        else -> Color(0xFF34C759)
    }

    // Trend
    val trendArrow = when (metricData.trend) {
        "IMPROVING" -> "\u2191"
        "DECLINING" -> "\u2193"
        else -> "\u2192"
    }
    val trendColor = when (metricData.trend) {
        "IMPROVING" -> Color(0xFF34C759)
        "DECLINING" -> Color(0xFFFF3B30)
        else -> Color(0xFFFF8000)
    }
    val trendLabel = when (metricData.trend) {
        "IMPROVING" -> if (hindi) "\u0938\u0941\u0927\u093E\u0930" else "Improving"
        "DECLINING" -> if (hindi) "\u0917\u093F\u0930\u093E\u0935\u091F" else "Declining"
        else -> if (hindi) "\u0938\u094D\u0925\u093F\u0930" else "Stable"
    }

    Column {
        // Metric label + trend
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = metricLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = trendArrow,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = trendColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = trendLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = trendColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Value display
        Text(
            text = "${String.format("%.1f", metricData.value)}${metricSchema.unit}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isBreach) Color(0xFFFF3B30) else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Visual bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Value bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(barPct)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )

            // Threshold marker
            if (thresholdPct in 0.01f..0.99f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(thresholdPct)
                        .height(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .height(8.dp)
                            .background(Color(0xFFFF8000))
                    )
                }
            }

            // Severe threshold marker
            if (severePct in 0.01f..0.99f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(severePct)
                        .height(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .height(8.dp)
                            .background(Color(0xFFFF3B30))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Threshold labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (hindi)
                    "\u0938\u0940\u092E\u093E: ${String.format("%.1f", metricData.threshold)}${metricSchema.unit}"
                else
                    "Threshold: ${String.format("%.1f", metricData.threshold)}${metricSchema.unit}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFF8000)
            )
            if (metricData.severeThreshold != null) {
                Text(
                    text = if (hindi)
                        "\u0917\u0902\u092D\u0940\u0930: ${String.format("%.1f", metricData.severeThreshold)}${metricSchema.unit}"
                    else
                        "Severe: ${String.format("%.1f", metricData.severeThreshold)}${metricSchema.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF3B30)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Guidance and breach info
        if (insufficientData) {
            Text(
                text = if (hindi)
                    "\u0905\u092A\u0930\u094D\u092F\u093E\u092A\u094D\u0924 \u0921\u0947\u091F\u093E (${metricData.sampleCount}/${metricData.minSample} \u0928\u092E\u0942\u0928\u0947)"
                else
                    "Insufficient data (${metricData.sampleCount}/${metricData.minSample} samples)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (isBreach) {
            val delta = kotlin.math.abs(metricData.value - metricData.threshold)
            Text(
                text = if (direction == "above") {
                    if (hindi)
                        "\u0938\u0940\u092E\u093E \u0938\u0947 ${String.format("%.1f", delta)}${metricSchema.unit} \u0928\u0940\u091A\u0947"
                    else
                        "${String.format("%.1f", delta)}${metricSchema.unit} below threshold"
                } else {
                    if (hindi)
                        "\u0938\u0940\u092E\u093E \u0938\u0947 ${String.format("%.1f", delta)}${metricSchema.unit} \u090A\u092A\u0930"
                    else
                        "${String.format("%.1f", delta)}${metricSchema.unit} above threshold"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFFF3B30)
            )
        }

        // Direction guidance
        Text(
            text = if (direction == "above") {
                if (hindi)
                    "${String.format("%.1f", metricData.threshold)}${metricSchema.unit} \u0938\u0947 \u090A\u092A\u0930 \u0939\u094B\u0928\u093E \u091A\u093E\u0939\u093F\u090F"
                else
                    "Needs to be above ${String.format("%.1f", metricData.threshold)}${metricSchema.unit}"
            } else {
                if (hindi)
                    "${String.format("%.1f", metricData.threshold)}${metricSchema.unit} \u0938\u0947 \u0928\u0940\u091A\u0947 \u0939\u094B\u0928\u093E \u091A\u093E\u0939\u093F\u090F"
                else
                    "Needs to be below ${String.format("%.1f", metricData.threshold)}${metricSchema.unit}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────

/**
 * Resolve standing to (badge color, background color, label).
 */
private fun resolveStandingDisplay(standing: String, hindi: Boolean): Triple<Color, Color, String> {
    return when (standing) {
        "COMPLIANT" -> Triple(
            Color(0xFF34C759),
            Color(0xFF34C759).copy(alpha = 0.12f),
            if (hindi) "\u0905\u0928\u0941\u092A\u093E\u0932\u0928 \u092E\u0947\u0902" else "Compliant"
        )
        "AT_RISK" -> Triple(
            Color(0xFFFF8000),
            Color(0xFFFF8000).copy(alpha = 0.12f),
            if (hindi) "\u091C\u094B\u0916\u093F\u092E \u092E\u0947\u0902" else "At Risk"
        )
        "NON_COMPLIANT" -> Triple(
            Color(0xFFFF3B30),
            Color(0xFFFF3B30).copy(alpha = 0.12f),
            if (hindi) "\u0917\u0948\u0930-\u0905\u0928\u0941\u092A\u093E\u0932\u0928" else "Non-Compliant"
        )
        else -> Triple(
            Color(0xFF888888),
            Color(0xFF888888).copy(alpha = 0.12f),
            standing
        )
    }
}

/**
 * Check if a metric is in breach state.
 * Returns false if insufficient data (sampleCount < minSample).
 */
private fun isBreach(metric: SlaMetricData, schema: SchemaResolver, domainId: String): Boolean {
    if (metric.sampleCount < metric.minSample) return false
    val metricSchema = schema.resolveSlaMetricSchema(domainId, metric.id)
    val direction = metricSchema?.direction ?: "above"
    return if (direction == "above") {
        metric.value < metric.threshold
    } else {
        metric.value > metric.threshold
    }
}

/**
 * Resolve metric dot color: green (ok), amber (near threshold), red (breach).
 */
@Composable
private fun resolveMetricDotColor(
    metric: SlaMetricData,
    schema: SchemaResolver,
    domainId: String
): Color {
    if (metric.sampleCount < metric.minSample) {
        return MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    val metricSchema = schema.resolveSlaMetricSchema(domainId, metric.id)
    val direction = metricSchema?.direction ?: "above"

    val breached = if (direction == "above") {
        metric.value < metric.threshold
    } else {
        metric.value > metric.threshold
    }

    if (breached) return Color(0xFFFF3B30)

    // Check if close to threshold (within 10% of threshold value)
    val margin = metric.threshold * 0.1
    val nearThreshold = if (direction == "above") {
        metric.value < metric.threshold + margin
    } else {
        metric.value > metric.threshold - margin
    }

    return if (nearThreshold) Color(0xFFFF8000) else Color(0xFF34C759)
}
