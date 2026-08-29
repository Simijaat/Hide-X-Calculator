package com.example.vaultcalc.download.resolver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class HtmlMetadataStrategy @Inject constructor() : ResolverStrategy {

    override suspend fun resolve(url: String): List<DownloadOption> = withContext(Dispatchers.IO) {
        val options = mutableListOf<DownloadOption>()
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            // Basic user agent to get standard responses instead of mobile-lite ones sometimes
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            connection.connect()

            val contentType = connection.contentType ?: ""
            if (connection.responseCode in 200..299 && contentType.contains("text/html")) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                var line: String?

                // Read a limited amount of HTML to find standard metadata tags (prevent reading massive files)
                var linesRead = 0
                val maxLines = 1000

                val ogVideoRegex = Regex("<meta\\s+property=\"og:video:url\"\\s+content=\"([^\"]+)\"")
                val ogVideoAltRegex = Regex("<meta\\s+property=\"og:video\"\\s+content=\"([^\"]+)\"")
                val twitterPlayerStream = Regex("<meta\\s+name=\"twitter:player:stream\"\\s+content=\"([^\"]+)\"")
                val titleRegex = Regex("<title>([^<]+)</title>")

                var title = "Extracted Media"

                while (reader.readLine().also { line = it } != null && linesRead < maxLines) {
                    val l = line!!

                    titleRegex.find(l)?.let { title = it.groupValues[1] }

                    listOf(ogVideoRegex, ogVideoAltRegex, twitterPlayerStream).forEach { regex ->
                        regex.find(l)?.let { match ->
                            val mediaUrl = match.groupValues[1].replace("&amp;", "&")
                            options.add(
                                DownloadOption(
                                    title = "$title (Video)",
                                    url = mediaUrl,
                                    mimeType = "video/mp4", // default guess, will be resolved by head request when downloading
                                    sizeBytes = 0,
                                    isDirect = false
                                )
                            )
                        }
                    }
                    linesRead++
                }
                reader.close()
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Deduplicate URLs
        return@withContext options.distinctBy { it.url }
    }
}
