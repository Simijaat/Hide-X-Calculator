package com.example.vaultcalc.ui.vpn

import androidx.lifecycle.ViewModel
import com.example.vaultcalc.vpn.VpnLocation
import com.example.vaultcalc.vpn.VpnManager
import com.example.vaultcalc.vpn.VpnState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class VpnViewModel @Inject constructor(
    private val vpnManager: VpnManager
) : ViewModel() {

    val vpnState: StateFlow<VpnState> = vpnManager.vpnState
    val selectedLocation: StateFlow<VpnLocation> = vpnManager.selectedLocation
    val availableLocations = VpnLocation.values().toList()

    fun selectLocation(location: VpnLocation) {
        vpnManager.setLocation(location)
    }

    fun connect() {
        vpnManager.connect()
    }

    fun disconnect() {
        vpnManager.disconnect()
    }
}
