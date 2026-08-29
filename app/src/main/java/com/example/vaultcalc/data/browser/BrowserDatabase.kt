package com.example.vaultcalc.data.browser

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Bookmark::class, HistoryItem::class], version = 1, exportSchema = false)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao
}
