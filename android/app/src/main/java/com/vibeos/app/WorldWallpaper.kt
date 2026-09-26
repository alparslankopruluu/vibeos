package com.vibeos.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.sin

object WorldPainter {
    fun draw(canvas: Canvas, palette: List<Int>, world: String, time: Float, tilt: Float, touch: Float, artwork: android.graphics.Bitmap? = null) {
        val w = canvas.width.toFloat(); val h = canvas.height.toFloat()
        canvas.drawColor(palette[0])
        val paint = Paint(3)
        if (artwork != null) canvas.drawBitmap(artwork, null, android.graphics.Rect(0, 0, canvas.width, canvas.height), paint)
        else {
            paint.shader = android.graphics.LinearGradient(0f, 0f, w, h, palette[0], palette[1], android.graphics.Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        }
        paint.color = palette[2]
        val density = if (world == "space") 60 else 32
        for (i in 0 until density) {
            val seed = (i * 73 % 101) / 101f
            val x = ((i * 137 % 103) / 103f * w + tilt * (10 + i % 18) + touch * sin(i.toFloat())) % w
            val y = ((i * 71 % 97) / 97f * h + time * (if (world == "sakura") 9 else 1) * (1 + seed)) % h
            paint.alpha = (80 + ((sin(time + i) + 1) * 55)).toInt().coerceIn(0,255)
            canvas.drawCircle(x, y, if (world == "space") (1 + i % 3).toFloat() else (3 + i % 6).toFloat(), paint)
        }
        paint.alpha = if (artwork == null) 110 else 35
        if (world == "ocean") {
            for (j in 0..4) {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
                val y = h * (.57f + j * .08f)
                val path = android.graphics.Path().apply { moveTo(0f, y); for (x in 0..w.toInt() step 12) lineTo(x.toFloat(), y + sin(x / 42f + time + j) * 11) }
                canvas.drawPath(path, paint)
            }
        } else {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(w * .55f + tilt * 6, h * .39f, w * .27f, paint)
        }
    }
}

class WorldWallpaper : WallpaperService() {
    override fun onCreateEngine(): Engine = WorldEngine()
    inner class WorldEngine : Engine(), SensorEventListener {
        private val handler = Handler(Looper.getMainLooper())
        private val sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private var visible = false; private var tilt = 0f; private var touch = 0f; private var start = System.currentTimeMillis()
        private var artworkId: String? = null; private var artwork: android.graphics.Bitmap? = null
        private val tick = object : Runnable { override fun run() { drawFrame(); if (visible) handler.postDelayed(this, 33) } }
        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible; handler.removeCallbacks(tick)
            if (isVisible) { sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }; tick.run() }
            else sensors.unregisterListener(this)
        }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(tick)
            sensors.unregisterListener(this)
            artwork?.recycle()
            artwork = null
            super.onSurfaceDestroyed(holder)
        }
        override fun onTouchEvent(event: android.view.MotionEvent) { if (event.action == android.view.MotionEvent.ACTION_DOWN) touch += 1f }
        override fun onSensorChanged(event: SensorEvent) { tilt = tilt * .85f + event.values[0] * .15f }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        private fun drawFrame() {
            val holder = surfaceHolder; var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                val id = getSharedPreferences("vibe", 0).getString("world", "space") ?: "space"
                val theme = Catalog.bundled(this@WorldWallpaper).firstOrNull { it.world == id } ?: Catalog.bundled(this@WorldWallpaper).first()
                if (artworkId != theme.id) {
                    artwork?.recycle(); artworkId = theme.id
                    artwork = runCatching { assets.open("${theme.id}.png").use { android.graphics.BitmapFactory.decodeStream(it) } }.getOrNull()
                }
                WorldPainter.draw(canvas, theme.colors, id, (System.currentTimeMillis() - start) / 1000f, tilt, touch, artwork)
            } catch (e: Exception) { Telemetry.error(this@WorldWallpaper, e) }
            finally { canvas?.let { holder.unlockCanvasAndPost(it) } }
        }
    }
}
