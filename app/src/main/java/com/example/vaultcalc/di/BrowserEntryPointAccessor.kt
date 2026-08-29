package com.example.vaultcalc.di

import android.content.Context
import com.example.vaultcalc.data.browser.AdBlockManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

object BrowserEntryPointAccessor {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BrowserEntryPoint {
        fun getAdBlockManager(): AdBlockManager
    }

    fun getAdBlockManager(context: Context): AdBlockManager {
        val appContext = context.applicationContext ?: context
        val hiltEntryPoint = EntryPointAccessors.fromApplication(appContext, BrowserEntryPoint::class.java)
        return hiltEntryPoint.getAdBlockManager()
    }
}
