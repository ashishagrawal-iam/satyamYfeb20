package com.wiom.csp.ui.menu

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.wiom.csp.R
import com.wiom.csp.ui.theme.WiomCspTheme

private data class MenuItemDef(
    val key: String,
    val labelRes: Int,
    val descRes: Int,
    val icon: ImageVector
)

private val menuItemDefs = listOf(
    MenuItemDef("wallet", R.string.menu_wallet, R.string.menu_wallet_desc, Icons.Default.AccountBalanceWallet),
    MenuItemDef("team", R.string.menu_team, R.string.menu_team_desc, Icons.Default.Groups),
    MenuItemDef("netbox", R.string.menu_netbox, R.string.menu_netbox_desc, Icons.Default.Inventory2),
    MenuItemDef("support", R.string.menu_support, R.string.menu_support_desc, Icons.Default.SupportAgent),
    MenuItemDef("policies", R.string.menu_policies, R.string.menu_policies_desc, Icons.Default.Description),
    MenuItemDef("profile", R.string.menu_profile, R.string.menu_profile_desc, Icons.Default.Person),
    MenuItemDef("technician", R.string.menu_technician, R.string.menu_technician_desc, Icons.Default.Engineering),
)

@Composable
fun SecondaryMenuDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val colors = WiomCspTheme.colors

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.overlayBg)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClose() },
            contentAlignment = Alignment.CenterEnd
        ) {
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .widthIn(max = 320.dp)
                        .fillMaxHeight()
                        .background(colors.bgSecondary)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { /* consume */ }
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.menu_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            "\u2715",
                            modifier = Modifier
                                .clickable { onClose() }
                                .padding(4.dp),
                            fontSize = 20.sp,
                            color = colors.textMuted
                        )
                    }

                    // Menu items
                    Spacer(Modifier.height(8.dp))

                    menuItemDefs.forEach { item ->
                        val label = stringResource(item.labelRes)
                        val desc = stringResource(item.descRes)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(item.key) }
                                .semantics { contentDescription = "$label: $desc" }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Icon in a 40x40 box with bgCard background
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.bgCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    item.icon,
                                    contentDescription = label,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    desc,
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }

                            // Chevron
                            Text(
                                "\u203A",
                                fontSize = 16.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
