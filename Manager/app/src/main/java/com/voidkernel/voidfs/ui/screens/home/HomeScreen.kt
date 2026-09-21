package com.voidkernel.voidfs.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidkernel.voidfs.ui.theme.*
import com.voidkernel.voidfs.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: UiState,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VoidFS",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VoidPurplePrimary.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoidPurplePrimary)
                        ) {
                            Text(
                                text = "SUSFS CONTROL",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = VoidPurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = VoidPurplePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBackground
                )
            )
        },
        containerColor = VoidBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Big Status Card (KSU-Style Active Card)
            BigStatusCard(uiState = uiState)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Feature Engines",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Active Features Checklist
            val features = uiState.status.features
            FeatureRow(name = "SUS Path", isEnabled = features.susPath)
            FeatureRow(name = "SUS Mount", isEnabled = features.susMount)
            FeatureRow(name = "SUS Kstat", isEnabled = features.susKstat)
            FeatureRow(name = "Uname Spoof", isEnabled = features.setUname)
            FeatureRow(name = "SUS_SU Engine", isEnabled = features.susSu)
            FeatureRow(name = "Try Umount", isEnabled = features.tryUmount)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun BigStatusCard(uiState: UiState) {
    val isSupported = uiState.status.supported
    val activeColor = if (isSupported) VoidActiveGreen else VoidInactiveRed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VoidCardBorder, RoundedCornerShape(16.dp)),
        color = VoidSurface
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSupported) "SUSFS ACTIVE" else "SUSFS INACTIVE / UNSUPPORTED",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = activeColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Engine Version: ${uiState.status.version}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = VoidCardBorder
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Kernel",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Text(
                        text = uiState.status.kernel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Integration",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Text(
                        text = "VoidKernel",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = VoidPurplePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureRow(name: String, isEnabled: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, VoidCardBorder, RoundedCornerShape(12.dp)),
        color = VoidSurface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isEnabled) VoidActiveGreen else TextMuted
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isEnabled) VoidActiveGreen.copy(alpha = 0.15f) else VoidSurfaceVariant
            ) {
                Text(
                    text = if (isEnabled) "Enabled" else "Disabled",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled) VoidActiveGreen else TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
