package com.wiom.csp.ui.technician

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.wiom.csp.R
import com.wiom.csp.domain.model.Technician
import com.wiom.csp.ui.theme.WiomCspTheme

@Composable
fun TechProfileScreen(
    tech: Technician,
    activeCount: Int,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    val colors = WiomCspTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.general_back),
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(vertical = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.tech_profile),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            // Border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.borderSubtle)
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar + name
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.brandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tech.name.first().toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.bgPrimary
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    tech.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    tech.id,
                    fontSize = 14.sp,
                    color = colors.textMuted
                )

                Spacer(Modifier.height(24.dp))

                // Info card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.bgCard)
                        .padding(16.dp)
                ) {
                    ProfileInfoRow(stringResource(R.string.tech_band), "Band ${tech.band.name}", showBorder = true)
                    ProfileInfoRow(stringResource(R.string.tech_csp), tech.cspId, showBorder = true)
                    ProfileInfoRow(stringResource(R.string.tech_phone), tech.phone.ifEmpty { "--" }, showBorder = true)
                    ProfileInfoRow(stringResource(R.string.tech_joined), tech.joinDate, showBorder = false)
                }

                Spacer(Modifier.height(20.dp))

                // Stats card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.bgCard)
                        .padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.tech_stats),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatBox(
                            label = stringResource(R.string.tech_completed),
                            value = tech.completedCount,
                            color = colors.positive,
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            label = stringResource(R.string.tech_active),
                            value = activeCount,
                            color = colors.brandPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Logout button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.negative.copy(alpha = 0.1f))
                        .border(1.dp, colors.negative.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { onLogout() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.tech_logout),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.negative
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String, showBorder: Boolean) {
    val colors = WiomCspTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showBorder) Modifier.drawBehind {
                    drawLine(
                        color = colors.borderSubtle,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                } else Modifier
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = colors.textMuted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}

@Composable
private fun StatBox(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    val colors = WiomCspTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgPrimary)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = colors.textMuted
        )
    }
}

