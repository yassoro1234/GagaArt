package com.gagaart.app.engine.project

import android.graphics.Bitmap
import com.gagaart.app.engine.layer.LayerManager
import java.util.UUID

data class CanvasSize(
    val width: Int,
    val height: Int,
    val name: String
) {
    companion object {
        val A4 = CanvasSize(2480, 3508, "A4")
        val A5 = CanvasSize(1748, 2480, "A5")
        val A3 = CanvasSize(3508, 4961, "A3")
        val SQUARE = CanvasSize(2048, 2048, "Square")
        val PORTRAIT = CanvasSize(1080, 1920, "Portrait")
        val LANDSCAPE = CanvasSize(1920, 1080, "Landscape")

        val PRESETS = listOf(A4, A5, A3, SQUARE, PORTRAIT, LANDSCAPE)

        fun custom(width: Int, height: Int) = CanvasSize(width, height, "Custom")
    }
}

enum class CanvasBackground {
    TRANSPARENT,
    WHITE,
    BLACK,
    TEXTURED
}

/**
 * Represents a full Gaga Art project.
 */
class Project(
    val id: String = UUID.randomUUID().toString(),
    val canvasSize: CanvasSize,
    val background: CanvasBackground = CanvasBackground.WHITE,
    name: String = "Untitled"
) {
    var name: String = name
    val layerManager = LayerManager(canvasSize.width, canvasSize.height)
    var isDirty: Boolean = false

    fun markDirty() {
        isDirty = true
    }

    fun exportBitmap(): Bitmap = layerManager.composite()
}
