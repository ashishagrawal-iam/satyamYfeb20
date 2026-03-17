package com.wiom.csp.ui.team

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.domain.model.TechnicianData
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.ui.common.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Team screen: shows the CSP's technician roster.
 *
 * List view: each card shows avatar (initials circle), technician ID,
 * and availability badge (green=Available, grey=Busy/Offline).
 * Per design audit: ID + availability only on the list card.
 *
 * Add Technician: form overlay with name + phone (10-digit Indian mobile
 * validation). Auto-assigns Band B.
 *
 * Tap technician: detail view with avatar, ID, phone, join date,
 * band label (from schema.technicianBands()), and availability.
 *
 * Empty state: "No technicians added" with add prompt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    technicians: List<TechnicianData>,
    schema: SchemaResolver,
    hindi: Boolean,
    onBack: () -> Unit,
    onAddTechnician: (String, String) -> Unit
) {
    var showAddForm by rememberSaveable { mutableStateOf(false) }
    var selectedTechnician by remember { mutableStateOf<TechnicianData?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (hindi) "टीम" else "Team",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (!showAddForm && selectedTechnician == null) {
                FloatingActionButton(
                    onClick = { showAddForm = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (hindi) "टेक्नीशियन जोड़ें" else "Add Technician"
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
            if (technicians.isEmpty() && !showAddForm) {
                // Empty state
                EmptyTeamState(
                    hindi = hindi,
                    onAdd = { showAddForm = true }
                )
            } else {
                // Technician list
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
                        items = technicians,
                        key = { it.id }
                    ) { tech ->
                        TechnicianCard(
                            technician = tech,
                            onClick = { selectedTechnician = tech }
                        )
                    }
                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // Add technician form overlay
            AnimatedVisibility(
                visible = showAddForm,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AddTechnicianOverlay(
                    hindi = hindi,
                    onDismiss = { showAddForm = false },
                    onSubmit = { name, phone ->
                        onAddTechnician(name, phone)
                        showAddForm = false
                    }
                )
            }

            // Technician detail overlay
            AnimatedVisibility(
                visible = selectedTechnician != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedTechnician?.let { tech ->
                    TechnicianDetailOverlay(
                        technician = tech,
                        schema = schema,
                        hindi = hindi,
                        onDismiss = { selectedTechnician = null }
                    )
                }
            }
        }
    }
}

// ── Technician Card ─────────────────────────────────────────────────────

@Composable
private fun TechnicianCard(
    technician: TechnicianData,
    onClick: () -> Unit
) {
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
            // Avatar with initials
            TechnicianAvatar(
                name = technician.name,
                size = 44
            )

            Spacer(modifier = Modifier.width(14.dp))

            // ID
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = technician.id,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = technician.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Availability badge
            val badgeColor = if (technician.available) Color(0xFF34C759) else Color(0xFF8E8E93)
            val badgeBg = if (technician.available) Color(0xFF34C759).copy(alpha = 0.12f)
            else Color(0xFF8E8E93).copy(alpha = 0.12f)
            val badgeLabel = if (technician.available) "Available" else "Offline"

            StatusBadge(
                label = badgeLabel,
                color = badgeColor,
                bgColor = badgeBg
            )
        }
    }
}

// ── Avatar ──────────────────────────────────────────────────────────────

@Composable
private fun TechnicianAvatar(
    name: String,
    size: Int = 44
) {
    val initials = remember(name) {
        name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size / 3).sp
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ── Empty State ─────────────────────────────────────────────────────────

@Composable
private fun EmptyTeamState(
    hindi: Boolean,
    onAdd: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (hindi) "कोई टेक्नीशियन नहीं जोड़ा गया" else "No technicians added",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hindi) "टेक्नीशियन जोड़ें" else "Add Technician"
                )
            }
        }
    }
}

// ── Add Technician Overlay ──────────────────────────────────────────────

@Composable
private fun AddTechnicianOverlay(
    hindi: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneError by rememberSaveable { mutableStateOf<String?>(null) }

    val indianMobileRegex = remember { Regex("^[6-9]\\d{9}$") }

    fun validate(): Boolean {
        var valid = true
        if (name.isBlank()) {
            nameError = if (hindi) "नाम आवश्यक है" else "Name is required"
            valid = false
        } else {
            nameError = null
        }
        if (!indianMobileRegex.matches(phone)) {
            phoneError = if (hindi) "10 अंकों का मोबाइल नंबर दर्ज करें" else "Enter valid 10-digit mobile number"
            valid = false
        } else {
            phoneError = null
        }
        return valid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hindi) "टेक्नीशियन जोड़ें" else "Add Technician",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (hindi) "बैंड B में ऑटो-असाइन होगा" else "Auto-assigned to Band B",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = {
                        Text(if (hindi) "नाम" else "Name")
                    },
                    isError = nameError != null,
                    supportingText = nameError?.let { error ->
                        { Text(error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        // Allow only digits, max 10
                        val filtered = input.filter { it.isDigit() }.take(10)
                        phone = filtered
                        phoneError = null
                    },
                    label = {
                        Text(if (hindi) "फ़ोन नंबर" else "Phone Number")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    prefix = { Text("+91 ") },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { error ->
                        { Text(error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
                Button(
                    onClick = {
                        if (validate()) {
                            onSubmit(name.trim(), phone.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (hindi) "जोड़ें" else "Add",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// ── Technician Detail Overlay ───────────────────────────────────────────

@Composable
private fun TechnicianDetailOverlay(
    technician: TechnicianData,
    schema: SchemaResolver,
    hindi: Boolean,
    onDismiss: () -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }
    val bandLabel = remember(technician.band, schema, hindi) {
        val bands = schema.technicianBands()
        val band = bands.find { it.id == technician.band }
        if (hindi) band?.labelHi ?: band?.label ?: technician.band
        else band?.label ?: technician.band
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                // Large avatar
                TechnicianAvatar(
                    name = technician.name,
                    size = 72
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                Text(
                    text = technician.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ID
                Text(
                    text = technician.id,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Detail rows
                DetailRow(
                    label = if (hindi) "फ़ोन" else "Phone",
                    value = "+91 ${technician.phone}"
                )
                DetailRow(
                    label = if (hindi) "शामिल होने की तारीख" else "Join Date",
                    value = dateFormatter.format(Date(technician.joinDate))
                )
                DetailRow(
                    label = if (hindi) "बैंड" else "Band",
                    value = bandLabel
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Availability
                val badgeColor = if (technician.available) Color(0xFF34C759) else Color(0xFF8E8E93)
                val badgeBg = if (technician.available) Color(0xFF34C759).copy(alpha = 0.12f)
                else Color(0xFF8E8E93).copy(alpha = 0.12f)
                val badgeLabel = if (technician.available) {
                    if (hindi) "उपलब्ध" else "Available"
                } else {
                    if (hindi) "ऑफलाइन" else "Offline"
                }

                StatusBadge(
                    label = badgeLabel,
                    color = badgeColor,
                    bgColor = badgeBg
                )
            }
        }
    }
}

// ── Detail Row ──────────────────────────────────────────────────────────

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
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
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
