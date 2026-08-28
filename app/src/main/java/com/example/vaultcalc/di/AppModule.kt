package com.example.vaultcalc.di

import android.content.Context
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
}
