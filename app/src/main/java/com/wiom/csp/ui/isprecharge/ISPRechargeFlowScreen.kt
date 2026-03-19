package com.wiom.csp.ui.isprecharge

import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.domain.model.ISPPortal
import com.wiom.csp.domain.model.RechargeCustomer
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.mock.SeedDataProvider
import kotlinx.coroutines.delay

/**
 * ISP Recharge flow — Phase 1 implementation.
 *
 * Flow steps:
 * 1. CUSTOMER_LIST — see all customers due for recharge + total earnings
 * 2. CHOOSE_METHOD — "Recharge with Wiom" vs "Continue with Portal Directly"
 * 3. SETUP_PORTAL — enter ISP portal credentials (Path A) or URL only (Path B)
 * 4. RECHARGE_CUSTOMER — customer details + WebView + Start Recharge
 * 5. RECHARGE_PROGRESS — automation running on portal (visual)
 * 6. RECHARGE_DONE — single customer done, next prompt
 * 7. SESSION_SUMMARY — all done, total earnings, Phase 2 pitch
 */
private enum class RechargeStep {
    CUSTOMER_LIST,
    CHOOSE_METHOD,
    SETUP_PORTAL,
    RECHARGE_CUSTOMER,
    RECHARGE_PROGRESS,
    RECHARGE_DONE,
    SESSION_SUMMARY
}

private val STEP_LABELS = mapOf(
    RechargeStep.CUSTOMER_LIST to "Customers",
    RechargeStep.CHOOSE_METHOD to "Choose Method",
    RechargeStep.SETUP_PORTAL to "Portal Setup",
    RechargeStep.RECHARGE_CUSTOMER to "Recharge",
    RechargeStep.CHOOSE_METHOD to "Method",
    RechargeStep.RECHARGE_PROGRESS to "Processing",
    RechargeStep.RECHARGE_DONE to "Done",
    RechargeStep.SESSION_SUMMARY to "Summary"
)

