package com.gagaart.app.engine

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.gagaart.app.engine.brush.BrushSettings
import com.gagaart.app.engine.brush.BrushType
import com.gagaart.app.engine.layer.Layer
import com.gagaart.app.engine.project.Project
import com.gagaart.app.engine.tool.DrawingTool
import com.gagaart.app.engine.tool.EraserType
import java.util.ArrayDeque

/**
 * Core drawing engine that handles stroke recording, undo/redo and tool application.
 */
class DrawingEngine(val project: Project) {

    var currentTool by mutableStateOf(DrawingTool.BRUSH)
    val brushSettings = BrushSettings()
    var eraserType by mutableStateOf(EraserType.NORMAL)
    var eraserSize by mutableStateOf(30f)
    var eraserOpacity by mutableStateOf(1f)

    // Undo / Redo stacks store snapshots of the active layer bitmap
    private val undoStack = ArrayDeque<ByteArray>()
    private val redoStack = ArrayDeque<ByteArray>()
    private val maxHistory = 30

    private var currentPath: Path? = null
    private var lastPoint: Offset? = null

    private fun activeLayer(): Layer? = project.layerManager.activeLayer

    fun startStroke(point: Offset) {
        val layer = activeLayer() ?: return
        if (layer.isLocked) return

        saveUndoState()
        currentPath = Path().apply { moveTo(point.x, point.y) }
        lastPoint = point
        project.markDirty()
    }

    fun continueStroke(point: Offset) {
        val path = currentPath ?: return
        val layer = activeLayer() ?: return
        if (layer.isLocked) return

        val last = lastPoint ?: point
        // Smooth quadratic curve
        val midX = (last.x + point.x) / 2f
        val midY = (last.y + point.y) / 2f
        path.quadTo(last.x, last.y, midX, midY)

        val paint = createPaint()
        layer.drawPath(path, paint)

        // For continuous drawing we keep a short path segment
        currentPath = Path().apply { moveTo(midX, midY) }
        lastPoint = point
    }

    fun endStroke() {
        currentPath = null
        lastPoint = null
    }

    private fun createPaint(): Paint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        when (currentTool) {
            DrawingTool.BRUSH -> {
                val settings = brushSettings
                paint.color = settings.color.copy(alpha = settings.opacity).toArgb()
                paint.strokeWidth = settings.size
                // Softness approximation via blur-like alpha (simplified)
                if (settings.hardness < 0.5f) {
                    paint.maskFilter = android.graphics.BlurMaskFilter(
                        settings.size * (1f - settings.hardness) * 0.4f,
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                    )
                }
            }
            DrawingTool.ERASER -> {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                paint.strokeWidth = eraserSize
                paint.alpha = (eraserOpacity * 255).toInt()
                if (eraserType == EraserType.SOFT) {
                    paint.maskFilter = android.graphics.BlurMaskFilter(
                        eraserSize * 0.3f,
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                    )
                }
            }
            else -> {
                paint.color = brushSettings.color.toArgb()
                paint.strokeWidth = brushSettings.size
            }
        }
        return paint
    }

    private fun saveUndoState() {
        val layer = activeLayer() ?: return
        try {
            val stream = java.io.ByteArrayOutputStream()
            layer.bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            undoStack.addLast(stream.toByteArray())
            if (undoStack.size > maxHistory) undoStack.removeFirst()
            redoStack.clear()
        } catch (e: Exception) {
            // Out of memory protection
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val layer = activeLayer() ?: return
        try {
            // Save current for redo
            val currentStream = java.io.ByteArrayOutputStream()
            layer.bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, currentStream)
            redoStack.addLast(currentStream.toByteArray())

            val bytes = undoStack.removeLast()
            val restored = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val canvas = android.graphics.Canvas(layer.bitmap)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.drawBitmap(restored, 0f, 0f, null)
            restored.recycle()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val layer = activeLayer() ?: return
        try {
            val currentStream = java.io.ByteArrayOutputStream()
            layer.bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, currentStream)
            undoStack.addLast(currentStream.toByteArray())

            val bytes = redoStack.removeLast()
            val restored = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val canvas = android.graphics.Canvas(layer.bitmap)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.drawBitmap(restored, 0f, 0f, null)
            restored.recycle()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun clearCanvas() {
        saveUndoState()
        project.layerManager.clearAll()
        project.markDirty()
    }

    fun setBrush(type: BrushType) {
        brushSettings.applyType(type)
        currentTool = DrawingTool.BRUSH
    }

    fun setColor(color: Color) {
        brushSettings.color = color
    }
}
