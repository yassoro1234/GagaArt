package com.robotdraw.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.robotdraw.model.Document
import com.robotdraw.model.Layer
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class Tool { BRUSH, ERASER, PICKER }

/**
 * Owns the editing state for one document: current brush and colour, the viewport
 * (zoom, pan, rotation), stroke rendering, and undo/redo for pixel edits.
 */
class Editor(val doc: Document) {

    var brush by mutableStateOf(BrushSettings.PRESETS.first())
    var tool by mutableStateOf(Tool.BRUSH)
    var color by mutableStateOf(0xFF1B1F27.toInt())
        private set
    val recentColors = mutableStateListOf<Int>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    /** Called after every finished edit so the project can be autosaved. */
    var onChanged: (() -> Unit)? = null

    // ---- Viewport -----------------------------------------------------------------

    var viewW = 0f
        private set
    var viewH = 0f
        private set
    private var baseScale = 1f
    private var fitted = false

    var zoom by mutableStateOf(1f)
        private set
    var rotation by mutableStateOf(0f)
        private set
    var panX by mutableStateOf(0f)
        private set
    var panY by mutableStateOf(0f)
        private set

    val scale: Float
        get() = zoom * baseScale

    fun setViewport(width: Float, height: Float) {
        viewW = width
        viewH = height
        if (!fitted && width > 0f && height > 0f) {
            fit()
            fitted = true
        }
    }

    fun fit() {
        if (viewW <= 0f || viewH <= 0f) return
        baseScale = min(viewW / doc.width, viewH / doc.height) * 0.92f
        zoom = 1f
        rotation = 0f
        panX = 0f
        panY = 0f
    }

    fun resetRotation() {
        rotation = 0f
    }

    /** Applies a two-finger gesture while keeping the point under the fingers in place. */
    fun gesture(zoomDelta: Float, pan: Offset, rotationDelta: Float, centroid: Offset) {
        val (docX, docY) = screenToDoc(centroid.x, centroid.y)
        zoom = (zoom * zoomDelta).coerceIn(0.2f, 40f)
        rotation += rotationDelta
        val (rx, ry) = docRelative(docX, docY)
        panX = (centroid.x + pan.x - viewW / 2f) - rx
        panY = (centroid.y + pan.y - viewH / 2f) - ry
    }

    /** Converts a screen point (view pixels) into document pixel coordinates. */
    fun screenToDoc(x: Float, y: Float): Pair<Float, Float> {
        val vx = x - (viewW / 2f + panX)
        val vy = y - (viewH / 2f + panY)
        val r = Math.toRadians(-rotation.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        val ux = vx * c - vy * s
        val uy = vx * s + vy * c
        return Pair(ux / scale + doc.width / 2f, uy / scale + doc.height / 2f)
    }

    /** Document point relative to the document centre, in screen pixels, without pan. */
    private fun docRelative(px: Float, py: Float): Pair<Float, Float> {
        val dx = (px - doc.width / 2f) * scale
        val dy = (py - doc.height / 2f) * scale
        val t = Math.toRadians(rotation.toDouble())
        val c = cos(t).toFloat()
        val s = sin(t).toFloat()
        return Pair(dx * c - dy * s, dx * s + dy * c)
    }

    // ---- Colour -------------------------------------------------------------------

    fun setColor(argb: Int) {
        color = argb
        recentColors.remove(argb)
        recentColors.add(0, argb)
        while (recentColors.size > 12) recentColors.removeAt(recentColors.size - 1)
    }

    // ---- Strokes ------------------------------------------------------------------

    private class Snap(val layerId: Long, val bitmap: Bitmap)

    private val undoStack = ArrayDeque<Snap>()
    private val redoStack = ArrayDeque<Snap>()

    private var strokeLayer: Layer? = null
    private var paint: Paint? = null
    private var strokeColor = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastP = 1f
    private var carry = 0f

    /** Starts a stroke at a screen point. Returns false if the active layer cannot be drawn on. */
    fun beginStroke(screenX: Float, screenY: Float, pressure: Float): Boolean {
        if (tool == Tool.PICKER) return false
        val layer = doc.active ?: return false
        if (layer.locked || !layer.visible) return false

        pushUndo(layer)
        strokeLayer = layer

        val erasing = tool == Tool.ERASER || brush.erase
        strokeColor = if (erasing) 0xFF000000.toInt() else (color or 0xFF000000.toInt())
        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = strokeColor
            if (erasing) xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }

        val (x, y) = screenToDoc(screenX, screenY)
        lastX = x
        lastY = y
        lastP = pressure
        carry = 0f
        stampAt(Canvas(layer.bitmap), x, y, pressure)
        doc.touch()
        return true
    }

