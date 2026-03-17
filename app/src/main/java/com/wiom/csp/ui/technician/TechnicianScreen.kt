package com.wiom.csp.ui.technician

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.domain.model.TechnicianData
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.ui.common.ConfirmationToast
import com.wiom.csp.ui.common.StatusBadge
import com.wiom.csp.ui.renderer.TaskCardRenderer
import com.wiom.csp.ui.renderer.TaskDetailRenderer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Container for the technician sub-app.
 * Internal navigation: LOGIN -> DASHBOARD -> TASK_DETAIL / PROFILE.
 */
@Composable
fun TechnicianScreen(
    schema: SchemaResolver,
    hindi: Boolean,
    onBack: () -> Unit
) {
    val viewModel: TechViewModel = hiltViewModel()
    val view by viewModel.view.collectAsState()
    val technicians by viewModel.technicians.collectAsState()
    val tech by viewModel.tech.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    val confirmMessage by viewModel.confirmMessage.collectAsState()

    // BackHandler chain
    BackHandler(enabled = true) {
        when (view) {
            TechView.TASK_DETAIL -> viewModel.navigateTo(TechView.DASHBOARD)
            TechView.PROFILE -> viewModel.navigateTo(TechView.DASHBOARD)
            TechView.DASHBOARD -> onBack()
            TechView.LOGIN -> onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (view) {
            TechView.LOGIN -> {
                TechLoginContent(
                    technicians = technicians,
                    hindi = hindi,
                    onSelectTech = { viewModel.loginAs(it) },
                    onBack = onBack
                )
            }

            TechView.DASHBOARD -> {
                tech?.let { currentTech ->
                    TechDashboardContent(
                        tech = currentTech,
                        tasks = tasks,
                        schema = schema,
                        hindi = hindi,
                        onTaskClick = { taskId -> viewModel.selectTask(taskId) },
                        onAction = { taskId, actionId ->
                            viewModel.handleAction(taskId, actionId, emptyMap())
                        },
                        onToggleAvailability = { viewModel.toggleAvailability() },
                        onProfileClick = { viewModel.navigateTo(TechView.PROFILE) },
                        onBack = onBack
                    )
                }
            }

            TechView.TASK_DETAIL -> {
                val task = tasks.find { it.taskId == selectedTaskId }
                if (task != null) {
                    TaskDetailRenderer(
                        task = task,
                        schema = schema,
                        hindi = hindi,
                        technicians = technicians,
                        onAction = { taskId, actionId, params ->
                            viewModel.handleAction(taskId, actionId, params)
                        },
                        onBack = { viewModel.navigateTo(TechView.DASHBOARD) },
                        onCallCustomer = { /* Intent handled by parent */ },
                        onCallTech = { /* N/A for tech view */ },
                        onAddNote = { /* Quick note */ },
                        onRequestHelp = { /* Help request */ }
                    )
                } else {
                    viewModel.navigateTo(TechView.DASHBOARD)
                }
            }

            TechView.PROFILE -> {
                tech?.let { currentTech ->
                    TechProfileContent(
                        tech = currentTech,
                        hindi = hindi,
                        onLogout = { viewModel.logout() },
                        onBack = { viewModel.navigateTo(TechView.DASHBOARD) }
                    )
                }
            }
        }

        // Confirmation toast overlay
        confirmMessage?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ConfirmationToast(
                    message = message,
                    onDismiss = { viewModel.dismissConfirmation() }
                )
            }
        }
    }
}

