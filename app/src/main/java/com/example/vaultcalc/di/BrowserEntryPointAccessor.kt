package com.example.vaultcalc.di

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

object BrowserEntryPointAccessor {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BrowserEntryPoint {
    }
}
