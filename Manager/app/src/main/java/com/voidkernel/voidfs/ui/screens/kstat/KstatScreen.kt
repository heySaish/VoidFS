package com.voidkernel.voidfs.ui.screens.kstat

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voidkernel.voidfs.data.model.SusfsKstatItem
import com.voidkernel.voidfs.ui.screens.paths.EmptyStatePlaceholder
import com.voidkernel.voidfs.ui.theme.*
import com.voidkernel.voidfs.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KstatScreen(
    uiState: UiState,
    onAddKstat: (String) -> Unit,
    onRemoveKstat: (SusfsKstatItem) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var targetPathInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SUS Kstat Spoofing",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = VoidPurplePrimary,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Kstat")
            }
        },
        containerColor = VoidBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Spoofed Attributes Target Paths",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.spoofedKstats.isEmpty()) {
                EmptyStatePlaceholder(text = "No spoofed kstat paths configured.")
            } else {
                LazyColumn {
                    items(uiState.spoofedKstats, key = { it.id }) { item ->
                        KstatCard(item = item, onDelete = { onRemoveKstat(item) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = VoidSurface,
            title = {
                Text("Add Kstat Target Path", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        "Enter the file or directory path whose stat attributes will be spoofed by SUSFS:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetPathInput,
                        onValueChange = { targetPathInput = it },
                        placeholder = { Text("/system/bin/example", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VoidPurplePrimary,
                            unfocusedBorderColor = VoidCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetPathInput.isNotBlank()) {
                            onAddKstat(targetPathInput)
                            targetPathInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoidPurplePrimary)
                ) {
                    Text("Add Kstat", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun KstatCard(item: SusfsKstatItem, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, VoidCardBorder, RoundedCornerShape(12.dp)),
        color = VoidSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = VoidCyanAccent
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.targetPath,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Stat attributes spoof active",
                        style = MaterialTheme.typography.labelMedium,
                        color = VoidActiveGreen
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = VoidInactiveRed
                )
            }
        }
    }
}
