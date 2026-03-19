package com.wiom.csp.ui.isprecharge

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.domain.model.RechargeCustomer
import com.wiom.csp.domain.model.TaskData
import com.wiom.csp.mock.SeedDataProvider

/**
 * Phase 0 — Simple acknowledgment screen.
 *
 * Shows list of customers due for recharge. Partner selects which ones
 * they've recharged on the ISP portal, then confirms. No automation,
 * no portal access — just a checklist + acknowledge.
 */
@Composable
fun Phase0AcknowledgeScreen(
    task: TaskData,
    onBack: () -> Unit,
    onConfirm: (taskId: String, selectedCustomerIds: List<String>) -> Unit
) {
    val customers = remember { SeedDataProvider.buildSeedRechargeCustomers() }
    val selectedIds = remember { mutableStateListOf<String>() }
    val allSelected = selectedIds.size == customers.size
    var showSuccess by remember { mutableStateOf(false) }
    var confirmedCount by remember { mutableIntStateOf(0) }

    if (showSuccess) {
        Phase0SuccessScreen(
            confirmedCount = confirmedCount,
            totalShare = confirmedCount * 300.0,
            onDone = { onConfirm(task.taskId, selectedIds.toList()) }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1832))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mark as Recharged",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${customers.size} customers pending",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Info card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF252347))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFDCB6E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Select the customers you have recharged on the ISP portal, then confirm.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Select all
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        if (allSelected) selectedIds.clear()
                        else {
                            selectedIds.clear()
                            selectedIds.addAll(customers.map { it.id })
                        }
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = {
                        if (allSelected) selectedIds.clear()
                        else {
                            selectedIds.clear()
                            selectedIds.addAll(customers.map { it.id })
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF00B894),
                        uncheckedColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select All (${customers.size})",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer list
            customers.forEach { customer ->
                val isSelected = customer.id in selectedIds
                CustomerCheckRow(
                    customer = customer,
                    isSelected = isSelected,
                    onToggle = {
                        if (isSelected) selectedIds.remove(customer.id)
                        else selectedIds.add(customer.id)
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Bottom bar — confirm button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E))
                .padding(16.dp)
        ) {
            if (selectedIds.isNotEmpty()) {
                Text(
                    text = "${selectedIds.size} of ${customers.size} selected  •  \u20B9${selectedIds.size * 300} share",
                    color = Color(0xFF00B894),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedIds.isNotEmpty()) Color(0xFF00B894)
                        else Color(0xFF00B894).copy(alpha = 0.3f)
                    )
                    .then(
                        if (selectedIds.isNotEmpty()) Modifier.clickable {
                            confirmedCount = selectedIds.size
                            showSuccess = true
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedIds.isEmpty()) "Select customers to confirm"
                    else "Confirm Recharged (${selectedIds.size})",
                    color = if (selectedIds.isNotEmpty()) Color.White
                    else Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CustomerCheckRow(
    customer: RechargeCustomer,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) Color(0xFF00B894).copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                if (isSelected) Color(0xFF00B894).copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.04f)
            )
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF00B894),
                uncheckedColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.size(20.dp)
        )

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
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
                Text(
                    text = customer.speed,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
                customer.username?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "...${customer.phoneLast5}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = "\u20B9${customer.shareAmount.toInt()}",
            color = if (isSelected) Color(0xFF00B894) else Color.White.copy(alpha = 0.4f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Success Screen — shown after confirming recharge
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun Phase0SuccessScreen(
    confirmedCount: Int,
    totalShare: Double,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1832)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color(0xFF00B894).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF00B894),
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recharge Confirmed!",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u0915\u0940 \u092A\u0941\u0937\u094D\u091F\u093F!",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Share amount card
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF00B894).copy(alpha = 0.12f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Color(0xFF00B894),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\u20B9${totalShare.toInt()}",
                color = Color(0xFF00B894),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "transferred to your wallet",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Text(
                text = "\u0906\u092A\u0915\u0947 \u0935\u0949\u0932\u0947\u091F \u092E\u0947\u0902 \u091F\u094D\u0930\u093E\u0902\u0938\u092B\u0930 \u0939\u094B \u0917\u092F\u093E",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$confirmedCount customer${if (confirmedCount > 1) "s" else ""} recharged",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Done button
        Box(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00B894))
                .clickable { onDone() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Back to Tasks",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
