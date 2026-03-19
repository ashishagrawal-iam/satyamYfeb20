package com.wiom.csp.ui.renderer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.wiom.csp.domain.model.ActionSchema
import com.wiom.csp.domain.model.LabeledItem
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.domain.model.TechnicianData
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.ui.common.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full detail screen for any task, rendered entirely from schema.
 * All sections are conditional: shown/hidden based on schema flags for the current state.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailRenderer(
    task: TaskData,
    schema: SchemaResolver,
    hindi: Boolean,
    technicians: List<TechnicianData>,
    onAction: (String, String, Map<String, String>) -> Unit,
    onBack: () -> Unit,
    onCallCustomer: (String) -> Unit,
    onCallTech: (String) -> Unit,
    onAddNote: (String) -> Unit,
    onRequestHelp: (String) -> Unit
) {
    val taskType = task.taskType
    val state = task.currentState
    val stateSchema = remember(taskType, state) { schema.resolveTaskState(taskType, state) }
    val stateLabel = remember(taskType, state, hindi) { schema.resolveTaskLabel(taskType, state, hindi) }
    val typeLabel = remember(taskType, hindi) { schema.resolveTaskTypeLabel(taskType, hindi) }
    val dotColor = remember(taskType) { parseColor(schema.resolveTaskColor(taskType)) }
    val stateColor = remember(taskType, state) { parseColor(schema.resolveStateColor(taskType, state)) }
    val actions = remember(taskType, state) { schema.resolveActions(taskType, state) }

    var actionLoading by remember { mutableStateOf(false) }
    var selectedTechnician by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDeclineReason by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSlot by rememberSaveable { mutableStateOf<String?>(null) }
    var helpExpanded by rememberSaveable { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("en", "IN")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "$typeLabel \u00B7 ${task.connectionId ?: task.taskId}",
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
        },
        bottomBar = {
            if (actions.isNotEmpty()) {
                ActionFooter(
                    actions = actions,
                    hindi = hindi,
                    loading = actionLoading,
                    onAction = { action ->
                        val params = mutableMapOf<String, String>()
                        if (action.requiresTechnician && selectedTechnician != null) {
                            params["technicianId"] = selectedTechnician!!
                        }
                        if (action.requiresReason && selectedDeclineReason != null) {
                            params["reason"] = selectedDeclineReason!!
                        }
                        if (action.requiresSlot && selectedSlot != null) {
                            params["slot"] = selectedSlot!!
                        }
                        onAction(task.taskId, action.id, params)
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 2: Status-Contact bar
            item {
                Spacer(modifier = Modifier.height(4.dp))
                StatusContactBar(
                    dotColor = dotColor,
                    stateLabel = stateLabel,
                    stateColor = stateColor,
                    assignedToName = task.assignedToName,
                    customerPhone = task.customerPhone,
                    techPhone = technicians.find { it.id == task.assignedTo }?.phone,
                    onCallCustomer = { task.customerPhone?.let(onCallCustomer) },
                    onCallTech = {
                        technicians.find { it.id == task.assignedTo }?.phone?.let(onCallTech)
                    }
                )
            }

            // Section 3: "Need Help from Wiom" collapsible
            item {
                HelpFromWiomSection(
                    expanded = helpExpanded,
                    onToggle = { helpExpanded = !helpExpanded },
                    helpReasons = schema.helpReasons(),
                    hindi = hindi,
                    onRequestHelp = onRequestHelp
                )
            }

            // Section 4: Customer details (conditional)
            if (stateSchema?.showCustomerDetails == true) {
                item {
                    CustomerDetailsCard(task = task, dateFormat = dateFormat)
                }
            }

            // Section 5: Slot picker (conditional)
            if (stateSchema?.showSlotPicker == true) {
                item {
                    SlotPickerSection(
                        selectedSlot = selectedSlot,
                        onSlotSelected = { selectedSlot = it },
                        hindi = hindi
                    )
                }
            }

            // Section 6: Decline reason picker (conditional)
            if (stateSchema?.showDeclineReasons == true) {
                item {
                    DeclineReasonPicker(
                        reasons = schema.declineReasons(),
                        selectedReason = selectedDeclineReason,
                        onReasonSelected = { selectedDeclineReason = it },
                        hindi = hindi
                    )
                }
            }

            // Technician picker (if any action requires it)
            if (actions.any { it.requiresTechnician }) {
                item {
                    TechnicianPickerSection(
                        technicians = technicians,
                        selectedTechnician = selectedTechnician,
                        onTechnicianSelected = { selectedTechnician = it },
                        hindi = hindi
                    )
                }
            }

            // Section 7: Task info
            item {
                TaskInfoSection(
                    task = task,
                    typeLabel = typeLabel,
                    schema = schema,
                    hindi = hindi,
                    dateFormat = dateFormat
                )
            }

            // Section 8: Quick notes chips
            item {
                QuickNotesSection(
                    quickNotes = schema.quickNotes(),
                    hindi = hindi,
                    onAddNote = onAddNote
                )
            }

            // Section 9: Proof capture (conditional)
            if (stateSchema?.showProofCapture == true) {
                item {
                    ProofCaptureSection(hindi = hindi)
                }
            }

            // Section 10: Timeline
            if (task.timeline.isNotEmpty()) {
                item {
                    Text(
                        text = if (hindi) "\u0938\u092E\u092F\u0930\u0947\u0916\u093E" else "Timeline",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                item {
                    TimelineRenderer(entries = task.timeline, hindi = hindi)
                }
            }

            // Bottom spacing for action footer
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Section Composables ──────────────────────────────────────────────

@Composable
private fun StatusContactBar(
    dotColor: Color,
    stateLabel: String,
    stateColor: Color,
    assignedToName: String?,
    customerPhone: String?,
    techPhone: String?,
    onCallCustomer: () -> Unit,
    onCallTech: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // State label
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = stateColor
            )

            // Assigned person
            if (assignedToName != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "\u00B7 $assignedToName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Call buttons
            if (customerPhone != null) {
                IconButton(onClick = onCallCustomer, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call customer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (techPhone != null) {
                IconButton(onClick = onCallTech, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call technician",
                        tint = Color(0xFFFF8000),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpFromWiomSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    helpReasons: List<LabeledItem>,
    hindi: Boolean,
    onRequestHelp: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hindi) "Wiom \u0938\u0947 \u092E\u0926\u0926 \u091A\u093E\u0939\u093F\u090F" else "Need Help from Wiom",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    helpReasons.forEach { reason ->
                        OutlinedButton(
                            onClick = { onRequestHelp(reason.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (hindi) reason.labelHi else reason.label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerDetailsCard(
    task: TaskData,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Customer Details",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            task.customerName?.let { DetailRow("Name", it) }
            task.customerPhone?.let { DetailRow("Phone", it) }
            task.connectionId?.let { DetailRow("Connection ID", it) }
            task.customerArea?.let { DetailRow("Area", it) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SlotPickerSection(
    selectedSlot: String?,
    onSlotSelected: (String) -> Unit,
    hindi: Boolean
) {
    val slots = listOf(
        "TODAY" to (if (hindi) "\u0906\u091C" else "Today"),
        "TOMORROW" to (if (hindi) "\u0915\u0932" else "Tomorrow"),
        "DAY_AFTER" to (if (hindi) "\u092A\u0930\u0938\u094B\u0902" else "Day After")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (hindi) "\u0938\u092E\u092F \u091A\u0941\u0928\u0947\u0902" else "Select Slot",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                slots.forEach { (id, label) ->
                    val isSelected = selectedSlot == id
                    val shape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = shape
                            )
                            .clickable { onSlotSelected(id) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeclineReasonPicker(
    reasons: List<LabeledItem>,
    selectedReason: String?,
    onReasonSelected: (String) -> Unit,
    hindi: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (hindi) "\u0915\u093E\u0930\u0923 \u091A\u0941\u0928\u0947\u0902" else "Select Reason",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                reasons.forEach { reason ->
                    val isSelected = selectedReason == reason.id
                    AssistChip(
                        onClick = { onReasonSelected(reason.id) },
                        label = {
                            Text(
                                text = if (hindi) reason.labelHi else reason.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        },
                        modifier = Modifier.then(
                            if (isSelected) Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            ) else Modifier
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TechnicianPickerSection(
    technicians: List<TechnicianData>,
    selectedTechnician: String?,
    onTechnicianSelected: (String) -> Unit,
    hindi: Boolean
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedTech = technicians.find { it.id == selectedTechnician }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (hindi) "\u0924\u0915\u0928\u0940\u0936\u093F\u092F\u0928 \u091A\u0941\u0928\u0947\u0902" else "Assign Technician",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = selectedTech?.let { "${it.name} (${it.band})" }
                            ?: if (hindi) "\u0924\u0915\u0928\u0940\u0936\u093F\u092F\u0928 \u091A\u0941\u0928\u0947\u0902" else "Select technician",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    technicians
                        .filter { it.available }
                        .forEach { tech ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tech.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusBadge(
                                            label = tech.band,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            bgColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    }
                                },
                                onClick = {
                                    onTechnicianSelected(tech.id)
                                    dropdownExpanded = false
                                }
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun TaskInfoSection(
    task: TaskData,
    typeLabel: String,
    schema: SchemaResolver,
    hindi: Boolean,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (hindi) "\u0915\u093E\u0930\u094D\u092F \u0935\u093F\u0935\u0930\u0923" else "Task Details",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            DetailRow(if (hindi) "\u092A\u094D\u0930\u0915\u093E\u0930" else "Type", typeLabel)
            task.connectionId?.let { DetailRow("Connection ID", it) }
            task.netboxId?.let { DetailRow("NetBox ID", it) }
            task.customerArea?.let { DetailRow(if (hindi) "\u0915\u094D\u0937\u0947\u0924\u094D\u0930" else "Area", it) }
            DetailRow(
                if (hindi) "\u092C\u0928\u093E\u092F\u093E" else "Created",
                dateFormat.format(Date(task.createdAt))
            )
            task.dueAt?.let {
                DetailRow(
                    if (hindi) "\u0905\u0902\u0924\u093F\u092E \u0924\u093F\u0925\u093F" else "Deadline",
                    dateFormat.format(Date(it))
                )
            }
            if (task.retryCount > 0) {
                DetailRow(if (hindi) "\u092A\u0941\u0928\u0903 \u092A\u094D\u0930\u092F\u093E\u0938" else "Retries", task.retryCount.toString())
            }

            // Escalation flags
            if (task.escalationFlags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Divider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hindi) "\u090F\u0938\u094D\u0915\u0947\u0932\u0947\u0936\u0928" else "Escalation Flags",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFFF3B30)
                )
                Spacer(modifier = Modifier.height(4.dp))
                task.escalationFlags.forEach { flagId ->
                    val flag = schema.resolveEscalationFlag(flagId)
                    if (flag != null) {
                        val flagColor = parseColor(flag.color)
                        StatusBadge(
                            label = if (hindi) flag.labelHi else flag.label,
                            color = flagColor,
                            bgColor = flagColor.copy(alpha = 0.12f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

            // Blocked reason
            task.blockedReason?.let { reason ->
                Spacer(modifier = Modifier.height(4.dp))
                Divider()
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow(if (hindi) "\u0905\u0935\u0930\u0941\u0926\u094D\u0927 \u0915\u093E\u0930\u0923" else "Blocked", reason)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickNotesSection(
    quickNotes: List<LabeledItem>,
    hindi: Boolean,
    onAddNote: (String) -> Unit
) {
    if (quickNotes.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (hindi) "\u0924\u094D\u0935\u0930\u093F\u0924 \u0928\u094B\u091F\u094D\u0938" else "Quick Notes",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickNotes.forEach { note ->
                AssistChip(
                    onClick = { onAddNote(if (hindi) note.labelHi else note.label) },
                    label = {
                        Text(
                            text = if (hindi) note.labelHi else note.label,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ProofCaptureSection(hindi: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = "Capture proof",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hindi) "\u092A\u094D\u0930\u092E\u093E\u0923 \u0915\u0948\u092A\u094D\u091A\u0930 \u0915\u0930\u0947\u0902" else "Capture Proof",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
