package com.example.vaultcalc.download.resolver

data class DownloadOption(
    val title: String,
    val url: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isDirect: Boolean
)
