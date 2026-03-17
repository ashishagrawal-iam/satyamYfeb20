package com.wiom.csp.ui.deposit

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.domain.model.DepositData
import com.wiom.csp.domain.model.DepositTransaction
import com.wiom.csp.domain.model.NetBoxUnit
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.ui.common.StatusBadge
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Deposit screen: shows security deposit balance, NetBox unit ledger,
 * and transaction history.
 *
 * Balance card: deposit balance formatted as currency.
 * Info row: security deposit rate, carry fee, grace period.
 * NetBox unit list: ID, status badge, assigned/expiry date, grace/carry fee labels.
 * Transaction list: type, amount (+/-), netbox ID if applicable, date, description.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(
    deposit: DepositData,
    schema: SchemaResolver,
    hindi: Boolean,
    onBack: () -> Unit
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    val sortedTransactions = remember(deposit.transactions) {
        deposit.transactions.sortedByDescending { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (hindi) "जमा राशि" else "Deposit",
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Balance Card ────────────────────────────────────────
            item(key = "balance_card") {
                BalanceCard(
                    balance = deposit.balance,
                    currencyFormatter = currencyFormatter
                )
            }

            // ── Info Row ────────────────────────────────────────────
            item(key = "info_row") {
                InfoRow(
                    ratePerUnit = deposit.ratePerUnit,
                    carryFeePerDay = deposit.carryFeePerDay,
                    gracePeriodDays = deposit.gracePeriodDays,
                    hindi = hindi
                )
            }

            // ── NetBox Units Section ────────────────────────────────
            if (deposit.units.isNotEmpty()) {
                item(key = "units_header") {
                    SectionHeader(
                        title = if (hindi) "नेटबॉक्स इकाइयाँ" else "NetBox Units"
                    )
                }
                items(
                    items = deposit.units,
                    key = { "unit_${it.id}" }
                ) { unit ->
                    NetBoxUnitCard(
                        unit = unit,
                        gracePeriodDays = deposit.gracePeriodDays,
                        carryFeePerDay = deposit.carryFeePerDay,
                        dateFormatter = dateFormatter,
                        hindi = hindi
                    )
                }
            }

            // ── Transactions Section ────────────────────────────────
            item(key = "transactions_header") {
                SectionHeader(
                    title = if (hindi) "लेनदेन" else "Transactions"
                )
            }

            if (sortedTransactions.isEmpty()) {
                item(key = "no_transactions") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hindi) "कोई लेनदेन नहीं" else "No transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = sortedTransactions,
                    key = { "txn_${it.id}" }
                ) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        currencyFormatter = currencyFormatter,
                        dateFormatter = dateFormatter,
                        hindi = hindi
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Balance Card ────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(
    balance: Double,
    currencyFormatter: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Deposit Balance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currencyFormatter.format(balance),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ── Info Row ────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(
    ratePerUnit: Double,
    carryFeePerDay: Double,
    gracePeriodDays: Int,
    hindi: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoItem(
                label = if (hindi) "दर/यूनिट" else "Rate/Unit",
                value = "\u20B9${ratePerUnit.toLong()}"
            )
            InfoItem(
                label = if (hindi) "कैरी शुल्क/दिन" else "Carry Fee/Day",
                value = "\u20B9${carryFeePerDay.toLong()}"
            )
            InfoItem(
                label = if (hindi) "छूट अवधि" else "Grace Period",
                value = "$gracePeriodDays ${if (hindi) "दिन" else "days"}"
            )
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Section Header ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

// ── NetBox Unit Card ────────────────────────────────────────────────────

@Composable
private fun NetBoxUnitCard(
    unit: NetBoxUnit,
    gracePeriodDays: Int,
    carryFeePerDay: Double,
    dateFormatter: SimpleDateFormat,
    hindi: Boolean
) {
    val now = System.currentTimeMillis()
    val msPerDay = 86_400_000L

    // Compute grace/carry state
    val expiryInfo = remember(unit, gracePeriodDays, carryFeePerDay, now) {
        if (unit.expiresAt == null || unit.expiresAt >= now) {
            null // Not expired
        } else {
            val daysPastExpiry = ((now - unit.expiresAt) / msPerDay).toInt()
            if (daysPastExpiry < gracePeriodDays) {
                UnitExpiryInfo.GracePeriod(daysPastExpiry)
            } else {
                val carryDays = daysPastExpiry - gracePeriodDays
                val accrued = carryDays * carryFeePerDay
                UnitExpiryInfo.CarryFee(accrued)
            }
        }
    }

    val isLost = unit.status.equals("LOST", ignoreCase = true)

    // Status badge colors
    val (statusLabel, statusColor, statusBg) = remember(unit.status) {
        when (unit.status.uppercase()) {
            "ACTIVE" -> Triple("Active", Color(0xFF34C759), Color(0xFF34C759).copy(alpha = 0.12f))
            "EXPIRED" -> Triple("Expired", Color(0xFFFF8000), Color(0xFFFF8000).copy(alpha = 0.12f))
            "LOST" -> Triple("Lost", Color(0xFFFF3B30), Color(0xFFFF3B30).copy(alpha = 0.12f))
            else -> Triple(unit.status, Color(0xFF8E8E93), Color(0xFF8E8E93).copy(alpha = 0.12f))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Row 1: ID + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = unit.id,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusBadge(
                    label = statusLabel,
                    color = statusColor,
                    bgColor = statusBg
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: assigned + expiry dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (hindi) "सौंपा" else "Assigned",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormatter.format(Date(unit.assignedAt)),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                unit.expiresAt?.let { expiry ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (hindi) "समाप्ति" else "Expires",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormatter.format(Date(expiry)),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Row 3: special labels for grace/carry/lost
            when {
                isLost -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hindi) "खोया - जमा राशि काटी गई" else "Lost \u2014 deposit deducted",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFFFF3B30)
                        )
                    }
                }
                expiryInfo is UnitExpiryInfo.GracePeriod -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF8000).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hindi) "छूट अवधि" else "Grace period",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFFFF8000)
                        )
                    }
                }
                expiryInfo is UnitExpiryInfo.CarryFee -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hindi)
                                "कैरी शुल्क: \u20B9${expiryInfo.accrued.toLong()}"
                            else
                                "Carry fee accrued: \u20B9${expiryInfo.accrued.toLong()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFFFF3B30)
                        )
                    }
                }
            }
        }
    }
}

private sealed class UnitExpiryInfo {
    data class GracePeriod(val daysPast: Int) : UnitExpiryInfo()
    data class CarryFee(val accrued: Double) : UnitExpiryInfo()
}

// ── Transaction Row ─────────────────────────────────────────────────────

@Composable
private fun TransactionRow(
    transaction: DepositTransaction,
    currencyFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    hindi: Boolean
) {
    val isCredit = transaction.amount > 0
    val amountColor = if (isCredit) Color(0xFF34C759) else Color(0xFFFF3B30)
    val amountPrefix = if (isCredit) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: type + description + netbox ID
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.type,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.description.isNotBlank()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    transaction.netboxId?.let { nbId ->
                        Text(
                            text = nbId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = dateFormatter.format(Date(transaction.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: amount
            Text(
                text = "$amountPrefix${currencyFormatter.format(kotlin.math.abs(transaction.amount))}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = amountColor
            )
        }
    }
}
