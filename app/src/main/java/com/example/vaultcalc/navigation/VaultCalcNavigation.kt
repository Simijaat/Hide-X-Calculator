package com.example.vaultcalc.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vaultcalc.data.security.VaultSecurityManager
import com.example.vaultcalc.ui.calculator.CalculatorScreen
import com.example.vaultcalc.ui.vault.VaultScreen
import com.example.vaultcalc.ui.browser.BrowserScreen
import com.example.vaultcalc.ui.download.DownloadCenterScreen
import com.example.vaultcalc.ui.PlaceholderScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.vaultcalc.MainActivity

@Composable
fun VaultCalcNavigation(
    securityManager: VaultSecurityManager,
    sharedUrl: StateFlow<String?>? = null
) {
    val navController = rememberNavController()
    val isVaultUnlocked by securityManager.isVaultUnlocked.collectAsState()

    // We need to manage browser-to-download routing URL passing
    val _browserDownloadUrl = MutableStateFlow<String?>(null)
    val browserDownloadUrl = _browserDownloadUrl.asStateFlow()

    LaunchedEffect(isVaultUnlocked) {
        if (!isVaultUnlocked) {
            navController.popBackStack("calculator", inclusive = false)
        }
    }

    NavHost(navController = navController, startDestination = "calculator") {
        composable("calculator") {
            CalculatorScreen(
                onNavigateToVault = { navController.navigate("vault") }
            )
        }
        composable("vault") {
            VaultScreen(
                onNavigateBack = {
                    securityManager.lockVault()
                },
                onNavigateToBrowser = {
                    navController.navigate("browser")
                },
                onNavigateToDownloads = {
                    navController.navigate("downloads")
                },
                onNavigateToPhotos = {
                    navController.navigate("photos")
                },
                onNavigateToVideos = {
                    navController.navigate("videos")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("browser") {
            BrowserScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDownloads = { url ->
                    _browserDownloadUrl.value = url
                    navController.navigate("downloads")
                }
            )
        }
        composable("downloads") {
            // Determine which URL flow to pass (External intent vs Internal browser)
            val currentBrowserUrl by browserDownloadUrl.collectAsState()
            val activeFlow = if (currentBrowserUrl != null) {
                browserDownloadUrl
            } else {
                sharedUrl
            }

            DownloadCenterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                sharedUrlFlow = activeFlow,
                onSharedUrlHandled = {
                    // Clear whichever was active
                    if (browserDownloadUrl.value != null) {
                        _browserDownloadUrl.value = null
                    } else if (navController.context is MainActivity) {
                        (navController.context as MainActivity).clearSharedUrl()
                    }
                }
            )
        }
        composable("photos") {
            PlaceholderScreen("Photos", onNavigateBack = { navController.popBackStack() })
        }
        composable("videos") {
            PlaceholderScreen("Videos", onNavigateBack = { navController.popBackStack() })
        }
        composable("settings") {
            com.example.vaultcalc.ui.settings.SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
