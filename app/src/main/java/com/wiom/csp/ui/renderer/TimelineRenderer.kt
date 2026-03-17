package com.wiom.csp.ui.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wiom.csp.domain.model.TimelineEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a vertical list of timeline entries sorted by timestamp ascending.
 * Each row: time | detail text | actor tag (colored pill by actorType).
 */
@Composable
fun TimelineRenderer(entries: List<TimelineEntry>, hindi: Boolean) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale("en", "IN")) }

    val sortedEntries = remember(entries) {
        entries.sortedBy { it.timestamp }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        sortedEntries.forEach { entry ->
            TimelineRow(
                entry = entry,
                timeFormat = timeFormat
            )
        }
    }
}

@Composable
private fun TimelineRow(
    entry: TimelineEntry,
    timeFormat: SimpleDateFormat
) {
    val formattedTime = remember(entry.timestamp) {
        timeFormat.format(Date(entry.timestamp))
    }

    val actorColors = resolveActorColors(entry.actorType)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Time column (fixed width)
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Detail text (flexible)
        Text(
            text = entry.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Actor tag (colored pill)
        Text(
            text = entry.actor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = actorColors.first,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(actorColors.second)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Resolve actor type to (text color, background color) pair.
 * SYSTEM=grey, CSP=brand purple, ADMIN=blue, TECHNICIAN=amber.
 */
@Composable
private fun resolveActorColors(actorType: String): Pair<Color, Color> {
    return when (actorType.uppercase()) {
        "SYSTEM" -> Color(0xFF666666) to Color(0x1A666666)
        "CSP" -> Color(0xFF6750A4) to Color(0x1A6750A4) // Material3 brand purple
        "ADMIN", "WIOM" -> Color(0xFF2196F3) to Color(0x1A2196F3)
        "TECHNICIAN" -> Color(0xFFFF8000) to Color(0x1AFF8000)
        else -> Color(0xFF888888) to Color(0x1A888888)
    }
}
