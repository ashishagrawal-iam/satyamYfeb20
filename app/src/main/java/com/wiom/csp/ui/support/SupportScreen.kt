package com.wiom.csp.ui.support

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wiom.csp.domain.model.CaseMessage
import com.wiom.csp.domain.model.SupportCaseData
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.ui.common.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Support screen: manages support cases for the CSP partner.
 *
 * Flow steps: list | create_case | create_receipt | case_detail
 *
 * List view: cases sorted by date (newest first), each showing subject +
 * status badge (color from schema.resolveSupportStatus). Empty: "No cases".
 *
 * Create case: subject + description fields (both required), optional
 * linked task ID. Submit -> receipt with case ID.
 *
 * Case detail: message thread (CSP messages brand-colored, others grey),
 * reply input with send button. CLOSED cases: reply disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    cases: List<SupportCaseData>,
    schema: SchemaResolver,
    hindi: Boolean,
    onBack: () -> Unit,
    onCreateCase: (String, String, String?) -> Unit,
    onReply: (String, String) -> Unit
) {
    // Flow step: list, create_case, create_receipt, case_detail
    var flowStep by rememberSaveable { mutableStateOf("list") }
    var selectedCaseId by rememberSaveable { mutableStateOf<String?>(null) }
    var createdCaseId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedCase = remember(selectedCaseId, cases) {
        cases.find { it.id == selectedCaseId }
    }

    val sortedCases = remember(cases) {
        cases.sortedByDescending { it.createdAt }
    }

    val screenTitle = when (flowStep) {
        "create_case" -> if (hindi) "नया केस" else "New Case"
        "create_receipt" -> if (hindi) "केस बनाया गया" else "Case Created"
        "case_detail" -> selectedCase?.subject ?: (if (hindi) "केस विवरण" else "Case Detail")
        else -> if (hindi) "सहायता" else "Support"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (flowStep) {
                            "list" -> onBack()
                            "create_receipt" -> {
                                flowStep = "list"
                                createdCaseId = null
                            }
                            else -> flowStep = "list"
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (flowStep == "list") {
                FloatingActionButton(
                    onClick = { flowStep = "create_case" },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (hindi) "नया केस" else "New Case"
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (flowStep) {
                "list" -> {
                    CaseListView(
                        cases = sortedCases,
                        schema = schema,
                        hindi = hindi,
                        onCaseClick = { caseId ->
                            selectedCaseId = caseId
                            flowStep = "case_detail"
                        }
                    )
                }
                "create_case" -> {
                    CreateCaseView(
                        hindi = hindi,
                        onSubmit = { subject, description, linkedTaskId ->
                            onCreateCase(subject, description, linkedTaskId)
                            // Show receipt with a placeholder ID;
                            // in real flow the ViewModel would provide the created case ID.
                            createdCaseId = "Pending..."
                            flowStep = "create_receipt"
                        }
                    )
                }
                "create_receipt" -> {
                    CaseReceiptView(
                        caseId = createdCaseId ?: "",
                        hindi = hindi,
                        onDone = {
                            flowStep = "list"
                            createdCaseId = null
                        }
                    )
                }
                "case_detail" -> {
                    selectedCase?.let { caseData ->
                        CaseDetailView(
                            caseData = caseData,
                            schema = schema,
                            hindi = hindi,
                            onReply = { text ->
                                onReply(caseData.id, text)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Case List View ──────────────────────────────────────────────────────

@Composable
private fun CaseListView(
    cases: List<SupportCaseData>,
    schema: SchemaResolver,
    hindi: Boolean,
    onCaseClick: (String) -> Unit
) {
    if (cases.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hindi) "कोई केस नहीं" else "No cases",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val dateFormatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "top_spacer") {
            Spacer(modifier = Modifier.height(4.dp))
        }
        items(
            items = cases,
            key = { it.id }
        ) { caseData ->
            CaseCard(
                caseData = caseData,
                schema = schema,
                dateFormatter = dateFormatter,
                onClick = { onCaseClick(caseData.id) }
            )
        }
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CaseCard(
    caseData: SupportCaseData,
    schema: SchemaResolver,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    val statusDisplay = remember(caseData.status, schema) {
        schema.resolveSupportStatus(caseData.status)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = caseData.subject,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormatter.format(Date(caseData.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (statusDisplay != null) {
                StatusBadge(
                    label = statusDisplay.label,
                    color = parseColor(statusDisplay.color),
                    bgColor = parseColor(statusDisplay.bgColor)
                )
            } else {
                StatusBadge(
                    label = caseData.status,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    bgColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

// ── Create Case View ────────────────────────────────────────────────────

@Composable
private fun CreateCaseView(
    hindi: Boolean,
    onSubmit: (String, String, String?) -> Unit
) {
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var linkedTaskId by rememberSaveable { mutableStateOf("") }
    var subjectError by rememberSaveable { mutableStateOf(false) }
    var descriptionError by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = subject,
            onValueChange = {
                subject = it
                subjectError = false
            },
            label = { Text(if (hindi) "विषय" else "Subject") },
            isError = subjectError,
            supportingText = if (subjectError) {
                { Text(if (hindi) "विषय आवश्यक है" else "Subject is required") }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                descriptionError = false
            },
            label = { Text(if (hindi) "विवरण" else "Description") },
            isError = descriptionError,
            supportingText = if (descriptionError) {
                { Text(if (hindi) "विवरण आवश्यक है" else "Description is required") }
            } else null,
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = linkedTaskId,
            onValueChange = { linkedTaskId = it },
            label = { Text(if (hindi) "जुड़ा टास्क ID (वैकल्पिक)" else "Linked Task ID (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                var valid = true
                if (subject.isBlank()) {
                    subjectError = true
                    valid = false
                }
                if (description.isBlank()) {
                    descriptionError = true
                    valid = false
                }
                if (valid) {
                    onSubmit(
                        subject.trim(),
                        description.trim(),
                        linkedTaskId.trim().ifBlank { null }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (hindi) "केस बनाएं" else "Create Case",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ── Case Receipt View ───────────────────────────────────────────────────

@Composable
private fun CaseReceiptView(
    caseId: String,
    hindi: Boolean,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (hindi) "केस बनाया गया!" else "Case Created!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (hindi) "केस ID:" else "Case ID:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = caseId,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (hindi) "ठीक है" else "Done",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// ── Case Detail View ────────────────────────────────────────────────────

@Composable
private fun CaseDetailView(
    caseData: SupportCaseData,
    schema: SchemaResolver,
    hindi: Boolean,
    onReply: (String) -> Unit
) {
    val isClosed = caseData.status.equals("CLOSED", ignoreCase = true)
    var replyText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val statusDisplay = remember(caseData.status, schema) {
        schema.resolveSupportStatus(caseData.status)
    }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(caseData.messages.size) {
        if (caseData.messages.isNotEmpty()) {
            listState.animateScrollToItem(caseData.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Status badge row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = caseData.id,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (statusDisplay != null) {
                StatusBadge(
                    label = statusDisplay.label,
                    color = parseColor(statusDisplay.color),
                    bgColor = parseColor(statusDisplay.bgColor)
                )
            }
        }

        // Message thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "thread_top_spacer") {
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(
                items = caseData.messages,
                key = { it.id }
            ) { message ->
                MessageBubble(message = message)
            }
            item(key = "thread_bottom_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Reply input
        if (isClosed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hindi) "यह केस बंद है" else "This case is closed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = {
                        Text(if (hindi) "उत्तर लिखें..." else "Type a reply...")
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onReply(replyText.trim())
                            replyText = ""
                        }
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (hindi) "भेजें" else "Send",
                        tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Message Bubble ──────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: CaseMessage) {
    val isCsp = message.senderType.equals("CSP", ignoreCase = true) ||
            message.senderType.equals("PARTNER", ignoreCase = true)

    val dateFormatter = remember {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCsp) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isCsp) 12.dp else 4.dp,
                        bottomEnd = if (isCsp) 4.dp else 12.dp
                    )
                )
                .background(
                    if (isCsp) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormatter.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Color parser ────────────────────────────────────────────────────────

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF888888)
    }
}
