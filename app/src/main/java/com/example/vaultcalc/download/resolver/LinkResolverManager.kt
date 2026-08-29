package com.example.vaultcalc.download.resolver

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkResolverManager @Inject constructor(
    private val directLinkStrategy: DirectLinkStrategy,
    private val htmlMetadataStrategy: HtmlMetadataStrategy
) {

    suspend fun resolve(url: String): List<DownloadOption> {
        val options = mutableListOf<DownloadOption>()

        // 1. Check if it's a direct file first (HEAD request)
        val directOptions = directLinkStrategy.resolve(url)
        if (directOptions.isNotEmpty()) {
            // If it's a direct file, don't bother doing HTML parsing
            return directOptions
        }

        // 2. If not a direct file, try to parse metadata (GET request)
        val htmlOptions = htmlMetadataStrategy.resolve(url)
        options.addAll(htmlOptions)

        return options
    }
}
