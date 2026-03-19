package com.wiom.csp.ui.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.domain.schema.SchemaResolver

/**
 * Generic task card composable that renders ANY task type in ANY state based on schema.
 * Never hardcodes business logic — all display rules come from [SchemaResolver].
 */
@Composable
fun TaskCardRenderer(
    task: TaskData,
    bucketIndex: Int,
    schema: SchemaResolver,
    hindi: Boolean,
    onCardClick: (String) -> Unit,
    onAction: (String, String) -> Unit
) {
    val taskType = task.taskType
    val state = task.currentState

    val dotColor = remember(taskType) {
        parseColor(schema.resolveTaskColor(taskType))
    }
    val urgencyColor = remember(task, bucketIndex) {
        parseColor(schema.resolveUrgencyColor(task, bucketIndex))
    }
    val typeLabel = remember(taskType, hindi) {
        schema.resolveTaskTypeLabel(taskType, hindi)
    }
    val reasonLabel = remember(taskType, state, task, hindi) {
        schema.resolveReasonLabel(taskType, state, task, hindi)
    }
    val timerField = remember(taskType, state) {
        schema.resolveTimerField(taskType, state)
    }
    val timerDeadline = remember(task, timerField) {
        task.getTimerDeadline(timerField)
    }
    val actions = schema.resolveActions(taskType, state)

    val identityText = buildString {
        append(typeLabel)
        task.connectionId?.let { append(" \u00B7 $it") }
        task.customerArea?.let { append(" \u00B7 $it") }
    }

    val accessibilityDescription = buildString {
        append("$taskType task")
        task.connectionId?.let { append(" $it") }
        task.customerArea?.let { append(" in $it") }
        append(", $reasonLabel")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(task.taskId) }
            .semantics { contentDescription = accessibilityDescription },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left urgency border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(urgencyColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Line 1: Type dot + identity text
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = identityText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Line 2: Reason label (largest text)
                Text(
                    text = reasonLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Line 3: Timer + note indicator + CTA button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Timer
                        Text(
                            text = formatTimer(timerDeadline),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = timerColor(timerDeadline)
                        )

                        // Note indicator
                        val noteText = resolveNoteIndicator(task)
                        if (noteText != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = noteText.first,
                                style = MaterialTheme.typography.bodySmall,
                                color = noteText.second
                            )
                        }
                    }

                    // CTA button (first action)
                    val firstAction = actions.firstOrNull()
                    if (firstAction != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { onAction(task.taskId, firstAction.id) },
                            modifier = Modifier.height(30.dp),
                            contentPadding = ButtonDefaults.ContentPadding.let {
                                ButtonDefaults.TextButtonContentPadding
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (hindi) firstAction.labelHi else firstAction.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats a timer deadline into human-readable relative time.
 * "Xm" if <60min, "Xh Ym" if <24h, "Xd Yh" if >24h, "Overdue" if past, "--" if null.
 */
private fun formatTimer(deadlineMs: Long?): String {
    if (deadlineMs == null) return "--"
    val now = System.currentTimeMillis()
    val diff = deadlineMs - now
    if (diff <= 0) return "Overdue"

    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h ${minutes % 60}m"
        else -> "${days}d ${hours % 24}h"
    }
}

/**
 * Returns the color for the timer text based on urgency.
 */
@Composable
private fun timerColor(deadlineMs: Long?): Color {
    if (deadlineMs == null) return MaterialTheme.colorScheme.onSurfaceVariant
    val now = System.currentTimeMillis()
    val diff = deadlineMs - now
    val minutes = diff / 60_000
    return when {
        diff <= 0 -> Color(0xFFFF3B30) // Overdue - red
        minutes < 30 -> Color(0xFFFF3B30) // Critical - red
        minutes < 120 -> Color(0xFFFF8000) // Urgent - amber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * Resolves the note indicator text and color for a task card.
 * "Wiom replied" in blue if any note has authorType="WIOM", else "N notes" in default.
 */
@Composable
private fun resolveNoteIndicator(task: TaskData): Pair<String, Color>? {
    if (task.notes.isEmpty()) return null
    val hasWiomReply = task.notes.any { it.authorType == "WIOM" }
    return if (hasWiomReply) {
        "Wiom replied" to Color(0xFF2196F3)
    } else {
        "${task.notes.size} notes" to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/** Parse hex color string to Compose Color. Falls back to grey on failure. */
internal fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF888888)
    }
}
