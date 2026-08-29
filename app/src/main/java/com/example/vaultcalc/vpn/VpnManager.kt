package com.example.vaultcalc.vpn

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _selectedLocation = MutableStateFlow(VpnLocation.default())
    val selectedLocation: StateFlow<VpnLocation> = _selectedLocation.asStateFlow()

    fun setLocation(location: VpnLocation) {
        if (_vpnState.value == VpnState.DISCONNECTED || _vpnState.value == VpnState.ERROR) {
            _selectedLocation.value = location
        }
    }

    fun updateState(newState: VpnState) {
        _vpnState.value = newState
    }

    fun connect() {
        if (_vpnState.value == VpnState.CONNECTED || _vpnState.value == VpnState.CONNECTING) return

        updateState(VpnState.PREPARING)
        val intent = Intent(context, SecureVpnService::class.java).apply {
            action = SecureVpnService.ACTION_CONNECT
            putExtra(SecureVpnService.EXTRA_LOCATION, _selectedLocation.value.name)
        }
        context.startService(intent)
    }

    fun disconnect() {
        if (_vpnState.value == VpnState.DISCONNECTED) return

        updateState(VpnState.DISCONNECTING)
        val intent = Intent(context, SecureVpnService::class.java).apply {
            action = SecureVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }
}
