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
import com.example.vaultcalc.ui.vpn.VpnScreen

@Composable
fun VaultCalcNavigation(securityManager: VaultSecurityManager) {
    val navController = rememberNavController()
    val isVaultUnlocked by securityManager.isVaultUnlocked.collectAsState()

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
                onNavigateToVpn = {
                    navController.navigate("vpn")
                }
            )
        }
        composable("browser") {
            BrowserScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("vpn") {
            VpnScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
