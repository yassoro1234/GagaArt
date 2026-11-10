package com.gagaart.app.engine.layer

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Manages the stack of layers for a project.
 */
class LayerManager(
    private val canvasWidth: Int,
    private val canvasHeight: Int
) {
    private val _layers = mutableStateListOf<Layer>()
    val layers: List<Layer> get() = _layers

    var activeLayerIndex by mutableStateOf(0)
        private set

    val activeLayer: Layer?
        get() = _layers.getOrNull(activeLayerIndex)

    init {
        // Start with one transparent layer
        addLayer("Background")
    }

    fun addLayer(name: String = "Layer ${_layers.size + 1}"): Layer {
        val layer = Layer(width = canvasWidth, height = canvasHeight, name = name)
        _layers.add(layer)
        activeLayerIndex = _layers.lastIndex
        return layer
    }

    fun deleteLayer(index: Int) {
        if (_layers.size <= 1) return // keep at least one layer
        if (index !in _layers.indices) return
        _layers.removeAt(index)
        if (activeLayerIndex >= _layers.size) {
            activeLayerIndex = _layers.lastIndex
        } else if (activeLayerIndex > index) {
            activeLayerIndex--
        }
    }

    fun duplicateLayer(index: Int) {
        val original = _layers.getOrNull(index) ?: return
        val copy = original.duplicate()
        _layers.add(index + 1, copy)
        activeLayerIndex = index + 1
    }

    fun setActiveLayer(index: Int) {
        if (index in _layers.indices) {
            activeLayerIndex = index
        }
    }

    fun moveLayer(from: Int, to: Int) {
        if (from !in _layers.indices || to !in _layers.indices) return
        val layer = _layers.removeAt(from)
        _layers.add(to, layer)
        activeLayerIndex = to
    }

    fun mergeDown(index: Int) {
        if (index <= 0 || index >= _layers.size) return
        val upper = _layers[index]
        val lower = _layers[index - 1]
        val paint = Paint().apply { alpha = (upper.opacity * 255).toInt() }
        AndroidCanvas(lower.bitmap).drawBitmap(upper.bitmap, 0f, 0f, paint)
        _layers.removeAt(index)
        activeLayerIndex = index - 1
    }

    /**
     * Composites all visible layers into a single bitmap.
     */
    fun composite(): Bitmap {
        val result = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(result)
        val paint = Paint()

        for (layer in _layers) {
            if (!layer.isVisible) continue
            paint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
        }
        return result
    }

    fun clearAll() {
        _layers.forEach { it.clear() }
    }
}
