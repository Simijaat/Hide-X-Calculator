package com.example.vaultcalc.data.browser

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdBlockManager @Inject constructor(private val context: Context) {

    private val blockedDomains = HashSet<String>()
    var isEnabled = true
        private set

    suspend fun loadRules() {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("ad_hosts.txt")
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim()
                    if (!trimmed.isNullOrEmpty() && !trimmed.startsWith("#")) {
                        blockedDomains.add(trimmed)
                    }
                }
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isAd(url: String): Boolean {
        if (!isEnabled) return false
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            isHostBlocked(host)
        } catch (e: Exception) {
            false
        }
    }

    private fun isHostBlocked(host: String): Boolean {
        var currentHost = host
        while (currentHost.contains(".")) {
            if (blockedDomains.contains(currentHost)) {
                return true
            }
            val dotIndex = currentHost.indexOf('.')
            if (dotIndex == -1 || dotIndex == currentHost.length - 1) {
                break
            }
            currentHost = currentHost.substring(dotIndex + 1)
        }
        return false
    }
}
