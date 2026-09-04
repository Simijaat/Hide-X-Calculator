package com.example.vaultcalc

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.example.vaultcalc.data.security.VaultSecurityManager
import com.example.vaultcalc.ui.theme.VaultCalcTheme
import com.example.vaultcalc.navigation.VaultCalcNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securityManager: VaultSecurityManager

    private val _sharedUrl = MutableStateFlow<String?>(null)
    val sharedUrl: StateFlow<String?> = _sharedUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    private var wasPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        handleIntent(intent)

        setContent {
            VaultCalcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLoading by _isLoading.collectAsState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        VaultCalcNavigation(securityManager, sharedUrl)

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
                // Basic URL check
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    _sharedUrl.value = url
                }
            }
        }
    }

    fun clearSharedUrl() {
        _sharedUrl.value = null
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        securityManager.updateActivity()
    }

    override fun onResume() {
        super.onResume()
        val wasExpecting = securityManager.isExpectingExternalActivity
        securityManager.isExpectingExternalActivity = false
        if (wasPaused && !wasExpecting) {
            _isLoading.value = true
            lifecycleScope.launch {
                delay(1000)
                _isLoading.value = false
            }
        }
        wasPaused = false
    }

    override fun onPause() {
        super.onPause()
        securityManager.lockVault()
        wasPaused = true
    }
}
