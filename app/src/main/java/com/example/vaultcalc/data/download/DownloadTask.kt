package com.example.vaultcalc.data.download

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadTask(
    @PrimaryKey val id: String, // UUID
    val originalUrl: String,
    val resolvedUrl: String?,
    val fileName: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val state: DownloadState,
    val filePath: String?,
    val createdAt: Long,
    val errorReason: String?
)
