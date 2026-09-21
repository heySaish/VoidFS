package com.voidkernel.voidfs.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voidkernel.voidfs.ui.screens.home.HomeScreen
import com.voidkernel.voidfs.ui.screens.kstat.KstatScreen
import com.voidkernel.voidfs.ui.screens.paths.PathsScreen
import com.voidkernel.voidfs.ui.screens.settings.SettingsScreen
import com.voidkernel.voidfs.ui.theme.*
import com.voidkernel.voidfs.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Paths : Screen("paths", "Paths", Icons.Default.Folder)
    object Kstat : Screen("kstat", "Kstat", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        Screen.Home,
        Screen.Paths,
        Screen.Kstat,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = VoidSurface,
                contentColor = VoidPurplePrimary
            ) {
                items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (selected) VoidPurplePrimary else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                color = if (selected) VoidPurplePrimary else TextMuted
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        snackbarHost = {
            uiState.errorMessage?.let { msg ->
                Snackbar(
                    containerColor = VoidInactiveRed,
                    contentColor = TextPrimary,
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("OK", color = TextPrimary)
                        }
                    }
                ) { Text(msg) }
            }
            uiState.successMessage?.let { msg ->
                Snackbar(
                    containerColor = VoidActiveGreen,
                    contentColor = TextPrimary,
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("OK", color = TextPrimary)
                        }
                    }
                ) { Text(msg) }
            }
        },
        containerColor = VoidBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshAll() }
                )
            }
            composable(Screen.Paths.route) {
                PathsScreen(
                    uiState = uiState,
                    onAddPath = { viewModel.addPath(it) },
                    onRemovePath = { viewModel.removePath(it) },
                    onAddMount = { viewModel.addMount(it) },
                    onRemoveMount = { viewModel.removeMount(it) }
                )
            }
            composable(Screen.Kstat.route) {
                KstatScreen(
                    uiState = uiState,
                    onAddKstat = { viewModel.addKstat(it) },
                    onRemoveKstat = { viewModel.removeKstat(it) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    uiState = uiState,
                    onApplyUname = { rel, ver -> viewModel.applyUnameSpoof(rel, ver) },
                    onToggleLogging = { viewModel.toggleLogging(it) },
                    onRefresh = { viewModel.refreshAll() }
                )
            }
        }
    }
}
