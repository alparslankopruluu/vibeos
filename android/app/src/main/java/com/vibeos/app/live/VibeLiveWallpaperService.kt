package com.vibeos.app.live

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import kotlin.math.sin

class VibeLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VibeEngine()

    inner class VibeEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var phase = 0f
        private var touchX = .5f
        private var touchY = .5f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val drawFrame = object : Runnable {
            override fun run() {
                draw()
                if (visible) handler.postDelayed(this, 33)
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(drawFrame)
            if (visible) handler.post(drawFrame)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
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

        private fun draw() {
            val canvas = runCatching { surfaceHolder.lockCanvas() }.getOrNull() ?: return
            try {
                phase += .025f
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                paint.shader = LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(Color.rgb(2, 10, 30), Color.rgb(26, 18, 100), Color.rgb(4, 154, 190)),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, paint)
                paint.shader = null

                repeat(32) { i ->
                    val x = ((i * 137f + phase * (120 + i % 7)) % (w + 80f)) - 40f
                    val y = (h * ((i % 13) / 13f) + sin(phase + i) * 28f + touchY * 20f) % h
                    paint.color = Color.argb(80 + (i % 3) * 35, 120, 225, 255)
                    canvas.drawCircle(x + (touchX - .5f) * (i % 5) * 18f, y, 2f + (i % 4), paint)
                }

                paint.color = Color.argb(55, 80, 230, 255)
                canvas.drawCircle(w * touchX, h * touchY, 120f + sin(phase) * 16f, paint)
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
