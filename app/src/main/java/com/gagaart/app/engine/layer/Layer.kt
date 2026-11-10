package com.gagaart.app.engine.layer

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import java.util.UUID

/**
 * A single drawing layer with its own bitmap and properties.
 */
class Layer(
    val id: String = UUID.randomUUID().toString(),
    val width: Int,
    val height: Int,
    name: String = "Layer",
    initialBitmap: Bitmap? = null
) {
    var name by mutableStateOf(name)
    var isVisible by mutableStateOf(true)
    var isLocked by mutableStateOf(false)
    var opacity by mutableFloatStateOf(1f)

    val bitmap: Bitmap = initialBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    private val androidCanvas = AndroidCanvas(bitmap)
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    fun getImageBitmap() = bitmap.asImageBitmap()

    fun clear() {
        androidCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), clearPaint)
    }

    fun drawPath(
        path: android.graphics.Path,
        paint: Paint
    ) {
        if (isLocked || !isVisible) return
        androidCanvas.drawPath(path, paint)
    }

    fun drawPoint(x: Float, y: Float, paint: Paint) {
        if (isLocked || !isVisible) return
        androidCanvas.drawPoint(x, y, paint)
    }

    fun duplicate(newName: String = "$name Copy"): Layer {
        return Layer(
            width = width,
            height = height,
            name = newName,
            initialBitmap = bitmap
        )
    }
}
