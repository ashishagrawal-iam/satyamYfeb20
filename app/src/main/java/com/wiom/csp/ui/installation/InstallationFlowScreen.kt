package com.wiom.csp.ui.installation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.domain.model.TaskData
import kotlinx.coroutines.delay
import java.io.File

/**
 * Installation flow steps — Phase 4 of the booking-to-installation workflow.
 * Triggered when a technician/CSP starts installation on an INSTALL task.
 */
private enum class InstallStep {
    REACHED_CUSTOMER,
    TAKE_SELFIE,
    AADHAAR_PHOTOS,
    CUSTOMER_PAYMENT,
    START_ROUTER,
    PROTOCOL_SELECTION,
    NETBOX_VLAN,
    WIFI_CONNECT,
    INSTALLATION_PHOTOS,
    OPTICAL_POWER,
    SPEED_TEST,
    HAPPY_CODE,
    COMPLETE
}

private val STEP_LABELS = mapOf(
    InstallStep.REACHED_CUSTOMER to "Reached Customer",
    InstallStep.TAKE_SELFIE to "Selfie Verification",
    InstallStep.AADHAAR_PHOTOS to "Aadhaar Photos",
    InstallStep.CUSTOMER_PAYMENT to "Customer Payment",
    InstallStep.START_ROUTER to "Start Router",
    InstallStep.PROTOCOL_SELECTION to "Protocol Selection",
    InstallStep.NETBOX_VLAN to "NetBox & VLAN Config",
    InstallStep.WIFI_CONNECT to "WiFi Connect",
    InstallStep.INSTALLATION_PHOTOS to "Installation Photos",
    InstallStep.OPTICAL_POWER to "Optical Power Check",
    InstallStep.SPEED_TEST to "Speed Test",
    InstallStep.HAPPY_CODE to "Happy Code",
    InstallStep.COMPLETE to "Complete"
)

