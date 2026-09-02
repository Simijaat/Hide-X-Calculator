package com.example.vaultcalc.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vaultcalc.data.browser.BrowserDao
import com.example.vaultcalc.data.browser.BrowserDatabase
import com.example.vaultcalc.data.browser.BrowserRepository
import com.example.vaultcalc.data.download.DownloadDao
import com.example.vaultcalc.data.download.DownloadDatabase
import com.example.vaultcalc.data.download.DownloadRepository
import com.example.vaultcalc.data.security.VaultSecurityManager
import com.example.vaultcalc.data.crypto.VaultStorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideVaultStorageManager(@ApplicationContext context: Context): VaultStorageManager {
        return VaultStorageManager(context)
    }

    @Provides
    @Singleton
    fun provideVaultSecurityManager(
        @ApplicationContext context: Context,
        vaultStorageManager: VaultStorageManager
    ): VaultSecurityManager {
        return VaultSecurityManager(context, vaultStorageManager)
    }

    @Provides
    @Singleton
    fun provideBrowserDatabase(@ApplicationContext context: Context): BrowserDatabase {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tabs` (`id` TEXT NOT NULL, `url` TEXT NOT NULL, `title` TEXT NOT NULL, `isLoading` INTEGER NOT NULL, `progress` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        return Room.databaseBuilder(
            context,
            BrowserDatabase::class.java,
            "browser_db"
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }

    @Provides
    @Singleton
    fun provideBrowserDao(database: BrowserDatabase): BrowserDao {
        return database.browserDao()
    }

    @Provides
    @Singleton
    fun provideBrowserRepository(browserDao: BrowserDao): BrowserRepository {
        return BrowserRepository(browserDao)
    }

    @Provides
    @Singleton
    fun provideDownloadDatabase(@ApplicationContext context: Context): DownloadDatabase {
        return Room.databaseBuilder(
            context,
            DownloadDatabase::class.java,
            "download_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(database: DownloadDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    @Singleton
    fun provideDownloadRepository(downloadDao: DownloadDao): DownloadRepository {
        return DownloadRepository(downloadDao)
    }
}
