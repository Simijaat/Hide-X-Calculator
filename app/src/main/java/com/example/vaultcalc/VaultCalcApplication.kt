package com.example.vaultcalc

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.vaultcalc.data.security.VaultSecurityManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.yausername.youtubedl_android.YoutubeDL
import android.util.Log

@HiltAndroidApp
class VaultCalcApplication : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var securityManager: VaultSecurityManager

    override fun onCreate() {
        super<Application>.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            Log.e("VaultCalcApp", "failed to initialize youtubedl-android", e)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        // App goes to background
        securityManager.lockVault()
        super<DefaultLifecycleObserver>.onStop(owner)
    }
}
