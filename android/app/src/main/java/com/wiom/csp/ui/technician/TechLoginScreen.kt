package com.wiom.csp.ui.technician

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.wiom.csp.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiom.csp.domain.model.Technician
import com.wiom.csp.ui.theme.WiomCspTheme

@Composable
fun TechLoginScreen(
    technicians: List<Technician>,
    isLoading: Boolean,
    onLogin: (String) -> Unit
) {
    val colors = WiomCspTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimary),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Logo / Brand — centered
            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.brandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wiom_logo),
                        contentDescription = "Wiom",
                        modifier = Modifier.size(32.dp),
                        tint = colors.bgPrimary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Title — left-aligned for F-pattern
            Text(
                stringResource(R.string.tech_app_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle — left-aligned for F-pattern
            Text(
                stringResource(R.string.tech_select_profile),
                fontSize = 14.sp,
                color = colors.textMuted
            )

            Spacer(Modifier.height(40.dp))

            // Technician list — cards are full-width (centered by nature)
            if (isLoading) {
                Text(
                    stringResource(R.string.home_loading),
                    fontSize = 14.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    technicians.forEach { tech ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.bgCard)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                .clickable { onLogin(tech.id) }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.brandPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    tech.name.first().toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.bgPrimary
                                )
                            }

                            // Name + details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    tech.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    "Band ${tech.band.name} \u00B7 ${tech.id}",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }

                            // Availability dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (tech.available) colors.positive
                                        else colors.textMuted
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.tech_only_csp),
                fontSize = 12.sp,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}