    /** Extends the current stroke. Smoothing pulls the stroke toward the finger over time. */
    fun moveStroke(screenX: Float, screenY: Float, pressure: Float) {
        val layer = strokeLayer ?: return
        val (rawX, rawY) = screenToDoc(screenX, screenY)
        val k = 1f - brush.smoothing.coerceIn(0f, 0.95f)
        val nx = lastX + (rawX - lastX) * k
        val ny = lastY + (rawY - lastY) * k
        val np = lastP + (pressure - lastP) * k
        drawSegment(Canvas(layer.bitmap), lastX, lastY, lastP, nx, ny, np)
        lastX = nx
        lastY = ny
        lastP = np
        doc.touch()
    }

    fun endStroke() {
        if (strokeLayer == null) return
        strokeLayer = null
        paint = null
        doc.touch()
        onChanged?.invoke()
    }

    private fun drawSegment(
        canvas: Canvas,
        x0: Float, y0: Float, p0: Float,
        x1: Float, y1: Float, p1: Float
    ) {
        val dx = x1 - x0
        val dy = y1 - y0
        val dist = sqrt(dx * dx + dy * dy)
        val step = maxOf(0.5f, brush.size * brush.spacing)
        var d = carry
        while (d <= dist) {
            val t = if (dist == 0f) 0f else d / dist
            stampAt(canvas, x0 + dx * t, y0 + dy * t, p0 + (p1 - p0) * t)
            d += step
        }
        carry = d - dist
    }

    private fun stampAt(canvas: Canvas, x: Float, y: Float, pressure: Float) {
        val p = paint ?: return
        val pr = if (pressure <= 0f) 1f else pressure.coerceIn(0f, 1f)
        val radius = maxOf(0.5f, brush.size * (0.3f + 0.7f * pr) / 2f)
        p.alpha = (brush.opacity.coerceIn(0f, 1f) * 255f).toInt()
        if (brush.hardness >= 0.99f) {
            p.shader = null
            p.color = strokeColor
        } else {
            val h = brush.hardness.coerceIn(0.01f, 0.99f)
            p.shader = RadialGradient(
                x, y, radius,
                intArrayOf(strokeColor, strokeColor and 0x00FFFFFF),
                floatArrayOf(h, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(x, y, radius, p)
    }

    // ---- Picking ------------------------------------------------------------------

    /** Samples the active layer under a screen point (eyedropper). */
    fun pick(screenX: Float, screenY: Float): Boolean {
        val layer = doc.active ?: return false
        val (x, y) = screenToDoc(screenX, screenY)
        val ix = x.toInt()
        val iy = y.toInt()
        if (ix !in 0 until doc.width || iy !in 0 until doc.height) return false
        val pixel = layer.bitmap.getPixel(ix, iy)
        if (AndroidColor.alpha(pixel) == 0) return false
        setColor(pixel or 0xFF000000.toInt())
        return true
    }

    // ---- Undo / redo --------------------------------------------------------------

    private fun pushUndo(layer: Layer) {
        undoStack.addLast(Snap(layer.id, layer.bitmap.copy(Bitmap.Config.ARGB_8888, false)))
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
        refreshHistory()
    }

    fun undo() {
        val snap = undoStack.removeLastOrNull() ?: return
        val layer = doc.layers.firstOrNull { it.id == snap.layerId } ?: return
        redoStack.addLast(Snap(layer.id, layer.bitmap.copy(Bitmap.Config.ARGB_8888, false)))
        restore(layer, snap.bitmap)
        afterHistoryChange()
    }

    fun redo() {
        val snap = redoStack.removeLastOrNull() ?: return
        val layer = doc.layers.firstOrNull { it.id == snap.layerId } ?: return
        undoStack.addLast(Snap(layer.id, layer.bitmap.copy(Bitmap.Config.ARGB_8888, false)))
        restore(layer, snap.bitmap)
        afterHistoryChange()
    }

    /** Clears the active layer's pixels (undoable). */
    fun clearActiveLayer() {
        val layer = doc.active ?: return
        if (layer.locked) return
        pushUndo(layer)
        Canvas(layer.bitmap).drawColor(0, PorterDuff.Mode.CLEAR)
        afterHistoryChange()
    }

    private fun restore(layer: Layer, source: Bitmap) {
        val canvas = Canvas(layer.bitmap)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(source, 0f, 0f, null)
    }

    private fun afterHistoryChange() {
        refreshHistory()
        doc.touch()
        onChanged?.invoke()
    }

    private fun refreshHistory() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    companion object {
        const val MAX_UNDO = 10
    }
}
