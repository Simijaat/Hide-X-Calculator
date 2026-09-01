package com.example.vaultcalc.di

import android.content.Context
import androidx.room.Room
import com.example.vaultcalc.data.browser.BrowserDao
import com.example.vaultcalc.data.browser.BrowserDatabase
import com.example.vaultcalc.data.browser.BrowserRepository
import com.example.vaultcalc.data.download.DownloadDao
import com.example.vaultcalc.data.download.DownloadDatabase
import com.example.vaultcalc.data.download.DownloadRepository
import com.example.vaultcalc.data.security.VaultSecurityManager
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
    fun provideVaultSecurityManager(
        @ApplicationContext context: Context
    ): VaultSecurityManager {
        return VaultSecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideBrowserDatabase(@ApplicationContext context: Context): BrowserDatabase {
        return Room.databaseBuilder(
            context,
            BrowserDatabase::class.java,
            "browser_db"
        ).build()
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
