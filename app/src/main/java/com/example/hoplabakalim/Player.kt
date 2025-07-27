package com.example.hoplabakalim

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class Player(x: Float, y: Float, width: Float, height: Float, private val bitmap: Bitmap) {
    private var rect: RectF
    private val paint: Paint
    var velocityY: Float = 0f
    var velocityX: Float = 0f
    private val gravity = 0.5f
    private val originalHeight: Float = height // Karakterin orijinal yüksekliğini sakla

    init {
        rect = RectF(x, y, x + width, y + height)
        paint = Paint()
        // Artık renk yerine resim kullanıldığı için Paint nesnesinin rengi önemli değil
    }

    fun jump(power: Float) {
        velocityY = power
    }

    fun move(screenWidth: Int) {
        rect.offset(velocityX, -velocityY)
        velocityY -= gravity

        if (rect.left < 0) {
            rect.offsetTo(0f, rect.top)
        }
        if (rect.right > screenWidth) {
            rect.offsetTo(screenWidth - rect.width(), rect.top)
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, null, rect, paint) // Bitmap'i çiz
    }

    fun getRect(): RectF {
        return rect
    }

    fun setPosition(x: Float, y: Float) {
        rect.set(x, y, x + rect.width(), y + originalHeight)
    }

    fun getHeight(): Float {
        return originalHeight
    }
}
