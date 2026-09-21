package com.voidkernel.voidfs.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voidkernel.voidfs.ui.theme.*
import com.voidkernel.voidfs.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: UiState,
    onApplyUname: (release: String, version: String) -> Unit,
    onToggleLogging: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    var unameRelease by remember(uiState.unameRelease) { mutableStateOf(uiState.unameRelease.ifBlank { uiState.status.kernel }) }
    var unameVersion by remember(uiState.unameVersion) { mutableStateOf(uiState.unameVersion.ifBlank { "#1 SMP PREEMPT" }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Debug",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBackground)
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

            // Uname Spoof Card
            UnameSpoofCard(
                release = unameRelease,
                version = unameVersion,
                onReleaseChange = { unameRelease = it },
                onVersionChange = { unameVersion = it },
                onApply = { onApplyUname(unameRelease, unameVersion) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Debug Card
            DebugCard(
                uiState = uiState,
                onToggleLogging = onToggleLogging,
                onRefresh = onRefresh
            )

            Spacer(modifier = Modifier.height(16.dp))

            // About Card
            AboutCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun UnameSpoofCard(
    release: String,
    version: String,
    onReleaseChange: (String) -> Unit,
    onVersionChange: (String) -> Unit,
    onApply: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VoidCardBorder, RoundedCornerShape(16.dp)),
        color = VoidSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = VoidPurplePrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Uname Spoof Engine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Release String", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = release,
                onValueChange = onReleaseChange,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VoidPurplePrimary,
                    unfocusedBorderColor = VoidCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("Version String", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = version,
                onValueChange = onVersionChange,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VoidPurplePrimary,
                    unfocusedBorderColor = VoidCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onApply,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = VoidPurplePrimary)
            ) {
                Text("Apply Uname Spoof", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DebugCard(
    uiState: UiState,
    onToggleLogging: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VoidCardBorder, RoundedCornerShape(16.dp)),
        color = VoidSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = VoidCyanAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Debug & Diagnostics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = VoidPurplePrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Kernel Logging switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Kernel Debug Logging", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text("enable_log 0 | 1", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
                Switch(
                    checked = uiState.isLoggingEnabled,
                    onCheckedChange = onToggleLogging,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = VoidPurplePrimary,
                        uncheckedTrackColor = VoidSurfaceVariant
                    )
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = VoidCardBorder)

            DebugInfoRow("SUSFS Version", uiState.status.version)
            DebugInfoRow("Supported Engine", if (uiState.status.supported) "Yes (Live)" else "No")
            DebugInfoRow("Running Kernel", uiState.status.kernel)
            DebugInfoRow("Resolved CLI Binary", uiState.binaryPath)
        }
    }
}

@Composable
fun DebugInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
fun AboutCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VoidCardBorder, RoundedCornerShape(16.dp)),
        color = VoidSurface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = VoidPurplePrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "VoidFS Manager v1.0.0",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Dedicated SUSFS Control Frontend for VoidKernel Ecosystem.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
