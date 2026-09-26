package com.vibeos.app

import android.app.Application
import com.vibeos.app.services.AppServices

class VibeOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppServices.initialize(this)
    }
}
