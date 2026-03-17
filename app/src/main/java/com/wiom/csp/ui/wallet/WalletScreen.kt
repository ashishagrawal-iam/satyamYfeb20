package com.wiom.csp.ui.wallet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.wiom.csp.domain.model.WalletData
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.ui.renderer.LedgerRenderer
import java.text.NumberFormat
import java.util.Locale

/**
 * WalletScreen: state-driven flow for viewing balance, withdrawing, and adding money.
 *
 * Flow steps: hub | withdraw_amount | withdraw_confirm | withdraw_receipt
 *           | add_amount | add_method | add_confirm | add_receipt
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WalletScreen(
    wallet: WalletData,
    schema: SchemaResolver,
    hindi: Boolean,
    onBack: () -> Unit,
    onWithdraw: (Double) -> Unit,
    onAddMoney: (Double, String) -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 2
        }
    }

    // Flow state
    var flowStep by rememberSaveable { mutableStateOf("hub") }

    // Withdraw flow state
    var withdrawAmountText by rememberSaveable { mutableStateOf("") }
    var withdrawAmount by remember { mutableDoubleStateOf(0.0) }
    var withdrawError by rememberSaveable { mutableStateOf<String?>(null) }
    var withdrawRef by rememberSaveable { mutableStateOf("") }

    // Add money flow state
    var addAmountText by rememberSaveable { mutableStateOf("") }
    var addAmount by remember { mutableDoubleStateOf(0.0) }
    var addError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMethod by rememberSaveable { mutableStateOf("") }
    var addRef by rememberSaveable { mutableStateOf("") }

    // Cooldown calculation
    val cooldownDaysRemaining = remember(wallet.lastWithdrawalAt) {
        val last = wallet.lastWithdrawalAt ?: return@remember 0
        val elapsed = System.currentTimeMillis() - last
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        if (elapsed < sevenDaysMs) {
            ((sevenDaysMs - elapsed) / (24 * 60 * 60 * 1000) + 1).toInt()
        } else 0
    }

    val withdrawDisabled = wallet.frozen || cooldownDaysRemaining > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = if (hindi) "\u0935\u0949\u0932\u0947\u091F" else "Wallet",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (flowStep == "hub") onBack()
                    else flowStep = "hub"
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Animated step content
        AnimatedContent(
            targetState = flowStep,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            },
            label = "wallet_flow"
        ) { step ->
            when (step) {
                "hub" -> WalletHub(
                    wallet = wallet,
                    schema = schema,
                    hindi = hindi,
                    currencyFormat = currencyFormat,
                    cooldownDaysRemaining = cooldownDaysRemaining,
                    withdrawDisabled = withdrawDisabled,
                    onWithdrawClick = {
                        withdrawAmountText = ""
                        withdrawError = null
                        flowStep = "withdraw_amount"
                    },
                    onAddMoneyClick = {
                        addAmountText = ""
                        addError = null
                        selectedMethod = ""
                        flowStep = "add_amount"
                    }
                )

                "withdraw_amount" -> WithdrawAmountStep(
                    hindi = hindi,
                    balance = wallet.balance,
                    amountText = withdrawAmountText,
                    error = withdrawError,
                    currencyFormat = currencyFormat,
                    onAmountChange = { text ->
                        withdrawAmountText = text
                        withdrawError = null
                    },
                    onNext = {
                        val parsed = withdrawAmountText.toDoubleOrNull()
                        if (parsed == null || parsed <= 0) {
                            withdrawError = if (hindi) "\u0935\u0948\u0927 \u0930\u093E\u0936\u093F \u0926\u0930\u094D\u091C \u0915\u0930\u0947\u0902"
                                else "Enter a valid amount"
                        } else if (parsed > wallet.balance) {
                            withdrawError = if (hindi) "\u0905\u092A\u0930\u094D\u092F\u093E\u092A\u094D\u0924 \u0936\u0947\u0937"
                                else "Amount exceeds balance"
                        } else if (!isValidTwoDecimals(withdrawAmountText)) {
                            withdrawError = if (hindi) "\u0905\u0927\u093F\u0915\u0924\u092E 2 \u0926\u0936\u092E\u0932\u0935 \u0938\u094D\u0925\u093E\u0928"
                                else "Maximum 2 decimal places"
                        } else {
                            withdrawAmount = parsed
                            flowStep = "withdraw_confirm"
                        }
                    }
                )

                "withdraw_confirm" -> WithdrawConfirmStep(
                    hindi = hindi,
                    amount = withdrawAmount,
                    currencyFormat = currencyFormat,
                    onConfirm = {
                        onWithdraw(withdrawAmount)
                        withdrawRef = "WDR-${System.currentTimeMillis()}"
                        flowStep = "withdraw_receipt"
                    },
                    onCancel = { flowStep = "withdraw_amount" }
                )

                "withdraw_receipt" -> ReceiptStep(
                    hindi = hindi,
                    title = if (hindi) "\u0928\u093F\u0915\u093E\u0938\u0940 \u0938\u092B\u0932" else "Withdrawal Successful",
                    amount = withdrawAmount,
                    reference = withdrawRef,
                    newBalance = wallet.balance - withdrawAmount,
                    currencyFormat = currencyFormat,
                    onDone = { flowStep = "hub" }
                )

                "add_amount" -> AddAmountStep(
                    hindi = hindi,
                    amountText = addAmountText,
                    error = addError,
                    onAmountChange = { text ->
                        addAmountText = text
                        addError = null
                    },
                    onNext = {
                        val parsed = addAmountText.toDoubleOrNull()
                        if (parsed == null || parsed <= 0) {
                            addError = if (hindi) "\u0935\u0948\u0927 \u0930\u093E\u0936\u093F \u0926\u0930\u094D\u091C \u0915\u0930\u0947\u0902"
                                else "Enter a valid amount"
                        } else {
                            addAmount = parsed
                            flowStep = "add_method"
                        }
                    }
                )

                "add_method" -> AddMethodStep(
                    hindi = hindi,
                    onSelect = { method ->
                        selectedMethod = method
                        flowStep = "add_confirm"
                    }
                )

                "add_confirm" -> AddConfirmStep(
                    hindi = hindi,
                    amount = addAmount,
                    method = selectedMethod,
                    currencyFormat = currencyFormat,
                    onConfirm = {
                        onAddMoney(addAmount, selectedMethod)
                        addRef = "TOP-${System.currentTimeMillis()}"
                        flowStep = "add_receipt"
                    },
                    onCancel = { flowStep = "add_method" }
                )

                "add_receipt" -> ReceiptStep(
                    hindi = hindi,
                    title = if (hindi) "\u0930\u093E\u0936\u093F \u091C\u094B\u0921\u093C\u0940 \u0917\u0908" else "Money Added",
                    amount = addAmount,
                    reference = addRef,
                    newBalance = wallet.balance + addAmount,
                    currencyFormat = currencyFormat,
                    onDone = { flowStep = "hub" }
                )
            }
        }
    }
}

// ── Hub view ────────────────────────────────────────────────────────────

@Composable
private fun WalletHub(
    wallet: WalletData,
    schema: SchemaResolver,
    hindi: Boolean,
    currencyFormat: NumberFormat,
    cooldownDaysRemaining: Int,
    withdrawDisabled: Boolean,
    onWithdrawClick: () -> Unit,
    onAddMoneyClick: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabTitles = if (hindi) {
        listOf("\u0915\u092E\u093E\u0908 \u0914\u0930 \u0915\u094D\u0930\u0947\u0921\u093F\u091F", "\u091C\u092E\u093E \u0914\u0930 \u0936\u0941\u0932\u094D\u0915", "\u0932\u0947\u0928\u0926\u0947\u0928")
    } else {
        listOf("Earnings & Credits", "Deposit & Fees", "Transactions")
    }
    val categoryFilters = listOf("earnings", "deposits_fees", "transactions")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Balance card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hindi) "\u0936\u0947\u0937 \u0930\u093E\u0936\u093F" else "Available Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currencyFormat.format(wallet.balance),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (wallet.pendingSettlement > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hindi) "\u0932\u0902\u092C\u093F\u0924 \u0928\u093F\u092A\u091F\u093E\u0928: ${currencyFormat.format(wallet.pendingSettlement)}"
                            else "Pending settlement: ${currencyFormat.format(wallet.pendingSettlement)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Frozen banner
        if (wallet.frozen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF3B30))
                    .padding(12.dp)
            ) {
                Text(
                    text = wallet.frozenReason
                        ?: if (hindi) "\u0935\u0949\u0932\u0947\u091F \u092B\u094D\u0930\u0940\u091C \u0939\u0948" else "Wallet is frozen",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Cooldown banner
        if (cooldownDaysRemaining > 0 && !wallet.frozen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF8000).copy(alpha = 0.15f))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (hindi)
                        "\u0928\u093F\u0915\u093E\u0938\u0940 \u0915\u0942\u0932\u0921\u093E\u0909\u0928: $cooldownDaysRemaining \u0926\u093F\u0928 \u0936\u0947\u0937"
                    else
                        "Withdrawal cooldown: $cooldownDaysRemaining day${if (cooldownDaysRemaining > 1) "s" else ""} remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF8000)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onWithdrawClick,
                enabled = !withdrawDisabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                Text(
                    text = if (hindi) "\u0928\u093F\u0915\u093E\u0938\u0940" else "Withdraw",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            OutlinedButton(
                onClick = onAddMoneyClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (hindi) "\u092A\u0948\u0938\u0947 \u091C\u094B\u0921\u093C\u0947\u0902" else "Add Money",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ledger tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ledger content
        LedgerRenderer(
            transactions = wallet.transactions,
            schema = schema,
            hindi = hindi,
            categoryFilter = categoryFilters[selectedTabIndex]
        )
    }
}

// ── Withdraw Amount Step ────────────────────────────────────────────────

@Composable
private fun WithdrawAmountStep(
    hindi: Boolean,
    balance: Double,
    amountText: String,
    error: String?,
    currencyFormat: NumberFormat,
    onAmountChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (hindi) "\u0928\u093F\u0915\u093E\u0938\u0940 \u0930\u093E\u0936\u093F \u0926\u0930\u094D\u091C \u0915\u0930\u0947\u0902" else "Enter withdrawal amount",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hindi) "\u0909\u092A\u0932\u092C\u094D\u0927 \u0936\u0947\u0937: ${currencyFormat.format(balance)}"
                else "Available: ${currencyFormat.format(balance)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { newValue ->
                // Allow only numbers and single decimal point
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    onAmountChange(newValue)
                }
            },
            label = { Text(if (hindi) "\u0930\u093E\u0936\u093F (\u20B9)" else "Amount (\u20B9)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (hindi) "\u0906\u0917\u0947 \u092C\u0922\u093C\u0947\u0902" else "Continue",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ── Withdraw Confirm Step ───────────────────────────────────────────────

@Composable
private fun WithdrawConfirmStep(
    hindi: Boolean,
    amount: Double,
    currencyFormat: NumberFormat,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = if (hindi) "\u0928\u093F\u0915\u093E\u0938\u0940 \u0915\u0940 \u092A\u0941\u0937\u094D\u091F\u093F \u0915\u0930\u0947\u0902" else "Confirm Withdrawal",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hindi) "\u0928\u093F\u0915\u093E\u0938\u0940 \u0930\u093E\u0936\u093F" else "Withdrawal Amount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currencyFormat.format(amount),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (hindi) "\u092A\u0941\u0937\u094D\u091F\u093F \u0915\u0930\u0947\u0902" else "Confirm",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (hindi) "\u0930\u0926\u094D\u0926 \u0915\u0930\u0947\u0902" else "Cancel",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ── Add Amount Step ─────────────────────────────────────────────────────

@Composable
private fun AddAmountStep(
    hindi: Boolean,
    amountText: String,
    error: String?,
    onAmountChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (hindi) "\u091C\u094B\u0921\u093C\u0928\u0947 \u0915\u0940 \u0930\u093E\u0936\u093F \u0926\u0930\u094D\u091C \u0915\u0930\u0947\u0902" else "Enter amount to add",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    onAmountChange(newValue)
                }
            },
            label = { Text(if (hindi) "\u0930\u093E\u0936\u093F (\u20B9)" else "Amount (\u20B9)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (hindi) "\u0906\u0917\u0947 \u092C\u0922\u093C\u0947\u0902" else "Continue",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ── Add Method Step ─────────────────────────────────────────────────────

@Composable
private fun AddMethodStep(
    hindi: Boolean,
    onSelect: (String) -> Unit
) {
    val methods = listOf(
        "UPI" to (if (hindi) "UPI" else "UPI"),
        "NET_BANKING" to (if (hindi) "\u0928\u0947\u091F \u092C\u0948\u0902\u0915\u093F\u0902\u0917" else "Net Banking"),
        "DEBIT_CARD" to (if (hindi) "\u0921\u0947\u092C\u093F\u091F \u0915\u093E\u0930\u094D\u0921" else "Debit Card")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (hindi) "\u092D\u0941\u0917\u0924\u093E\u0928 \u0935\u093F\u0927\u093F \u091A\u0941\u0928\u0947\u0902" else "Select Payment Method",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        methods.forEach { (key, label) ->
            Card(
                onClick = { onSelect(key) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                )
            }
        }
    }
}

// ── Add Confirm Step ────────────────────────────────────────────────────

@Composable
private fun AddConfirmStep(
    hindi: Boolean,
    amount: Double,
    method: String,
    currencyFormat: NumberFormat,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val methodLabel = when (method) {
        "UPI" -> "UPI"
        "NET_BANKING" -> if (hindi) "\u0928\u0947\u091F \u092C\u0948\u0902\u0915\u093F\u0902\u0917" else "Net Banking"
        "DEBIT_CARD" -> if (hindi) "\u0921\u0947\u092C\u093F\u091F \u0915\u093E\u0930\u094D\u0921" else "Debit Card"
        else -> method
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = if (hindi) "\u092D\u0941\u0917\u0924\u093E\u0928 \u0915\u0940 \u092A\u0941\u0937\u094D\u091F\u093F \u0915\u0930\u0947\u0902" else "Confirm Payment",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hindi) "\u0930\u093E\u0936\u093F" else "Amount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currencyFormat.format(amount),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (hindi) "\u0935\u093F\u0927\u093F" else "Method",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = methodLabel,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (hindi) "\u092A\u0941\u0937\u094D\u091F\u093F \u0915\u0930\u0947\u0902" else "Confirm",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (hindi) "\u0930\u0926\u094D\u0926 \u0915\u0930\u0947\u0902" else "Cancel",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ── Receipt Step (shared by withdraw and add money) ─────────────────────

@Composable
private fun ReceiptStep(
    hindi: Boolean,
    title: String,
    amount: Double,
    reference: String,
    newBalance: Double,
    currencyFormat: NumberFormat,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF34C759),
            modifier = Modifier
                .width(64.dp)
                .height(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                ReceiptRow(
                    label = if (hindi) "\u0930\u093E\u0936\u093F" else "Amount",
                    value = currencyFormat.format(amount)
                )
                Spacer(modifier = Modifier.height(12.dp))
                ReceiptRow(
                    label = if (hindi) "\u0938\u0902\u0926\u0930\u094D\u092D" else "Reference",
                    value = reference
                )
                Spacer(modifier = Modifier.height(12.dp))
                ReceiptRow(
                    label = if (hindi) "\u0928\u092F\u093E \u0936\u0947\u0937" else "New Balance",
                    value = currencyFormat.format(newBalance)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (hindi) "\u0939\u094B \u0917\u092F\u093E" else "Done",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Utility ─────────────────────────────────────────────────────────────

private fun isValidTwoDecimals(text: String): Boolean {
    val dotIndex = text.indexOf('.')
    if (dotIndex == -1) return true
    return text.length - dotIndex - 1 <= 2
}
