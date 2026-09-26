package com.vibeos.app.live

import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import kotlin.math.sin

class VibeLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VibeEngine()

    inner class VibeEngine : Engine(), SensorEventListener {
        private val handler = Handler(Looper.getMainLooper())
        private val sensorManager = getSystemService(SensorManager::class.java)
        private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private var visible = false
        private var phase = 0f
        private var touchX = .5f
        private var touchY = .5f
        private var tiltX = 0f
        private var tiltY = 0f

        private val worldId: String =
            getSharedPreferences("vibeos", MODE_PRIVATE)
                .getString("live_world_id", "ocean")
                ?: "ocean"

        init {
            setTouchEventsEnabled(true)
        }

        private val drawFrame = object : Runnable {
            override fun run() {
                draw()
                if (visible) handler.postDelayed(this, 33)
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(drawFrame)

            if (isVisible) {
                accelerometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                }
                handler.post(drawFrame)
            } else {
                sensorManager.unregisterListener(this)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            sensorManager.unregisterListener(this)
            handler.removeCallbacks(drawFrame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
                val frame = surfaceHolder.surfaceFrame
                touchX = event.x / frame.width().coerceAtLeast(1)
                touchY = event.y / frame.height().coerceAtLeast(1)
            }
            super.onTouchEvent(event)
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            tiltX = (-event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
            tiltY = (event.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

        private fun palette(): IntArray = when (worldId) {
            "night_drive" -> intArrayOf(
                Color.rgb(4, 5, 10),
                Color.rgb(82, 13, 56),
                Color.rgb(205, 32, 74)
            )
            "galaxy" -> intArrayOf(
                Color.rgb(2, 7, 28),
                Color.rgb(45, 31, 128),
                Color.rgb(185, 47, 218)
            )
            "forest" -> intArrayOf(
                Color.rgb(3, 20, 17),
                Color.rgb(7, 84, 61),
                Color.rgb(20, 156, 111)
            )
            else -> intArrayOf(
                Color.rgb(2, 10, 30),
                Color.rgb(26, 69, 144),
                Color.rgb(4, 154, 190)
            )
        }

        private fun draw() {
            val canvas = runCatching { surfaceHolder.lockCanvas() }.getOrNull() ?: return

            try {
                phase += .025f
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                val colors = palette()

                paint.shader = LinearGradient(
                    0f,
                    0f,
                    w,
                    h,
                    colors,
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, paint)
                paint.shader = null

                val motionX = tiltX * 42f
                val motionY = tiltY * 32f

                repeat(36) { i ->
                    val x = ((i * 137f + phase * (120 + i % 7)) % (w + 80f)) - 40f
                    val y = (
                        h * ((i % 13) / 13f) +
                            sin(phase + i) * 28f +
                            touchY * 20f
                        ) % h

                    val alpha = 65 + (i % 4) * 34
                    paint.color = when (worldId) {
                        "night_drive" -> Color.argb(alpha, 255, 98, 145)
                        "forest" -> Color.argb(alpha, 120, 255, 190)
                        "galaxy" -> Color.argb(alpha, 196, 140, 255)
                        else -> Color.argb(alpha, 120, 225, 255)
                    }

                    canvas.drawCircle(
                        x + (touchX - .5f) * (i % 5) * 18f + motionX * (1f + i % 3),
                        y + motionY * (1f + i % 4),
                        2f + (i % 4),
                        paint
                    )
                }

                paint.color = Color.argb(50, 255, 255, 255)
                canvas.drawCircle(
                    w * touchX + motionX * 2f,
                    h * touchY + motionY * 2f,
                    120f + sin(phase) * 16f,
                    paint
                )
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