@Composable
fun InstallationFlowScreen(
    task: TaskData,
    onBack: () -> Unit,
    onComplete: (taskId: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(InstallStep.REACHED_CUSTOMER) }
    val steps = InstallStep.entries
    val currentIndex = steps.indexOf(currentStep)
    val progress = (currentIndex.toFloat() / (steps.size - 1).toFloat())

    // Shared state across steps
    var selfieUri by remember { mutableStateOf<Uri?>(null) }
    var aadhaarFrontUri by remember { mutableStateOf<Uri?>(null) }
    var aadhaarBackUri by remember { mutableStateOf<Uri?>(null) }
    var paymentConfirmed by remember { mutableStateOf(false) }
    var selectedProtocol by remember { mutableStateOf("") }
    var pppoeUsername by remember { mutableStateOf("") }
    var pppoePassword by remember { mutableStateOf("") }
    var pppoeServiceName by remember { mutableStateOf("") }
    var staticIp by remember { mutableStateOf("") }
    var staticGateway by remember { mutableStateOf("") }
    var staticNetwork by remember { mutableStateOf("") }
    var staticDns1 by remember { mutableStateOf("") }
    var staticDns2 by remember { mutableStateOf("") }
    var netboxId by remember { mutableStateOf("") }
    var vlanMode by remember { mutableStateOf("TAG") }
    var vlanId by remember { mutableStateOf("") }
    var wifiConnected by remember { mutableStateOf(false) }
    var devicePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var wiringPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var plugPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var opticalPower by remember { mutableStateOf("") }
    var speedDownload by remember { mutableStateOf("") }
    var speedUpload by remember { mutableStateOf("") }
    var happyCode by remember { mutableStateOf("") }

    fun goNext() {
        val nextIndex = currentIndex + 1
        if (nextIndex < steps.size) {
            currentStep = steps[nextIndex]
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back + step indicator
            InstallationTopBar(
                stepLabel = STEP_LABELS[currentStep] ?: "",
                stepNumber = currentIndex + 1,
                totalSteps = steps.size,
                progress = progress,
                onBack = {
                    if (currentIndex > 0) {
                        currentStep = steps[currentIndex - 1]
                    } else {
                        onBack()
                    }
                }
            )

            // Step content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                when (currentStep) {
                    InstallStep.REACHED_CUSTOMER -> ReachedCustomerStep(
                        task = task,
                        onConfirm = { goNext() }
                    )
                    InstallStep.TAKE_SELFIE -> TakeSelfieStep(
                        capturedUri = selfieUri,
                        onCapture = { selfieUri = it },
                        onNext = { goNext() }
                    )
                    InstallStep.AADHAAR_PHOTOS -> AadhaarPhotosStep(
                        frontUri = aadhaarFrontUri,
                        backUri = aadhaarBackUri,
                        onCaptureFront = { aadhaarFrontUri = it },
                        onCaptureBack = { aadhaarBackUri = it },
                        onNext = { goNext() }
                    )
                    InstallStep.CUSTOMER_PAYMENT -> CustomerPaymentStep(
                        confirmed = paymentConfirmed,
                        onConfirm = { paymentConfirmed = true; goNext() }
                    )
                    InstallStep.START_ROUTER -> StartRouterStep(
                        onNext = { goNext() }
                    )
                    InstallStep.PROTOCOL_SELECTION -> ProtocolSelectionStep(
                        selectedProtocol = selectedProtocol,
                        onSelectProtocol = { selectedProtocol = it },
                        pppoeUsername = pppoeUsername,
                        onPppoeUsername = { pppoeUsername = it },
                        pppoePassword = pppoePassword,
                        onPppoePassword = { pppoePassword = it },
                        pppoeServiceName = pppoeServiceName,
                        onPppoeServiceName = { pppoeServiceName = it },
                        staticIp = staticIp,
                        onStaticIp = { staticIp = it },
                        staticGateway = staticGateway,
                        onStaticGateway = { staticGateway = it },
                        staticNetwork = staticNetwork,
                        onStaticNetwork = { staticNetwork = it },
                        staticDns1 = staticDns1,
                        onStaticDns1 = { staticDns1 = it },
                        staticDns2 = staticDns2,
                        onStaticDns2 = { staticDns2 = it },
                        onNext = { goNext() }
                    )
                    InstallStep.NETBOX_VLAN -> NetBoxVlanStep(
                        netboxId = netboxId,
                        onNetboxId = { netboxId = it },
                        vlanMode = vlanMode,
                        onVlanMode = { vlanMode = it },
                        vlanId = vlanId,
                        onVlanId = { vlanId = it },
                        onNext = { goNext() }
                    )
                    InstallStep.WIFI_CONNECT -> WifiConnectStep(
                        connected = wifiConnected,
                        onConnect = { wifiConnected = true },
                        onNext = { goNext() }
                    )
                    InstallStep.INSTALLATION_PHOTOS -> InstallationPhotosStep(
                        deviceUri = devicePhotoUri,
                        wiringUri = wiringPhotoUri,
                        plugUri = plugPhotoUri,
                        onCaptureDevice = { devicePhotoUri = it },
                        onCaptureWiring = { wiringPhotoUri = it },
                        onCapturePlug = { plugPhotoUri = it },
                        onNext = { goNext() }
                    )
                    InstallStep.OPTICAL_POWER -> OpticalPowerStep(
                        reading = opticalPower,
                        onReading = { opticalPower = it },
                        onNext = { goNext() }
                    )
                    InstallStep.SPEED_TEST -> SpeedTestStep(
                        download = speedDownload,
                        upload = speedUpload,
                        onDownload = { speedDownload = it },
                        onUpload = { speedUpload = it },
                        onNext = { goNext() }
                    )
                    InstallStep.HAPPY_CODE -> HappyCodeStep(
                        code = happyCode,
                        onCode = { happyCode = it },
                        onNext = { goNext() }
                    )
                    InstallStep.COMPLETE -> InstallationCompleteStep(
                        task = task,
                        onFinish = { onComplete(task.taskId) }
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// =====================================================================
// Top Bar
// =====================================================================

@Composable
private fun InstallationTopBar(
    stepLabel: String,
    stepNumber: Int,
    totalSteps: Int,
    progress: Float,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u2190",
                modifier = Modifier
                    .clickable { onBack() }
                    .semantics { contentDescription = "Go back" }
                    .padding(end = 12.dp),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stepLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Step $stepNumber of $totalSteps",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// =====================================================================
// Shared UI helpers
// =====================================================================

@Composable
private fun StepCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(20.dp),
        content = content
    )
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun StepDescription(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp
    )
}

@Composable
private fun PrimaryButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val disabledBg = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) MaterialTheme.colorScheme.primary else disabledBg)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecondaryButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipBg = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else chipBg)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PhotoCaptureButton(
    label: String,
    capturedUri: Uri?,
    onCapture: (Uri) -> Unit
) {
    val context = LocalContext.current
    val positiveColor = Color(0xFF34C759)

    var tempUri by remember { mutableStateOf<Uri?>(null) }

    fun getOrCreateUri(): Uri {
        val existing = tempUri
        if (existing != null) return existing
        val dir = File(context.cacheDir, "install_photos").apply { mkdirs() }
        val file = File(dir, "install_${label.replace(" ", "_")}_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        tempUri = uri
        return uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) tempUri?.let { onCapture(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = getOrCreateUri()
            cameraLauncher.launch(uri)
        }
    }

    val launchCamera: () -> Unit = {
        val uri = getOrCreateUri()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column {
        if (capturedUri != null) {
            // Replaced WiomAsyncImage with a placeholder showing the URI filename
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = capturedUri.lastPathSegment ?: "Photo captured",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    if (capturedUri != null) positiveColor else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(8.dp)
                )
                .clickable { launchCamera() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (capturedUri != null) "\u2713" else "\uD83D\uDCF7",
                    fontSize = 16.sp,
                    color = if (capturedUri != null) positiveColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (capturedUri != null) "$label \u2713" else "Capture $label",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (capturedUri != null) positiveColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// =====================================================================
// Step 1: Reached Customer
// =====================================================================

@Composable
private fun ReachedCustomerStep(
    task: TaskData,
    onConfirm: () -> Unit
) {
    val contextId = task.connectionId ?: task.netboxId ?: "--"
    val area = task.customerArea ?: "--"

    StepCard {
        StepTitle("Reached Customer Location")
        StepDescription("Confirm that you have arrived at the customer's premises and are ready to begin the installation.")

        Spacer(Modifier.height(20.dp))

        // Customer info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoRow("Connection ID", contextId)
            InfoRow("Area", area)
            InfoRow("Task", task.taskId)
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton("I Have Reached the Customer") { onConfirm() }
    }
}

// =====================================================================
// Step 2: Take Selfie
// =====================================================================

@Composable
private fun TakeSelfieStep(
    capturedUri: Uri?,
    onCapture: (Uri) -> Unit,
    onNext: () -> Unit
) {
    StepCard {
        StepTitle("Selfie Verification")
        StepDescription("Take a selfie at the customer's location for verification purposes. Make sure your face is clearly visible.")

        Spacer(Modifier.height(20.dp))

        PhotoCaptureButton(
            label = "Selfie",
            capturedUri = capturedUri,
            onCapture = onCapture
        )

        Spacer(Modifier.height(20.dp))

        PrimaryButton("Continue", enabled = capturedUri != null) { onNext() }
    }
}

// =====================================================================
// Step 3: Aadhaar Photos
// =====================================================================

@Composable
private fun AadhaarPhotosStep(
    frontUri: Uri?,
    backUri: Uri?,
    onCaptureFront: (Uri) -> Unit,
    onCaptureBack: (Uri) -> Unit,
    onNext: () -> Unit
) {
    StepCard {
        StepTitle("Aadhaar Card Photos")
        StepDescription("Capture clear photos of the customer's Aadhaar card — both front and back sides.")

        Spacer(Modifier.height(20.dp))

        PhotoCaptureButton(
            label = "Aadhaar Front",
            capturedUri = frontUri,
            onCapture = onCaptureFront
        )

        Spacer(Modifier.height(12.dp))

        PhotoCaptureButton(
            label = "Aadhaar Back",
            capturedUri = backUri,
            onCapture = onCaptureBack
        )

        Spacer(Modifier.height(20.dp))

        PrimaryButton("Continue", enabled = frontUri != null && backUri != null) { onNext() }
    }
}

// =====================================================================
// Step 4: Customer Payment
// =====================================================================

@Composable
private fun CustomerPaymentStep(
    confirmed: Boolean,
    onConfirm: () -> Unit
) {
    StepCard {
        StepTitle("Customer Payment")
        StepDescription("Collect payment from the customer or confirm payment has been received via wallet/external method.")

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "PAYMENT OPTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Text(
                "\u2022 Cash collection\n\u2022 UPI / Online payment\n\u2022 Wallet deduction (if pre-paid)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton("Payment Confirmed") { onConfirm() }
    }
}

// =====================================================================
// Step 5: Start Router
// =====================================================================

@Composable
private fun StartRouterStep(
    onNext: () -> Unit
) {
    StepCard {
        StepTitle("Start Router")
        StepDescription("Power on the router and wait for it to boot up completely. The ISP account setup will be configured in the next steps.")

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "CHECKLIST",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Text("1. Connect power cable to the router", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("2. Wait for all LED indicators to stabilize", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("3. Ensure WAN port is connected via fiber/ethernet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("4. Confirm LAN port connectivity (if wired)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton("Router is Ready") { onNext() }
    }
}

// =====================================================================
// Step 6: Protocol Selection (PPPoE / Static IP / DHCP)
// =====================================================================

@Composable
private fun ProtocolSelectionStep(
    selectedProtocol: String,
    onSelectProtocol: (String) -> Unit,
    pppoeUsername: String,
    onPppoeUsername: (String) -> Unit,
    pppoePassword: String,
    onPppoePassword: (String) -> Unit,
    pppoeServiceName: String,
    onPppoeServiceName: (String) -> Unit,
    staticIp: String,
    onStaticIp: (String) -> Unit,
    staticGateway: String,
    onStaticGateway: (String) -> Unit,
    staticNetwork: String,
    onStaticNetwork: (String) -> Unit,
    staticDns1: String,
    onStaticDns1: (String) -> Unit,
    staticDns2: String,
    onStaticDns2: (String) -> Unit,
    onNext: () -> Unit
) {
    val positiveColor = Color(0xFF34C759)

    StepCard {
        StepTitle("Protocol Selection")
        StepDescription("Select the ISP connection protocol and enter the required configuration details.")

        Spacer(Modifier.height(20.dp))

        // Protocol chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableChip("PPPoE", selectedProtocol == "PPPoE") { onSelectProtocol("PPPoE") }
            SelectableChip("Static IP", selectedProtocol == "Static IP") { onSelectProtocol("Static IP") }
            SelectableChip("DHCP", selectedProtocol == "DHCP") { onSelectProtocol("DHCP") }
        }

        Spacer(Modifier.height(20.dp))

        // Dynamic fields based on protocol
        when (selectedProtocol) {
            "PPPoE" -> {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InputField("Username", pppoeUsername, onPppoeUsername)
                    InputField("Password", pppoePassword, onPppoePassword, isPassword = true)
                    InputField("Service Name (optional)", pppoeServiceName, onPppoeServiceName)
                }
            }
            "Static IP" -> {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InputField("IP Address", staticIp, onStaticIp)
                    InputField("Gateway", staticGateway, onStaticGateway)
                    InputField("Network / Subnet", staticNetwork, onStaticNetwork)
                    InputField("Primary DNS", staticDns1, onStaticDns1)
                    InputField("Secondary DNS", staticDns2, onStaticDns2)
                }
            }
            "DHCP" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(positiveColor.copy(alpha = 0.1f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "DHCP CONFIGURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = positiveColor,
                        letterSpacing = 0.5.sp
                    )
                    InfoRow("Interface", "wan")
                    InfoRow("Protocol", "dhcp")
                    InfoRow("Device", "eth0.2")
                    InfoRow("Metric", "10")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Verify these settings on the router admin panel.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        val canProceed = when (selectedProtocol) {
            "PPPoE" -> pppoeUsername.isNotBlank() && pppoePassword.isNotBlank()
            "Static IP" -> staticIp.isNotBlank() && staticGateway.isNotBlank() && staticNetwork.isNotBlank() && staticDns1.isNotBlank()
            "DHCP" -> true
            else -> false
        }

        PrimaryButton("Continue", enabled = canProceed) { onNext() }
    }
}

// =====================================================================
// Step 7: NetBox ID + VLAN Config
// =====================================================================

@Composable
private fun NetBoxVlanStep(
    netboxId: String,
    onNetboxId: (String) -> Unit,
    vlanMode: String,
    onVlanMode: (String) -> Unit,
    vlanId: String,
    onVlanId: (String) -> Unit,
    onNext: () -> Unit
) {
    val negativeColor = Color(0xFFFF3B30)

    StepCard {
        StepTitle("NetBox & VLAN Configuration")
        StepDescription("Enter the NetBox device ID and configure the VLAN settings.")

        Spacer(Modifier.height(20.dp))

        InputField("NetBox ID", netboxId, onNetboxId)

        Spacer(Modifier.height(20.dp))

        Text(
            "VLAN MODE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableChip("TAG", vlanMode == "TAG") { onVlanMode("TAG") }
            SelectableChip("TRANSPARENT", vlanMode == "TRANSPARENT") { onVlanMode("TRANSPARENT") }
            SelectableChip("UNTAG", vlanMode == "UNTAG") { onVlanMode("UNTAG") }
        }

        if (vlanMode == "TAG") {
            Spacer(Modifier.height(14.dp))
            InputField(
                label = "VLAN ID (128\u20131492)",
                value = vlanId,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    onVlanId(filtered)
                },
                keyboardType = KeyboardType.Number
            )

            val vlanNum = vlanId.toIntOrNull()
            if (vlanId.isNotEmpty() && (vlanNum == null || vlanNum < 128 || vlanNum > 1492)) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "VLAN ID must be between 128 and 1492",
                    fontSize = 11.sp,
                    color = negativeColor
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        val vlanValid = when (vlanMode) {
            "TAG" -> {
                val num = vlanId.toIntOrNull()
                num != null && num in 128..1492
            }
            else -> true
        }

        PrimaryButton(
            "Continue",
            enabled = netboxId.isNotBlank() && vlanValid
        ) { onNext() }
    }
}

// =====================================================================
// Step 8: WiFi Connect
// =====================================================================

@Composable
private fun WifiConnectStep(
    connected: Boolean,
    onConnect: () -> Unit,
    onNext: () -> Unit
) {
    val positiveColor = Color(0xFF34C759)

    StepCard {
        StepTitle("WiFi Connection")
        StepDescription("Connect to the customer's WiFi network to verify the router is broadcasting correctly.")

        Spacer(Modifier.height(20.dp))

        // WiFi network card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (connected) positiveColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    if (connected) positiveColor else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "\u221E wiom net",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (connected) positiveColor else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (connected) "Connected \u2713" else "Tap below to confirm WiFi connection",
                fontSize = 13.sp,
                color = if (connected) positiveColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        if (!connected) {
            PrimaryButton("Confirm WiFi Connected") { onConnect() }
        } else {
            PrimaryButton("Continue") { onNext() }
        }
    }
}

// =====================================================================
// Step 9: Installation Photos
// =====================================================================

@Composable
private fun InstallationPhotosStep(
    deviceUri: Uri?,
    wiringUri: Uri?,
    plugUri: Uri?,
    onCaptureDevice: (Uri) -> Unit,
    onCaptureWiring: (Uri) -> Unit,
    onCapturePlug: (Uri) -> Unit,
    onNext: () -> Unit
) {
    StepCard {
        StepTitle("Installation Photos")
        StepDescription("Capture photos of the installed equipment for documentation.")

        Spacer(Modifier.height(20.dp))

        PhotoCaptureButton(label = "Device Setup", capturedUri = deviceUri, onCapture = onCaptureDevice)
        Spacer(Modifier.height(12.dp))
        PhotoCaptureButton(label = "Wiring", capturedUri = wiringUri, onCapture = onCaptureWiring)
        Spacer(Modifier.height(12.dp))
        PhotoCaptureButton(label = "Power Plug", capturedUri = plugUri, onCapture = onCapturePlug)

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            "Continue",
            enabled = deviceUri != null && wiringUri != null && plugUri != null
        ) { onNext() }
    }
}

// =====================================================================
// Step 10: Optical Power Check
// =====================================================================

@Composable
private fun OpticalPowerStep(
    reading: String,
    onReading: (String) -> Unit,
    onNext: () -> Unit
) {
    val positiveColor = Color(0xFF34C759)
    val negativeColor = Color(0xFFFF3B30)
    val warningColor = Color(0xFFFF8000)

    val power = reading.toDoubleOrNull()
    val isGood = power != null && power >= -25.0 && power <= -8.0
    val isWeak = power != null && power < -25.0
    val isStrong = power != null && power > -8.0

    StepCard {
        StepTitle("Optical Power Check")
        StepDescription("Enter the optical power reading from the ONT/ONU device. Acceptable range: -8 dBm to -25 dBm.")

        Spacer(Modifier.height(20.dp))

        InputField(
            label = "Optical Power (dBm)",
            value = reading,
            onValueChange = onReading,
            keyboardType = KeyboardType.Number
        )

        if (reading.isNotEmpty() && power != null) {
            Spacer(Modifier.height(12.dp))

            // Signal indicator
            val (statusText, statusColor) = when {
                isGood -> "Signal Good" to positiveColor
                isWeak -> "Signal Weak \u2014 may cause issues" to warningColor
                isStrong -> "Signal Too Strong \u2014 check attenuator" to warningColor
                else -> "Invalid reading" to negativeColor
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isGood -> positiveColor.copy(alpha = 0.1f)
                            else -> warningColor.copy(alpha = 0.1f)
                        }
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isGood) "\u2713" else "\u26A0",
                    fontSize = 16.sp,
                    color = statusColor
                )
                Text(statusText, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = statusColor)
            }
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton("Continue", enabled = reading.isNotBlank()) { onNext() }
    }
}

// =====================================================================
// Step 11: Speed Test
// =====================================================================

@Composable
private fun SpeedTestStep(
    download: String,
    upload: String,
    onDownload: (String) -> Unit,
    onUpload: (String) -> Unit,
    onNext: () -> Unit
) {
    val positiveColor = Color(0xFF34C759)

    var testing by remember { mutableStateOf(false) }
    var testComplete by remember { mutableStateOf(false) }

    StepCard {
        StepTitle("Speed Test")
        StepDescription("Run a speed test to verify the connection quality. You can enter values manually or run the built-in test.")

        Spacer(Modifier.height(20.dp))

        if (!testComplete) {
            // Run test button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (!testing) Modifier.clickable {
                            testing = true
                        } else Modifier
                    )
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (testing) {
                    // Simulate speed test
                    LaunchedEffect(Unit) {
                        delay(3000)
                        onDownload("85.4")
                        onUpload("42.1")
                        testing = false
                        testComplete = true
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Testing...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Please wait", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "\u25B6",
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap to Run Speed Test",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("\u2014 or enter manually \u2014", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            Spacer(Modifier.height(16.dp))
        }

        // Manual entry / results
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                InputField(
                    label = "Download (Mbps)",
                    value = download,
                    onValueChange = { onDownload(it); testComplete = true },
                    keyboardType = KeyboardType.Decimal
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                InputField(
                    label = "Upload (Mbps)",
                    value = upload,
                    onValueChange = { onUpload(it); testComplete = true },
                    keyboardType = KeyboardType.Decimal
                )
            }
        }

        if (testComplete && download.isNotBlank() && upload.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(positiveColor.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u2193", fontSize = 16.sp, color = positiveColor)
                    Text("${download} Mbps", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = positiveColor)
                    Text("Download", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u2191", fontSize = 16.sp, color = positiveColor)
                    Text("${upload} Mbps", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = positiveColor)
                    Text("Upload", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            "Continue",
            enabled = download.isNotBlank() && upload.isNotBlank()
        ) { onNext() }
    }
}

// =====================================================================
// Step 12: Happy Code (4-digit OTP)
// =====================================================================

@Composable
private fun HappyCodeStep(
    code: String,
    onCode: (String) -> Unit,
    onNext: () -> Unit
) {
    StepCard {
        StepTitle("Happy Code")
        StepDescription("Ask the customer to provide the 4-digit Happy Code sent to their registered mobile number. This confirms they are satisfied with the installation.")

        Spacer(Modifier.height(24.dp))

        // 4-digit OTP display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 0 until 4) {
                val digit = code.getOrNull(i)?.toString() ?: ""
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            2.dp,
                            if (digit.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (i < 3) Spacer(Modifier.width(12.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Hidden text field to capture input
        BasicTextField(
            value = code,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }.take(4)
                onCode(filtered)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = TextStyle(
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                letterSpacing = 8.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "The customer acknowledges that the internet will be fully ready within 2 days.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        PrimaryButton("Verify & Complete", enabled = code.length == 4) { onNext() }
    }
}

// =====================================================================
// Step 13: Installation Complete
// =====================================================================

@Composable
private fun InstallationCompleteStep(
    task: TaskData,
    onFinish: () -> Unit
) {
    val positiveColor = Color(0xFF34C759)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Success checkmark
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(positiveColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "\u2713",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = positiveColor
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Installation Complete!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = positiveColor
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Task ${task.taskId} has been successfully installed.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Pending activation verification.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        StepCard {
            Text(
                "SUMMARY",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(12.dp))
            InfoRow("Task", task.taskId)
            Spacer(Modifier.height(8.dp))
            InfoRow("Connection", task.connectionId ?: "--")
            Spacer(Modifier.height(8.dp))
            InfoRow("Area", task.customerArea ?: "--")
            Spacer(Modifier.height(8.dp))
            InfoRow("Status", "INSTALLED")
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton("Back to Tasks") { onFinish() }
    }
}
