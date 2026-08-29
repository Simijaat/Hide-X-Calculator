package com.example.vaultcalc.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.vaultcalc.MainActivity
import com.example.vaultcalc.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class SecureVpnService : VpnService() {

    @Inject
    lateinit var vpnManager: VpnManager

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_CONNECT -> {
                val locationName = intent.getStringExtra(EXTRA_LOCATION) ?: VpnLocation.US.name
                val location = try {
                    VpnLocation.valueOf(locationName)
                } catch (e: Exception) {
                    VpnLocation.US
                }
                startVpn(location)
            }
            ACTION_DISCONNECT -> {
                stopVpn()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn(location: VpnLocation) {
        if (isRunning.getAndSet(true)) return

        startForeground(NOTIFICATION_ID, createNotification(location))
        vpnManager.updateState(VpnState.CONNECTING)

        vpnJob = serviceScope.launch {
            try {
                // In a production app, we would resolve the server address and perform a handshake here.
                // Since this is a test/dev implementation without a real server, we build a local dummy interface.

                val builder = Builder()
                    .addAddress("10.0.0.2", 24)
                    .addRoute("10.0.0.0", 24) // Only route test subnet to avoid breaking real internet
                    .addDnsServer("8.8.8.8")
                    .setSession("VaultVPN - ${location.displayName}")
                    .setMtu(1400)
                    .setBlocking(true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }

                vpnInterface = builder.establish()

                if (vpnInterface == null) {
                    throw IllegalStateException("Failed to establish VPN interface. Permission may have been revoked.")
                }

                vpnManager.updateState(VpnState.CONNECTED)

                // Packet loop to keep the tunnel open and handle local routing dev logic
                runPacketLoop()
            } catch (e: Exception) {
                Log.e(TAG, "VPN Error: ${e.message}", e)
                vpnManager.updateState(VpnState.ERROR)
                stopVpn()
            }
        }
    }

    private fun runPacketLoop() {
        val vpnFd = vpnInterface?.fileDescriptor ?: return
        val inputStream = FileInputStream(vpnFd)
        val outputStream = FileOutputStream(vpnFd)

        val buffer = ByteBuffer.allocate(32767)

        try {
            while (isRunning.get() && serviceScope.isActive) {
                val length = inputStream.read(buffer.array())
                if (length > 0) {
                    // For dev purposes, we just drop the packet.
                    // In a real implementation, we would encrypt it and send it to the VPN server via a protected socket.
                    buffer.clear()
                }
            }
        } catch (e: Exception) {
            if (isRunning.get()) {
                Log.e(TAG, "Packet loop error", e)
            }
        }
    }

    private fun stopVpn() {
        if (!isRunning.getAndSet(false)) return

        vpnManager.updateState(VpnState.DISCONNECTING)
        vpnJob?.cancel()

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null

        vpnManager.updateState(VpnState.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN permission revoked by system")
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active VPN connection status"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(location: VpnLocation): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vault VPN Active")
            .setContentText("Connected to ${location.displayName}")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "SecureVpnService"
        const val ACTION_CONNECT = "com.example.vaultcalc.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vaultcalc.vpn.DISCONNECT"
        const val EXTRA_LOCATION = "location"
        private const val CHANNEL_ID = "vault_vpn_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
