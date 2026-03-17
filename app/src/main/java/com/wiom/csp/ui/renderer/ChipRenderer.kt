package com.wiom.csp.ui.renderer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.domain.model.AssuranceData
import com.wiom.csp.domain.model.ChipSchema
import com.wiom.csp.domain.schema.SchemaResolver
import java.text.NumberFormat
import java.util.Locale

/**
 * Renders a horizontal row of assurance chips from schema.
 * The number of chips is driven by schema, not hardcoded — build rule #4.
 */
@Composable
fun AssuranceChipStrip(
    chips: List<ChipSchema>,
    assurance: AssuranceData,
    schema: SchemaResolver,
    hindi: Boolean,
    onChipClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { chip ->
            AssuranceChip(
                chip = chip,
                assurance = assurance,
                hindi = hindi,
                onClick = { onChipClick(chip.id) }
            )
        }
    }
}

@Composable
private fun AssuranceChip(
    chip: ChipSchema,
    assurance: AssuranceData,
    hindi: Boolean,
    onClick: () -> Unit
) {
    val label = if (hindi) chip.labelHi else chip.label

    // Resolve value from chipValues map or direct field
    val chipValue = assurance.chipValues[chip.id]
    val rawValue = chipValue?.value ?: resolveDirectField(chip.valueField, assurance)
    val formattedValue = remember(rawValue, chip.format) {
        formatChipValue(rawValue, chip.format)
    }

    // State color: COMPLIANT=green, AT_RISK=amber, NON_COMPLIANT/INELIGIBLE=red
    val stateColorStr = chipValue?.stateColor
        ?: if (chip.stateColorField != null) resolveDirectField(chip.stateColorField, assurance) else null
    val valueColor = resolveStateColor(stateColorStr)

    // Trend arrow
    val trendStr = chipValue?.trend
        ?: if (chip.trendField != null) resolveDirectField(chip.trendField, assurance) else null
    val trendArrow = when (trendStr) {
        "IMPROVING" -> "\u2191"
        "DECLINING" -> "\u2193"
        "STABLE" -> "\u2192"
        else -> null
    }

    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (trendArrow != null) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = trendArrow,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (trendStr) {
                            "IMPROVING" -> Color(0xFF34C759)
                            "DECLINING" -> Color(0xFFFF3B30)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * Resolve a direct field from AssuranceData by field name.
 * Used when chipValues map doesn't contain the chip's value.
 */
private fun resolveDirectField(fieldName: String, assurance: AssuranceData): String {
    return when (fieldName) {
        "activeBase", "active_base" -> assurance.activeBase.toString()
        "cycleEarned", "cycle_earned" -> assurance.cycleEarned.toString()
        "lifetimeEarnings", "lifetime_earnings" -> assurance.lifetimeEarnings.toString()
        "slaStanding", "sla_standing" -> assurance.slaStanding
        "exposureState", "exposure_state" -> assurance.exposureState
        "exposureTrend", "exposure_trend" -> assurance.exposureTrend
        "activeRestores", "active_restores" -> assurance.activeRestores.toString()
        "unresolvedCount", "unresolved_count" -> assurance.unresolvedCount.toString()
        else -> "--"
    }
}

/**
 * Format chip value based on format type.
 * "currency" → ₹ with compact Indian formatting (₹12.5K for 12500)
 * "number" → plain number
 */
private fun formatChipValue(rawValue: String, format: String): String {
    // Map state strings to short display labels
    val stateLabel = when (rawValue.uppercase()) {
        "COMPLIANT" -> "Good"
        "AT_RISK" -> "At Risk"
        "NON_COMPLIANT" -> "Low"
        "ELIGIBLE" -> "Full"
        "LIMITED" -> "Limited"
        "INELIGIBLE" -> "None"
        else -> null
    }
    if (stateLabel != null) return stateLabel

    return when (format) {
        "currency" -> {
            val numericValue = rawValue.toDoubleOrNull() ?: return rawValue
            formatIndianCurrency(numericValue)
        }
        "percentage" -> {
            val numericValue = rawValue.toDoubleOrNull() ?: return rawValue
            "${numericValue.toInt()}%"
        }
        else -> { // "number" and fallback
            val numericValue = rawValue.toDoubleOrNull()
            if (numericValue != null && numericValue == numericValue.toLong().toDouble()) {
                numericValue.toLong().toString()
            } else {
                rawValue
            }
        }
    }
}

/**
 * Compact Indian currency formatting.
 * ₹12.5K for 12500, ₹1.2L for 120000, plain ₹500 for small amounts.
 */
private fun formatIndianCurrency(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        absAmount >= 10_000_000 -> {
            val cr = absAmount / 10_000_000
            "${sign}\u20B9${formatCompact(cr)}Cr"
        }
        absAmount >= 100_000 -> {
            val lakh = absAmount / 100_000
            "${sign}\u20B9${formatCompact(lakh)}L"
        }
        absAmount >= 1_000 -> {
            val k = absAmount / 1_000
            "${sign}\u20B9${formatCompact(k)}K"
        }
        else -> {
            val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            fmt.maximumFractionDigits = 0
            sign + fmt.format(absAmount).replace("-", "")
        }
    }
}

private fun formatCompact(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
    }
}

/**
 * Resolve semantic state color.
 * COMPLIANT=green, AT_RISK=amber, NON_COMPLIANT/INELIGIBLE=red, else null for default.
 */
@Composable
private fun resolveStateColor(stateColor: String?): Color? {
    return when (stateColor?.uppercase()) {
        "COMPLIANT", "ELIGIBLE", "ACTIVE", "GREEN" -> Color(0xFF34C759)
        "AT_RISK", "WARNING", "AMBER" -> Color(0xFFFF8000)
        "NON_COMPLIANT", "INELIGIBLE", "CRITICAL", "RED" -> Color(0xFFFF3B30)
        else -> null
    }
}
