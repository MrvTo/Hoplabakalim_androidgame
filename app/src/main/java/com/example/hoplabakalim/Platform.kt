package com.example.hoplabakalim

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.util.Random

class Platform(x: Float, y: Float, width: Float, height: Float, private val bitmap: Bitmap) {
    private var rect: RectF
    private val paint: Paint

    init {
        rect = RectF(x, y, x + width, y + height)
        paint = Paint()
        // Renk yerine resim kullanıldığı için Paint nesnesinin rengi önemli değil
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, null, rect, paint) // Bitmap'i çiz
    }

    fun getRect(): RectF {
        return rect
    }

    fun setY(y: Float) {
        rect.offsetTo(rect.left, y)
    }

    fun setX(x: Float) {
        rect.offsetTo(x, rect.top)
    }

    fun getY(): Float {
        return rect.top
    }

    fun getHeight(): Float {
        return rect.height()
    }

    fun getWidth(): Float {
        return rect.width()
    }
}