@Composable
fun ISPRechargeFlowScreen(
    task: TaskData,
    phase: Int = 1,
    onBack: () -> Unit,
    onComplete: (taskId: String) -> Unit
) {
    // ── Phase-based initial step ──
    val initialStep = RechargeStep.CUSTOMER_LIST // All phases start at customer list

    // ── Step navigation ──
    var currentStep by remember { mutableStateOf(initialStep) }

    // ── Data state ──
    val customers = remember { SeedDataProvider.buildSeedRechargeCustomers().toMutableStateList() }
    val savedPortals = remember { mutableStateListOf<ISPPortal>() } // Start empty — no saved portals

    // ── Method selection ──
    var rechargeWithWiom by remember { mutableStateOf(phase >= 2) } // Phase 2/3 auto-selects Wiom

    // ── Portal setup ──
    var portalUrl by remember { mutableStateOf("") }
    var portalUsername by remember { mutableStateOf("") }
    var portalPassword by remember { mutableStateOf("") }
    var portalName by remember { mutableStateOf("") }
    var portalVerified by remember { mutableStateOf(false) }
    var selectedPortalId by remember { mutableStateOf<String?>(null) }

    // ── Recharge progress ──
    var currentCustomerIndex by remember { mutableIntStateOf(0) }
    var rechargeStatus by remember { mutableStateOf("idle") } // idle, logging_in, searching, recharging, done, failed

    // ── WebView ref ──
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    // Current customer
    val currentCustomer = if (currentCustomerIndex < customers.size) customers[currentCustomerIndex] else null

    // Total earnings
    val completedCount = customers.count { it.status == "COMPLETED" }
    val totalEarned = completedCount * 300.0

    // Progress
    val totalSteps = 7
    val currentStepIndex = RechargeStep.entries.indexOf(currentStep)
    val progress = (currentStepIndex + 1).toFloat() / totalSteps

    // ── UI ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        RechargeTopBar(
            stepLabel = STEP_LABELS[currentStep] ?: "",
            stepNumber = currentStepIndex + 1,
            totalSteps = totalSteps,
            progress = progress,
            onBack = {
                when (currentStep) {
                    RechargeStep.CUSTOMER_LIST -> onBack()
                    RechargeStep.CHOOSE_METHOD -> currentStep = RechargeStep.CUSTOMER_LIST
                    RechargeStep.SETUP_PORTAL -> currentStep = RechargeStep.CHOOSE_METHOD
                    RechargeStep.RECHARGE_CUSTOMER -> currentStep = RechargeStep.SETUP_PORTAL
                    RechargeStep.RECHARGE_PROGRESS -> {} // Can't go back during recharge
                    RechargeStep.RECHARGE_DONE -> {} // Can't go back after completion
                    RechargeStep.SESSION_SUMMARY -> {} // Can't go back from summary
                }
            }
        )

        // Step content
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = if (currentStep == RechargeStep.RECHARGE_CUSTOMER) 80.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (currentStep) {
                RechargeStep.CUSTOMER_LIST -> CustomerListStep(
                    customers = customers,
                    phase = phase,
                    onProceed = {
                        when (phase) {
                            2 -> {
                                // Phase 2: batch — skip setup, go to batch progress
                                currentCustomerIndex = 0
                                rechargeStatus = "logging_in"
                                currentStep = RechargeStep.RECHARGE_PROGRESS
                            }
                            3 -> {
                                // Phase 3: auto — go straight to progress
                                currentCustomerIndex = 0
                                rechargeStatus = "logging_in"
                                currentStep = RechargeStep.RECHARGE_PROGRESS
                            }
                            else -> currentStep = RechargeStep.CHOOSE_METHOD
                        }
                    }
                )

                RechargeStep.CHOOSE_METHOD -> ChooseMethodStep(
                    onRechargeWithWiom = {
                        rechargeWithWiom = true
                        currentStep = RechargeStep.SETUP_PORTAL
                    },
                    onContinueWithPortal = {
                        rechargeWithWiom = false
                        currentStep = RechargeStep.SETUP_PORTAL
                    }
                )

                RechargeStep.SETUP_PORTAL -> SetupPortalStep(
                    rechargeWithWiom = rechargeWithWiom,
                    savedPortals = savedPortals,
                    selectedPortalId = selectedPortalId,
                    portalUrl = portalUrl,
                    portalUsername = portalUsername,
                    portalPassword = portalPassword,
                    portalName = portalName,
                    portalVerified = portalVerified,
                    onPortalUrlChange = { portalUrl = it },
                    onPortalUsernameChange = { portalUsername = it },
                    onPortalPasswordChange = { portalPassword = it },
                    onPortalNameChange = { portalName = it },
                    onSelectPortal = { portal ->
                        selectedPortalId = portal.id
                        portalUrl = portal.url
                        portalUsername = portal.username
                        portalPassword = portal.password
                        portalName = portal.name
                        portalVerified = portal.verified
                    },
                    onVerify = {
                        if (portalVerified) {
                            // "Add Another Portal" tapped — reset fields
                            portalVerified = false
                        } else {
                            // Auto-detect portal name from URL domain
                            val domain = portalUrl
                                .removePrefix("https://").removePrefix("http://")
                                .split("/").firstOrNull() ?: ""
                            val autoName = domain.split(".").firstOrNull()
                                ?.replaceFirstChar { it.uppercase() } ?: "ISP Portal"
                            portalName = "$autoName Portal"
                            portalVerified = true

                            // Save to portal list
                            savedPortals.add(
                                ISPPortal(
                                    id = "PORTAL-${savedPortals.size + 1}",
                                    name = portalName,
                                    url = portalUrl,
                                    username = portalUsername,
                                    password = portalPassword,
                                    verified = true
                                )
                            )
                        }
                    },
                    onProceed = {
                        currentCustomerIndex = 0
                        rechargeStatus = "idle"
                        currentStep = RechargeStep.RECHARGE_CUSTOMER
                    }
                )

                RechargeStep.RECHARGE_CUSTOMER -> if (currentCustomer != null) RechargeCustomerStep(
                    customer = currentCustomer,
                    customerIndex = currentCustomerIndex,
                    totalCustomers = customers.size,
                    portalUrl = portalUrl,
                    rechargeWithWiom = rechargeWithWiom,
                    webViewRef = webViewRef
                )

                RechargeStep.RECHARGE_PROGRESS -> if (currentCustomer != null) RechargeProgressStep(
                    customer = currentCustomer,
                    rechargeStatus = rechargeStatus,
                    portalUrl = portalUrl,
                    phase = phase,
                    customerIndex = currentCustomerIndex,
                    totalCustomers = customers.size,
                    webViewRef = webViewRef,
                    onStatusChange = { rechargeStatus = it },
                    onComplete = {
                        // Mark customer as completed
                        customers[currentCustomerIndex] = currentCustomer.copy(status = "COMPLETED")
                        if (phase >= 2 && currentCustomerIndex < customers.size - 1) {
                            // Batch/Auto: auto-advance to next customer
                            currentCustomerIndex++
                            rechargeStatus = "logging_in"
                            // Stay on RECHARGE_PROGRESS
                        } else if (phase >= 2) {
                            // All done in batch mode
                            currentStep = RechargeStep.SESSION_SUMMARY
                        } else {
                            rechargeStatus = "done"
                            currentStep = RechargeStep.RECHARGE_DONE
                        }
                    }
                )

                RechargeStep.RECHARGE_DONE -> if (currentCustomer != null) RechargeDoneStep(
                    customer = currentCustomer,
                    customerIndex = currentCustomerIndex,
                    totalCustomers = customers.size,
                    completedCount = completedCount,
                    onNextCustomer = {
                        currentCustomerIndex++
                        rechargeStatus = "idle"
                        currentStep = RechargeStep.RECHARGE_CUSTOMER
                    },
                    onFinish = {
                        currentStep = RechargeStep.SESSION_SUMMARY
                    }
                )

                RechargeStep.SESSION_SUMMARY -> SessionSummaryStep(
                    customers = customers,
                    totalEarned = totalEarned,
                    rechargeWithWiom = rechargeWithWiom,
                    onRechargeRemaining = {
                        // Find next pending customer and go back to recharge step
                        val nextPending = customers.indexOfFirst { it.status != "COMPLETED" }
                        if (nextPending >= 0) {
                            currentCustomerIndex = nextPending
                            rechargeStatus = "idle"
                            currentStep = RechargeStep.RECHARGE_CUSTOMER
                        }
                    },
                    onDone = { onComplete(task.taskId) }
                )
            }
        }

        // Floating CTA for RECHARGE_CUSTOMER step
        if (currentStep == RechargeStep.RECHARGE_CUSTOMER && currentCustomer != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1A1832))
                    .padding(16.dp)
            ) {
                PrimaryButton(
                    label = "Start Recharge for ${currentCustomer.name}",
                    onClick = {
                        rechargeStatus = "logging_in"
                        currentStep = RechargeStep.RECHARGE_PROGRESS
                    }
                )
            }
        }
        } // close Box
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Top Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun RechargeTopBar(
    stepLabel: String,
    stepNumber: Int,
    totalSteps: Int,
    progress: Float,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ISP Recharge",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Step $stepNumber of $totalSteps - $stepLabel",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            // Recharge icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00B894).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CurrencyRupee,
                    contentDescription = null,
                    tint = Color(0xFF00B894),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF00B894),
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Step 1: Customer List
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CustomerListStep(
    customers: List<RechargeCustomer>,
    phase: Int = 1,
    onProceed: () -> Unit
) {
    val totalShare = customers.sumOf { it.shareAmount }

    StepCard {
        StepTitle(
            when (phase) {
                2 -> "Batch Recharge — ${customers.size} Customers"
                3 -> "Auto Recharge — ${customers.size} Customers"
                else -> "Customers Due for Recharge"
            }
        )
        StepDescription(
            when (phase) {
                2 -> "All customers are pre-selected. Tap to recharge all at once automatically."
                3 -> "Wiom will recharge all customers automatically. No manual steps needed."
                else -> "These customers need ISP portal recharge to continue their internet service."
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Summary card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00B894).copy(alpha = 0.15f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${customers.size} Customers",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pending recharge",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "\u20B9${totalShare.toInt()}",
                    color = Color(0xFF00B894),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your share",
                    color = Color(0xFF00B894).copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Customer cards
        customers.forEachIndexed { index, customer ->
            CustomerCard(customer = customer, index = index + 1)
            if (index < customers.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryButton(
            label = when (phase) {
                2 -> "Start Batch Recharge"
                3 -> "Start Auto Recharge"
                else -> "Proceed to Recharge"
            },
            onClick = onProceed
        )
    }
}

@Composable
private fun CustomerCard(customer: RechargeCustomer, index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF00B894).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                color = Color(0xFF00B894),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = customer.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = customer.deviceId,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Text(
                    text = customer.speed,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Text(
                    text = "...${customer.phoneLast5}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
        Text(
            text = "\u20B9${customer.shareAmount.toInt()}",
            color = Color(0xFF00B894),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Step 2: Choose Method
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ChooseMethodStep(
    onRechargeWithWiom: () -> Unit,
    onContinueWithPortal: () -> Unit
) {
    StepCard {
        StepTitle("How would you like to recharge?")
        StepDescription("Choose how you want to complete the ISP portal recharges for your customers.")

        Spacer(modifier = Modifier.height(20.dp))

        // Option A: Recharge with Wiom (Recommended)
        MethodCard(
            title = "Recharge with Wiom",
            titleHi = "Wiom \u0915\u0947 \u0938\u093E\u0925 \u0930\u093F\u091A\u093E\u0930\u094D\u091C",
            description = "Save your ISP portal login once. We'll handle the rest \u2014 faster recharges, no repeated logins.",
            benefits = listOf(
                "One-time portal setup",
                "Automatic login every time",
                "Faster & smoother recharges",
                "No repeated password entry"
            ),
            isRecommended = true,
            accentColor = Color(0xFF00B894),
            onClick = onRechargeWithWiom
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option B: Continue with Portal
        MethodCard(
            title = "Continue with Portal Directly",
            titleHi = "\u092A\u094B\u0930\u094D\u091F\u0932 \u092A\u0930 \u0938\u0940\u0927\u0947 \u091C\u093E\u0930\u0940 \u0930\u0916\u0947\u0902",
            description = "Enter your portal credentials each session. Your login details won't be saved.",
            benefits = listOf(
                "No credentials saved",
                "Manual login each time",
                "Same recharge automation"
            ),
            isRecommended = false,
            accentColor = Color(0xFF636E72),
            onClick = onContinueWithPortal
        )
    }
}

@Composable
private fun MethodCard(
    title: String,
    titleHi: String,
    description: String,
    benefits: List<String>,
    isRecommended: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isRecommended) 2.dp else 1.dp,
                color = if (isRecommended) accentColor else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            )
            .background(
                if (isRecommended) accentColor.copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.03f)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        if (isRecommended) {
            Text(
                text = "RECOMMENDED",
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = titleHi,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        benefits.forEach { benefit ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Icon(
                    if (isRecommended) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = benefit,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Step 3: Portal Setup
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SetupPortalStep(
    rechargeWithWiom: Boolean,
    savedPortals: List<ISPPortal>,
    selectedPortalId: String?,
    portalUrl: String,
    portalUsername: String,
    portalPassword: String,
    portalName: String,
    portalVerified: Boolean,
    onPortalUrlChange: (String) -> Unit,
    onPortalUsernameChange: (String) -> Unit,
    onPortalPasswordChange: (String) -> Unit,
    onPortalNameChange: (String) -> Unit,
    onSelectPortal: (ISPPortal) -> Unit,
    onVerify: () -> Unit,
    onProceed: () -> Unit
) {
    StepCard {
        if (rechargeWithWiom) {
            StepTitle("Setup ISP Portal")
            StepDescription("Enter your ISP portal credentials. We'll save them securely so you don't have to log in every time.")
        } else {
            StepTitle("Enter Portal URL")
            StepDescription("Enter your ISP portal URL. You will need to log in manually each session.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // URL — always required
        InputField(
            label = "Portal URL",
            value = portalUrl,
            onValueChange = onPortalUrlChange,
            placeholder = "https://portal.example.com",
            keyboardType = KeyboardType.Uri
        )

        if (rechargeWithWiom) {
            Spacer(modifier = Modifier.height(10.dp))
            InputField(
                label = "Username",
                value = portalUsername,
                onValueChange = onPortalUsernameChange,
                placeholder = "Your ISP portal username"
            )
            Spacer(modifier = Modifier.height(10.dp))
            InputField(
                label = "Password",
                value = portalPassword,
                onValueChange = onPortalPasswordChange,
                placeholder = "Your ISP portal password",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!portalVerified) {
                SecondaryButton(
                    label = "Verify & Save Portal",
                    enabled = portalUrl.isNotBlank() && portalUsername.isNotBlank() && portalPassword.isNotBlank(),
                    onClick = onVerify
                )
            } else {
                // Verified state — show portal name (auto-detected from meta)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF00B894).copy(alpha = 0.15f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00B894),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Portal verified & saved",
                            color = Color(0xFF00B894),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (portalName.isNotBlank()) {
                            Text(
                                text = portalName,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Option to add another portal
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(
                    label = "+ Add Another Portal",
                    onClick = {
                        // Reset fields for adding another portal
                        onPortalUrlChange("")
                        onPortalUsernameChange("")
                        onPortalPasswordChange("")
                        onPortalNameChange("")
                        onVerify() // Toggle verified back — will be handled by parent resetting portalVerified
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val canProceed = if (rechargeWithWiom) {
            portalVerified && portalUrl.isNotBlank()
        } else {
            portalUrl.isNotBlank()
        }

        PrimaryButton(
            label = "Start Recharging",
            enabled = canProceed,
            onClick = onProceed
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Step 4: Recharge Customer (details + WebView)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun RechargeCustomerStep(
    customer: RechargeCustomer,
    customerIndex: Int,
    totalCustomers: Int,
    portalUrl: String,
    rechargeWithWiom: Boolean,
    webViewRef: MutableState<WebView?>
) {
    // Customer details card
    StepCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepTitle("Customer ${customerIndex + 1} of $totalCustomers")
            Text(
                text = "\u20B9${customer.shareAmount.toInt()}",
                color = Color(0xFF00B894),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Customer info grid
        InfoRow(label = "Name", value = customer.name)
        InfoRow(label = "Connection ID", value = customer.connectionId)
        InfoRow(label = "Device ID", value = customer.deviceId)
        InfoRow(label = "Speed", value = customer.speed)
        customer.username?.let { InfoRow(label = "Username", value = it) }
        InfoRow(label = "Phone (last 5)", value = "...${customer.phoneLast5}")
    }

    Spacer(modifier = Modifier.height(12.dp))

    // ISP Portal WebView
    StepCard {
        Text(
            text = "ISP Portal",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (rechargeWithWiom) "Auto-login enabled" else "Please login manually below",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        PortalWebView(
            url = portalUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            webViewRef = webViewRef,
            onPageLoaded = {
                // Portal loaded — ready for recharge
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Step 5: Recharge Progress (automation visual)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun RechargeProgressStep(
    customer: RechargeCustomer,
    rechargeStatus: String,
    portalUrl: String,
    phase: Int = 1,
    customerIndex: Int = 0,
    totalCustomers: Int = 1,
    webViewRef: MutableState<WebView?>,
    onStatusChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    // Simulate automation progress — faster for batch/auto
    val speedMultiplier = if (phase >= 2) 0.5 else 1.0
    LaunchedEffect(rechargeStatus, customerIndex) {
        when (rechargeStatus) {
            "logging_in" -> {
                delay((2000 * speedMultiplier).toLong())
                onStatusChange("searching")
            }
            "searching" -> {
                delay((2500 * speedMultiplier).toLong())
                onStatusChange("recharging")
            }
            "recharging" -> {
                delay((3000 * speedMultiplier).toLong())
                onComplete()
            }
        }
    }

    StepCard {
        // Show batch progress for Phase 2/3
        if (phase >= 2) {
            Text(
                text = "Customer ${customerIndex + 1} of $totalCustomers",
                color = Color(0xFF00B894),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (customerIndex + 1).toFloat() / totalCustomers },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF00B894),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        StepTitle(
            if (phase == 3) "Auto-Recharging ${customer.name}"
            else "Recharging ${customer.name}"
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Progress steps
        AutomationStep(
            label = "Logging into ISP Portal",
            labelHi = "ISP \u092A\u094B\u0930\u094D\u091F\u0932 \u092E\u0947\u0902 \u0932\u0949\u0917\u093F\u0928",
            status = when (rechargeStatus) {
                "logging_in" -> "in_progress"
                else -> "completed"
            }
        )
        AutomationStep(
            label = "Searching for ${customer.connectionId}",
            labelHi = "${customer.connectionId} \u0916\u094B\u091C \u0930\u0939\u0947 \u0939\u0948\u0902",
            status = when (rechargeStatus) {
                "logging_in" -> "pending"
                "searching" -> "in_progress"
                else -> "completed"
            }
        )
        AutomationStep(
            label = "Processing recharge",
            labelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u092A\u094D\u0930\u094B\u0938\u0947\u0938 \u0939\u094B \u0930\u0939\u093E \u0939\u0948",
            status = when (rechargeStatus) {
                "recharging" -> "in_progress"
                "done" -> "completed"
                else -> "pending"
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // WebView showing portal (builds trust)
        PortalWebView(
            url = portalUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            webViewRef = webViewRef
        )
    }
}

@Composable
private fun AutomationStep(
    label: String,
    labelHi: String,
    status: String // pending, in_progress, completed
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                "completed" -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00B894),
                    modifier = Modifier.size(20.dp)
                )
                "in_progress" -> CircularProgressIndicator(
                    color = Color(0xFF0984E3),
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                else -> Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = when (status) {
                    "completed" -> Color(0xFF00B894)
                    "in_progress" -> Color.White
                    else -> Color.White.copy(alpha = 0.4f)
                },
                fontSize = 14.sp,
                fontWeight = if (status == "in_progress") FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = labelHi,
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Step 6: Recharge Done (single customer)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun RechargeDoneStep(
    customer: RechargeCustomer,
    customerIndex: Int,
    totalCustomers: Int,
    completedCount: Int,
    onNextCustomer: () -> Unit,
    onFinish: () -> Unit
) {
    val isLast = customerIndex >= totalCustomers - 1

    StepCard {
        // Success icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00B894).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00B894),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Text(
            text = "Recharge Successful!",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u0938\u092B\u0932!",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Customer + earning
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF00B894).copy(alpha = 0.1f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = customer.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${customer.connectionId} \u2022 ${customer.deviceId}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+\u20B9${customer.shareAmount.toInt()}",
                    color = Color(0xFF00B894),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Earned",
                    color = Color(0xFF00B894).copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress counter
        Text(
            text = "$completedCount of $totalCustomers completed",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isLast) {
            PrimaryButton(
                label = "View Summary",
                onClick = onFinish
            )
        } else {
            PrimaryButton(
                label = "Next Customer \u2192",
                onClick = onNextCustomer
            )
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(
                label = "Finish & View Summary",
                onClick = onFinish
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Step 7: Session Summary
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SessionSummaryStep(
    customers: List<RechargeCustomer>,
    totalEarned: Double,
    rechargeWithWiom: Boolean,
    onRechargeRemaining: (() -> Unit)? = null,
    onDone: () -> Unit
) {
    val completed = customers.filter { it.status == "COMPLETED" }
    val pending = customers.filter { it.status != "COMPLETED" }

    StepCard {
        // Big success header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF00B894).copy(alpha = 0.12f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CurrencyRupee,
                    contentDescription = null,
                    tint = Color(0xFF00B894),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\u20B9${totalEarned.toInt()}",
                    color = Color(0xFF00B894),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Earned This Session",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Text(
                    text = "\u0907\u0938 \u0938\u0947\u0936\u0928 \u092E\u0947\u0902 \u0915\u0941\u0932 \u0915\u092E\u093E\u0908",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Completed list
        Text(
            text = "${completed.size} Recharged",
            color = Color(0xFF00B894),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        completed.forEach { customer ->
            SummaryCustomerRow(customer = customer, isCompleted = true)
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (pending.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${pending.size} Remaining",
                color = Color(0xFFFF8000),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            pending.forEach { customer ->
                SummaryCustomerRow(customer = customer, isCompleted = false)
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Primary: Recharge remaining customers
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                label = "Recharge Remaining (${pending.size})",
                onClick = { onRechargeRemaining?.invoke() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(
                label = "Back to Tasks",
                onClick = onDone
            )
        } else {
            // All done — Phase 2 pitch + back to tasks
            if (rechargeWithWiom) {
                Spacer(modifier = Modifier.height(20.dp))
                Phase2PitchCard()
            }

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryButton(
                label = "Back to Tasks",
                onClick = onDone
            )
        }
    }
}

@Composable
private fun SummaryCustomerRow(customer: RechargeCustomer, isCompleted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule,
            contentDescription = null,
            tint = if (isCompleted) Color(0xFF00B894) else Color(0xFFFF8000),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = customer.name,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (isCompleted) "+\u20B9${customer.shareAmount.toInt()}" else "Pending",
            color = if (isCompleted) Color(0xFF00B894) else Color(0xFFFF8000),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun Phase2PitchCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF6C5CE7).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .background(Color(0xFF6C5CE7).copy(alpha = 0.08f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = Color(0xFF6C5CE7),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Make it even faster!",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enable one-click batch recharge \u2014 recharge all customers at once, automatically. No manual steps needed.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Text(
            text = "\u090F\u0915 \u0915\u094D\u0932\u093F\u0915 \u092E\u0947\u0902 \u0938\u092C \u0915\u0938\u094D\u091F\u092E\u0930\u094D\u0938 \u0915\u093E \u0930\u093F\u091A\u093E\u0930\u094D\u091C \u0915\u0930\u0947\u0902 \u2014 \u0911\u091F\u094B\u092E\u0948\u091F\u093F\u0915!",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(
            label = "Enable Batch Recharge (Coming Soon)",
            enabled = false,
            onClick = {}
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Shared UI components
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun StepCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16213E))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StepDescription(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    Column {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(Color(0xFF00B894)),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.White.copy(alpha = 0.25f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) Color(0xFF00B894) else Color(0xFF00B894).copy(alpha = 0.3f)
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SecondaryButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (enabled) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