// ── Login: Profile picker ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechLoginContent(
    technicians: List<TechnicianData>,
    hindi: Boolean,
    onSelectTech: (TechnicianData) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (hindi) "\u0924\u0915\u0928\u0940\u0936\u093F\u092F\u0928 \u0932\u0949\u0917\u093F\u0928" else "Technician Login",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = if (hindi)
                        "\u0915\u0947\u0935\u0932 \u0906\u092A\u0915\u0947 CSP \u0926\u094D\u0935\u093E\u0930\u093E \u091C\u094B\u0921\u093C\u0947 \u0917\u090F \u0924\u0915\u0928\u0940\u0936\u093F\u092F\u0928 \u0932\u0949\u0917 \u0907\u0928 \u0915\u0930 \u0938\u0915\u0924\u0947 \u0939\u0948\u0902"
                    else "Only technicians added by your CSP can log in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (technicians.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hindi) "\u0915\u094B\u0908 \u0924\u0915\u0928\u0940\u0936\u093F\u092F\u0928 \u0928\u0939\u0940\u0902 \u092E\u093F\u0932\u093E" else "No technicians found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(technicians, key = { it.id }) { techItem ->
                        TechProfileCard(
                            tech = techItem,
                            hindi = hindi,
                            onClick = { onSelectTech(techItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TechProfileCard(
    tech: TechnicianData,
    hindi: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tech.name.firstOrNull()?.uppercase() ?: "T",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tech.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tech.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Band badge
            StatusBadge(
                label = "Band ${tech.band}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                bgColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Availability dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (tech.available) Color(0xFF34C759) else Color(0xFFFF3B30)
                    )
            )
        }
    }
}

// ── Dashboard ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechDashboardContent(
    tech: TechnicianData,
    tasks: List<TaskData>,
    schema: SchemaResolver,
    hindi: Boolean,
    onTaskClick: (String) -> Unit,
    onAction: (String, String) -> Unit,
    onToggleAvailability: () -> Unit,
    onProfileClick: () -> Unit,
    onBack: () -> Unit
) {
    val activeTasks = remember(tasks) {
        tasks.filter { !schema.isTerminalState(it.taskType, it.currentState) }
    }
    val completedTasks = remember(tasks) {
        tasks.filter { schema.isTerminalState(it.taskType, it.currentState) }
    }

    var completedExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tech.name.firstOrNull()?.uppercase() ?: "T",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tech.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(
                            label = tech.band,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            bgColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Availability toggle
                    Text(
                        text = if (tech.available) {
                            if (hindi) "\u0909\u092A\u0932\u092C\u094D\u0927" else "Available"
                        } else {
                            if (hindi) "\u0905\u0928\u0941\u092A\u0932\u092C\u094D\u0927" else "Offline"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tech.available) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = tech.available,
                        onCheckedChange = { onToggleAvailability() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Active tasks section
            item {
                Text(
                    text = if (hindi) "\u0938\u0915\u094D\u0930\u093F\u092F \u0915\u093E\u0930\u094D\u092F" else "Active Tasks",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (activeTasks.isEmpty()) {
                item {
                    Text(
                        text = if (hindi) "\u0915\u094B\u0908 \u0938\u0915\u094D\u0930\u093F\u092F \u0915\u093E\u0930\u094D\u092F \u0928\u0939\u0940\u0902" else "No active tasks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(activeTasks, key = { it.taskId }) { task ->
                    TaskCardRenderer(
                        task = task,
                        bucketIndex = activeTasks.indexOf(task),
                        schema = schema,
                        hindi = hindi,
                        onCardClick = onTaskClick,
                        onAction = onAction
                    )
                }
            }

            // Completed tasks section (collapsible)
            if (completedTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { completedExpanded = !completedExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (hindi)
                                "\u092A\u0942\u0930\u094D\u0923 \u0915\u093E\u0930\u094D\u092F (${completedTasks.size})"
                            else "Completed Tasks (${completedTasks.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            imageVector = if (completedExpanded) Icons.Filled.ExpandLess
                            else Icons.Filled.ExpandMore,
                            contentDescription = if (completedExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (completedExpanded) {
                    items(completedTasks, key = { "completed_${it.taskId}" }) { task ->
                        TaskCardRenderer(
                            task = task,
                            bucketIndex = completedTasks.indexOf(task) + activeTasks.size,
                            schema = schema,
                            hindi = hindi,
                            onCardClick = onTaskClick,
                            onAction = onAction
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Profile ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechProfileContent(
    tech: TechnicianData,
    hindi: Boolean,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("en", "IN")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (hindi) "\u092A\u094D\u0930\u094B\u092B\u093C\u093E\u0907\u0932" else "Profile",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = tech.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            StatusBadge(
                label = "Band ${tech.band}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                bgColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileRow(
                        label = if (hindi) "\u092B\u093C\u094B\u0928" else "Phone",
                        value = tech.phone
                    )
                    ProfileRow(
                        label = if (hindi) "\u092C\u0948\u0902\u0921" else "Band",
                        value = tech.band
                    )
                    ProfileRow(
                        label = if (hindi) "\u0909\u092A\u0932\u092C\u094D\u0927\u0924\u093E" else "Availability",
                        value = if (tech.available) {
                            if (hindi) "\u0909\u092A\u0932\u092C\u094D\u0927" else "Available"
                        } else {
                            if (hindi) "\u0905\u0928\u0941\u092A\u0932\u092C\u094D\u0927" else "Offline"
                        }
                    )
                    ProfileRow(
                        label = if (hindi) "\u0936\u093E\u092E\u093F\u0932 \u0939\u0941\u090F" else "Joined",
                        value = dateFormat.format(Date(tech.joinDate))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout button
            TextButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hindi) "\u0932\u0949\u0917 \u0906\u0909\u091F" else "Logout",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
