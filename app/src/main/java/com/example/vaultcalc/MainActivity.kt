package com.example.vaultcalc

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.vaultcalc.data.security.VaultSecurityManager
import com.example.vaultcalc.ui.theme.VaultCalcTheme
import com.example.vaultcalc.navigation.VaultCalcNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securityManager: VaultSecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots and recent-task previews
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Start foreground inactivity tracking
        lifecycleScope.launch {
            while (isActive) {
                delay(30_000) // Check every 30 seconds
                securityManager.checkInactivity()
            }
        }

        setContent {
            VaultCalcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VaultCalcNavigation(securityManager)
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        securityManager.updateActivity()
    }
}
