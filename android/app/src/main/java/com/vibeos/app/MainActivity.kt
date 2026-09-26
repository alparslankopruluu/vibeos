package com.vibeos.app

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.vibeos.app.live.VibeLiveWallpaperService
import com.vibeos.app.services.DailyDropScheduler
import com.vibeos.app.ui.VibeOSRoot

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            DailyDropScheduler.schedule(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            getSharedPreferences("vibeos", MODE_PRIVATE)
                .getBoolean("daily_drop_notifications", false)
        ) {
            DailyDropScheduler.schedule(this)
        }

        setContent {
            VibeOSRoot(
                onApplyLiveWorld = {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(
                                this@MainActivity,
                                VibeLiveWallpaperService::class.java
                            )
                        )
                    }
                    startActivity(intent)
                },
                onRequestNotifications = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        DailyDropScheduler.schedule(this@MainActivity)
                    }
                }
            )
        }
    }
}
