package com.example.vaultcalc.data.download

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadTask::class], version = 1, exportSchema = false)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
