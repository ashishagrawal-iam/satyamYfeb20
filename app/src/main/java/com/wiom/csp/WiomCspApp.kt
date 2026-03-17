package com.wiom.csp

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WiomCspApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WiomCSP initialized")
    }

    companion object {
        private const val TAG = "WiomCspApp"
    }
}
