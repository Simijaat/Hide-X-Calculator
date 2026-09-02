package com.example.vaultcalc

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.vaultcalc.data.security.VaultSecurityManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VaultCalcApplication : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var securityManager: VaultSecurityManager

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        // App goes to background
        securityManager.lockVault()
        super<DefaultLifecycleObserver>.onStop(owner)
    }
}
