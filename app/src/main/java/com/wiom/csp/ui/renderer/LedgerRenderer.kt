package com.wiom.csp.ui.renderer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wiom.csp.domain.model.WalletTransaction
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.ui.common.StatusBadge
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a list of wallet transactions, fully data-driven from schema.
 * Labels, credit/debit direction, and categories all come from schema definitions.
 */
@Composable
fun LedgerRenderer(
    transactions: List<WalletTransaction>,
    schema: SchemaResolver,
    hindi: Boolean,
    categoryFilter: String? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("en", "IN")) }
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 2
        }
    }

    val filteredTransactions = remember(transactions, categoryFilter) {
        if (categoryFilter == null) {
            transactions
        } else {
            transactions.filter { tx ->
                schema.resolveWalletCategory(tx.type) == categoryFilter
            }
        }
    }

    if (filteredTransactions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hindi) "\u0915\u094B\u0908 \u0932\u0947\u0928\u0926\u0947\u0928 \u0928\u0939\u0940\u0902" else "No transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(
            items = filteredTransactions,
            key = { it.id }
        ) { transaction ->
            TransactionRow(
                transaction = transaction,
                schema = schema,
                hindi = hindi,
                dateFormat = dateFormat,
                currencyFormat = currencyFormat
            )
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: WalletTransaction,
    schema: SchemaResolver,
    hindi: Boolean,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat
) {
    val lineType = remember(transaction.type) { schema.resolveWalletLineType(transaction.type) }
    val label = remember(transaction.type, hindi) { schema.resolveWalletLabel(transaction.type, hindi) }
    val isCredit = lineType?.isCredit ?: (transaction.amount > 0)

    val amountText = remember(transaction.amount, isCredit) {
        val prefix = if (isCredit) "+" else "-"
        val formatted = currencyFormat.format(kotlin.math.abs(transaction.amount))
        "$prefix$formatted"
    }
    val amountColor = if (isCredit) Color(0xFF34C759) else Color(0xFFFF3B30)

    val statusColor = when (transaction.status.uppercase()) {
        "COMPLETED" -> Color(0xFF34C759)
        "PENDING" -> Color(0xFFFF8000)
        "FAILED" -> Color(0xFFFF3B30)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusBgColor = when (transaction.status.uppercase()) {
        "COMPLETED" -> Color(0x1A34C759)
        "PENDING" -> Color(0x1AFF8000)
        "FAILED" -> Color(0x1AFF3B30)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left side: label, description, date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right side: amount + status badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(
                    label = transaction.status,
                    color = statusColor,
                    bgColor = statusBgColor
                )
            }
        }
    }
}
