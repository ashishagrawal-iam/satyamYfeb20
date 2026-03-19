package com.wiom.csp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.domain.model.AssuranceData
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.ui.common.FilterChipRow
import com.wiom.csp.ui.common.ShimmerBox
import com.wiom.csp.ui.renderer.AssuranceChipStrip
import com.wiom.csp.ui.renderer.TaskCardRenderer

/**
 * Home screen: the CSP's primary interface.
 * Renders a header, assurance chip strip, filter row, safety banner,
 * and a two-zone task feed (YOUR TASKS + AVAILABLE).
 *
 * All display decisions flow through [SchemaResolver] — nothing is hardcoded.
 * Task order matches the server-provided order (build rule #5: no local sorting).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tasks: List<TaskData>,
    assurance: AssuranceData?,
    isOffline: Boolean,
    isLoading: Boolean,
    isRefreshing: Boolean,
    capabilityResetActive: Boolean,
    schema: SchemaResolver,
    hindi: Boolean,
    activeFilter: String,
    fadingTasks: Map<String, Boolean>,
    onFilterChange: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    onTaskAction: (String, String) -> Unit,
    onChipClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit
) {
    // Partition tasks into Zone A (YOUR TASKS) and Zone B (AVAILABLE/OFFERED).
    // Preserve server order within each zone — NO local sorting.
    val filteredTasks = remember(tasks, activeFilter) {
        if (activeFilter == "All") tasks
        else tasks.filter { it.taskType == activeFilter }
    }
    val zoneATasks = remember(filteredTasks) {
        filteredTasks.filter { it.currentState != "OFFERED" }
    }
    val zoneBTasks = remember(filteredTasks) {
        filteredTasks.filter { it.currentState == "OFFERED" }
    }

    // Safety banner: count critical tasks (bucketIndex <= 1) hidden by current filter.
    val hiddenCriticalCount = remember(tasks, activeFilter) {
        if (activeFilter == "All") 0
        else {
            // Tasks not matching the filter that are in the first two bucket positions
            // (index 0 or 1 in server order) — i.e. the most urgent tasks.
            val allNonOffered = tasks.filter { it.currentState != "OFFERED" }
            allNonOffered.take(2).count { it.taskType != activeFilter }
        }
    }

    // Build filter chip labels from schema task types
    val filterLabels = remember(schema) {
        val typeLabels = schema.getSchema().taskTypes.map { (key, _) -> key }
        listOf("All") + typeLabels
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ── 1. Header: hamburger + partner ID + avatar ──────────────
        HeaderRow(onMenuClick = onMenuClick)

        // ── 2. Capability Reset Banner ──────────────────────────────
        AnimatedVisibility(visible = capabilityResetActive) {
            CapabilityResetBanner()
        }

        // ── 3. Assurance Chip Strip ─────────────────────────────────
        AssuranceSection(
            assurance = assurance,
            isLoading = isLoading,
            schema = schema,
            hindi = hindi,
            onChipClick = onChipClick,
            onRetry = onRefresh
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── 4. Filter Chip Row ──────────────────────────────────────
        FilterChipRow(
            items = filterLabels,
            selected = activeFilter,
            onSelect = onFilterChange
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── 5. Safety Banner ────────────────────────────────────────
        AnimatedVisibility(visible = hiddenCriticalCount > 0) {
            SafetyBanner(count = hiddenCriticalCount)
        }

        // ── 6. Task Feed with pull-to-refresh ───────────────────────
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            if (isLoading && tasks.isEmpty()) {
                // Loading shimmer placeholders
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(3) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Zone A: YOUR TASKS ──────────────────────
                    item(key = "zone_a_header") {
                        ZoneHeader(
                            title = "YOUR TASKS (${zoneATasks.size})"
                        )
                    }

                    if (zoneATasks.isEmpty()) {
                        item(key = "zone_a_empty") {
                            EmptyState(message = if (hindi) "कोई सक्रिय कार्य नहीं" else "No active tasks")
                        }
                    } else {
                        items(
                            items = zoneATasks,
                            key = { it.taskId }
                        ) { task ->
                            val isFading = fadingTasks[task.taskId] == true
                            Box(
                                modifier = if (isFading) Modifier.alpha(0.3f) else Modifier
                            ) {
                                TaskCardRenderer(
                                    task = task,
                                    bucketIndex = tasks.indexOf(task),
                                    schema = schema,
                                    hindi = hindi,
                                    onCardClick = onTaskClick,
                                    onAction = onTaskAction
                                )
                                // Resolved overlay for fading tasks
                                if (isFading) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF34C759).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (hindi) "हल हो गया" else "Resolved",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFF34C759)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Zone B: AVAILABLE ────────────────────────
                    item(key = "zone_b_header") {
                        Spacer(modifier = Modifier.height(12.dp))
                        ZoneHeader(title = "AVAILABLE")
                    }

                    if (zoneBTasks.isEmpty()) {
                        item(key = "zone_b_empty") {
                            EmptyState(
                                message = if (hindi) "कोई उपलब्ध ऑफर नहीं" else "No available offers"
                            )
                        }
                    } else {
                        items(
                            items = zoneBTasks,
                            key = { it.taskId }
                        ) { task ->
                            val isFading = fadingTasks[task.taskId] == true
                            Box(
                                modifier = if (isFading) Modifier.alpha(0.3f) else Modifier
                            ) {
                                TaskCardRenderer(
                                    task = task,
                                    bucketIndex = tasks.indexOf(task),
                                    schema = schema,
                                    hindi = hindi,
                                    onCardClick = onTaskClick,
                                    onAction = onTaskAction
                                )
                                if (isFading) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF34C759).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (hindi) "हल हो गया" else "Resolved",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFF34C759)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom spacer so last card isn't clipped by nav
                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

// ── Header ──────────────────────────────────────────────────────────────

@Composable
private fun HeaderRow(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open menu",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "CSP-MH-1001",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Avatar circle with initials
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}

// ── Capability Reset Banner ─────────────────────────────────────────────

@Composable
private fun CapabilityResetBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFF8000))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Capability assessment in progress",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

// ── Assurance Section ───────────────────────────────────────────────────

@Composable
private fun AssuranceSection(
    assurance: AssuranceData?,
    isLoading: Boolean,
    schema: SchemaResolver,
    hindi: Boolean,
    onChipClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    val chips = remember(schema) { schema.resolveAssuranceChips() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        when {
            assurance != null -> {
                AssuranceChipStrip(
                    chips = chips,
                    assurance = assurance,
                    schema = schema,
                    hindi = hindi,
                    onChipClick = onChipClick
                )
            }
            isLoading -> {
                // Shimmer placeholders for assurance chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(chips.size.coerceAtLeast(3)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(100.dp)
                                .height(56.dp)
                        )
                    }
                }
            }
            else -> {
                // Error state with retry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (hindi) "डेटा लोड करने में विफल" else "Failed to load assurance data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onRetry) {
                        Text(
                            text = if (hindi) "पुनः प्रयास करें" else "Retry",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

// ── Safety Banner ───────────────────────────────────────────────────────

@Composable
private fun SafetyBanner(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "$count critical tasks hidden by filter",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFFF3B30)
        )
    }
}

// ── Zone Header ─────────────────────────────────────────────────────────

@Composable
private fun ZoneHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

// ── Empty State ─────────────────────────────────────────────────────────

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
