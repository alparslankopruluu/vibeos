package com.vibeos.app.services

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import com.vibeos.app.model.ThemePack

object ThemeInstaller {
    fun applyGeneratedWallpaper(context: Context, theme: ThemePack): Result<Unit> = runCatching {
        val dm = context.resources.displayMetrics
        val width = dm.widthPixels.coerceAtLeast(1080)
        val height = dm.heightPixels.coerceAtLeast(1920)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                Color.rgb(5, 6, 12),
                theme.accentA.toInt(),
                theme.accentB.toInt(),
                Color.rgb(5, 6, 12)
            ),
            floatArrayOf(0f, .34f, .7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.shader = null
        repeat(8) { i ->
            paint.color = Color.argb(22 + i * 4, 255, 255, 255)
            canvas.drawCircle(
                width * (.15f + (i % 4) * .25f),
                height * (.16f + (i / 4) * .52f),
                width * (.08f + (i % 3) * .035f),
                paint
            )
        }

        WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        bitmap.recycle()
    }
}
