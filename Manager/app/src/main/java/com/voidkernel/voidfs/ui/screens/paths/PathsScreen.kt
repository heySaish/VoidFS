package com.voidkernel.voidfs.ui.screens.paths

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voidkernel.voidfs.data.model.SusfsMountItem
import com.voidkernel.voidfs.data.model.SusfsPathItem
import com.voidkernel.voidfs.ui.theme.*
import com.voidkernel.voidfs.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathsScreen(
    uiState: UiState,
    onAddPath: (String) -> Unit,
    onRemovePath: (SusfsPathItem) -> Unit,
    onAddMount: (String) -> Unit,
    onRemoveMount: (SusfsMountItem) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = SUS Path, 1 = SUS Mount
    var showAddDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hidden Paths & Mounts",
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
                Icon(Icons.Default.Add, contentDescription = "Add")
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = VoidSurface,
                contentColor = VoidPurplePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = VoidPurplePrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "SUS Path",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) VoidPurplePrimary else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "SUS Mount",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) VoidPurplePrimary else TextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // SUS Path List
                if (uiState.hiddenPaths.isEmpty()) {
                    EmptyStatePlaceholder(text = "No hidden SUS paths added yet.")
                } else {
                    LazyColumn {
                        items(uiState.hiddenPaths, key = { it.id }) { item ->
                            PathCard(path = item.path, onDelete = { onRemovePath(item) })
                        }
                    }
                }
            } else {
                // SUS Mount List
                if (uiState.hiddenMounts.isEmpty()) {
                    EmptyStatePlaceholder(text = "No hidden SUS mounts added yet.")
                } else {
                    LazyColumn {
                        items(uiState.hiddenMounts, key = { it.id }) { item ->
                            PathCard(path = item.mountPath, onDelete = { onRemoveMount(item) })
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val dialogTitle = if (selectedTab == 0) "Add SUS Path" else "Add SUS Mount"
        val placeholder = if (selectedTab == 0) "/data/local/tmp/example" else "/data/adb/modules/example"

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = VoidSurface,
            title = {
                Text(dialogTitle, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(placeholder, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VoidPurplePrimary,
                        unfocusedBorderColor = VoidCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            if (selectedTab == 0) onAddPath(inputText) else onAddMount(inputText)
                            inputText = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoidPurplePrimary)
                ) {
                    Text("Add", color = TextPrimary)
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
fun PathCard(path: String, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, VoidCardBorder, RoundedCornerShape(12.dp)),
        color = VoidSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = path,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = VoidInactiveRed
                )
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}
