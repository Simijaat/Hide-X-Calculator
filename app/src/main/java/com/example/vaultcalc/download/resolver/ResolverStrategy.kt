package com.example.vaultcalc.download.resolver

interface ResolverStrategy {
    suspend fun resolve(url: String): List<DownloadOption>
}
