package com.example.vaultcalc.download.resolver

import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YoutubeDLResolver {
    suspend fun resolve(url: String): List<DownloadOption> = withContext(Dispatchers.IO) {
        val options = mutableListOf<DownloadOption>()
        try {
            val info = YoutubeDL.getInstance().getInfo(url)
            info.formats?.forEach { format ->
                options.add(
                    DownloadOption(
                        url = format.url ?: return@forEach,
                        title = info.title ?: "Downloaded_Video",
                        mimeType = format.ext ?: "video/mp4",
                        sizeBytes = format.fileSize.takeIf { it > 0 } ?: format.fileSizeApproximate.takeIf { it > 0 } ?: -1L,
                        quality = format.formatNote ?: format.format ?: "Unknown",
                        isDirect = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("YoutubeDLResolver", "Failed to resolve URL: $url", e)
        }
        options.distinctBy { it.quality }
    }
}
