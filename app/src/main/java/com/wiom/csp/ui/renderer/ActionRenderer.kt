package com.wiom.csp.ui.renderer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wiom.csp.domain.model.ActionSchema

/**
 * Renders action buttons in a row at the bottom of a task detail screen.
 * Button style is driven entirely by ActionSchema — never hardcoded per task type.
 *
 * style="primary"     -> filled button with brand color
 * style="secondary"   -> outlined button
 * style="destructive" -> red filled button
 *
 * All buttons are disabled when loading=true to prevent double-taps.
 */
@Composable
fun ActionFooter(
    actions: List<ActionSchema>,
    hindi: Boolean,
    loading: Boolean,
    onAction: (ActionSchema) -> Unit
) {
    if (actions.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                val label = if (hindi) action.labelHi else action.label
                val modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                val shape = RoundedCornerShape(8.dp)

                when (action.style) {
                    "primary" -> {
                        Button(
                            onClick = { onAction(action) },
                            enabled = !loading,
                            modifier = modifier,
                            shape = shape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    "destructive" -> {
                        Button(
                            onClick = { onAction(action) },
                            enabled = !loading,
                            modifier = modifier,
                            shape = shape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF3B30),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFFF3B30).copy(alpha = 0.38f),
                                disabledContentColor = Color.White.copy(alpha = 0.38f)
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    else -> { // "secondary" and any fallback
                        OutlinedButton(
                            onClick = { onAction(action) },
                            enabled = !loading,
                            modifier = modifier,
                            shape = shape
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
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
