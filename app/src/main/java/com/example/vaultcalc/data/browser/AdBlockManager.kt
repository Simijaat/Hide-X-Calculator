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

    private val exactDomains = HashSet<String>()
    private val wildcardDomains = ArrayList<String>()
    private val keywordBlocks = ArrayList<String>()
    
    var isEnabled = true
        private set

    suspend fun loadRules() {
        withContext(Dispatchers.IO) {
            try {
                // We parse standard hosts format or basic EasyList syntax (e.g., ||domain.com^)
                val inputStream = context.assets.open("ad_hosts.txt")
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim()
                    if (!trimmed.isNullOrEmpty() && !trimmed.startsWith("!") && !trimmed.startsWith("#")) {
                        
                        if (trimmed.startsWith("||") && trimmed.endsWith("^")) {
                            val domain = trimmed.substring(2, trimmed.length - 1)
                            exactDomains.add(domain)
                        } else if (trimmed.startsWith("||")) {
                            val domain = trimmed.substring(2)
                            wildcardDomains.add(domain)
                        } else if (trimmed.contains("*")) {
                            wildcardDomains.add(trimmed.replace("*", ""))
                        } else if (!trimmed.contains(".")) {
                            keywordBlocks.add(trimmed)
                        } else {
                            // Standard host format: 0.0.0.0 domain.com
                            val parts = trimmed.split(Regex("\\s+"))
                            val domain = if (parts.size > 1) parts[1] else parts[0]
                            exactDomains.add(domain)
                        }
                    }
                }
                reader.close()
                
                // Add some default aggressive keyword blocks
                keywordBlocks.add("/ad/")
                keywordBlocks.add("/ads/")
                keywordBlocks.add("/tracking/")
                keywordBlocks.add("/analytics/")
                keywordBlocks.add("google-analytics.com")
                keywordBlocks.add("doubleclick.net")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isAd(url: String, currentUrl: String = ""): Boolean {
        if (!isEnabled) return false
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            isHostBlocked(host) || isWildcardBlocked(host) || isKeywordBlocked(url.lowercase())
        } catch (e: Exception) {
            false
        }
    }

    private fun isHostBlocked(host: String): Boolean {
        var currentHost = host
        while (currentHost.contains(".")) {
            if (exactDomains.contains(currentHost)) {
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

    private fun isWildcardBlocked(host: String): Boolean {
        for (pattern in wildcardDomains) {
            if (host.contains(pattern)) return true
        }
        return false
    }

    private fun isKeywordBlocked(url: String): Boolean {
        for (keyword in keywordBlocks) {
            if (url.contains(keyword)) return true
        }
        return false
    }
}
