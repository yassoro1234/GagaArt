package com.robotdraw.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** One raster layer of a document. Pixels live in [bitmap]; the rest is observable UI state. */
class Layer(val id: Long, name: String, val bitmap: Bitmap) {
    var name by mutableStateOf(name)
    var visible by mutableStateOf(true)
    var locked by mutableStateOf(false)
    var opacity by mutableStateOf(1f)
}

/**
 * A drawing document: fixed pixel size, optional solid background (0 = transparent),
 * and an ordered list of layers (index 0 is the bottom).
 */
class Document(
    val id: String,
    val width: Int,
    val height: Int,
    val background: Int
) {
    var name by mutableStateOf("Untitled")
    val layers = mutableStateListOf<Layer>()
    var activeId by mutableStateOf(-1L)

    /** Incremented whenever pixels change, so the canvas knows to redraw. */
    var revision by mutableStateOf(0)

    val active: Layer?
        get() = layers.firstOrNull { it.id == activeId }

    fun touch() {
        revision++
    }

    private fun nextId(): Long = (layers.maxOfOrNull { it.id } ?: 0L) + 1L

    fun addLayer(name: String? = null, bitmap: Bitmap? = null): Layer {
        val pixels = bitmap ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val layer = Layer(nextId(), name ?: "Layer ${layers.size + 1}", pixels)
        val index = layers.indexOfFirst { it.id == activeId }
        if (index >= 0) layers.add(index + 1, layer) else layers.add(layer)
        activeId = layer.id
        touch()
        return layer
    }

    fun duplicate(layer: Layer): Layer {
        val copy = Layer(
            nextId(),
            "${layer.name} copy",
            layer.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        )
        copy.opacity = layer.opacity
        val index = layers.indexOf(layer)
        layers.add(index + 1, copy)
        activeId = copy.id
        touch()
        return copy
    }

    fun remove(layer: Layer) {
        val index = layers.indexOf(layer)
        if (index < 0 || layers.size <= 1) return
        layers.removeAt(index)
        if (activeId == layer.id) {
            activeId = layers[(index - 1).coerceAtLeast(0)].id
        }
        touch()
    }

    fun move(layer: Layer, delta: Int) {
        val from = layers.indexOf(layer)
        val to = from + delta
        if (from < 0 || to !in layers.indices) return
        layers.removeAt(from)
        layers.add(to, layer)
        touch()
    }

    fun mergeDown(layer: Layer) {
        val index = layers.indexOf(layer)
        if (index <= 0) return
        val lower = layers[index - 1]
        val canvas = Canvas(lower.bitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
        layers.removeAt(index)
        activeId = lower.id
        touch()
    }
}
