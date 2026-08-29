package com.example.vaultcalc.download.resolver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class DirectLinkStrategy @Inject constructor() : ResolverStrategy {

    override suspend fun resolve(url: String): List<DownloadOption> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val contentType = connection.contentType ?: "application/octet-stream"
                val contentLength = connection.contentLengthLong

                var fileName = "downloaded_file"
                val disposition = connection.getHeaderField("Content-Disposition")
                if (disposition != null && disposition.contains("filename=")) {
                    val match = Regex("filename=\"?([^\";]+)\"?").find(disposition)
                    if (match != null) {
                        fileName = match.groupValues[1]
                    }
                } else {
                     // try to get from path
                    val path = URL(url).path
                    val lastSegment = path.substringAfterLast("/")
                    if (lastSegment.isNotBlank()) {
                        fileName = lastSegment
                    }
                }

                // Consider it a direct file if it's an application, audio, video type or has a specific file disposition
                if (contentType.startsWith("application/") ||
                    contentType.startsWith("audio/") ||
                    contentType.startsWith("video/") ||
                    disposition?.contains("attachment") == true ||
                    contentType.startsWith("image/")) {

                    return@withContext listOf(
                        DownloadOption(
                            title = fileName,
                            url = url,
                            mimeType = contentType,
                            sizeBytes = if (contentLength > 0) contentLength else 0,
                            isDirect = true
                        )
                    )
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }
}
