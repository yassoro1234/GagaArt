package com.gagaart.app.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.gagaart.app.engine.DrawingEngine
import com.gagaart.app.engine.project.CanvasBackground

/**
 * Main interactive drawing surface with pan / zoom / rotate support.
 */
@Composable
fun DrawingCanvas(
    engine: DrawingEngine,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val backgroundColor = when (engine.project.background) {
        CanvasBackground.WHITE -> Color.White
        CanvasBackground.BLACK -> Color.Black
        CanvasBackground.TRANSPARENT -> Color(0xFFCCCCCC) // checker later
        CanvasBackground.TEXTURED -> Color(0xFFF5F0E6)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2A2E3F))
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotate ->
                    scale = (scale * zoom).coerceIn(0.15f, 8f)
                    offset += pan
                    rotation += rotate
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    rotationZ = rotation
                }
                .pointerInput(engine) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Convert screen to canvas coordinates (simplified)
                            val canvasPoint = Offset(offset.x, offset.y)
                            engine.startStroke(canvasPoint)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            engine.continueStroke(change.position)
                        },
                        onDragEnd = {
                            engine.endStroke()
                        }
                    )
                }
        ) {
            // Draw background
            drawRect(backgroundColor)

            // Draw all visible layers
            val layers = engine.project.layerManager.layers
            for (layer in layers) {
                if (!layer.isVisible) continue
                val imageBitmap = layer.getImageBitmap()
                drawImage(
                    image = imageBitmap,
                    alpha = layer.opacity
                )
            }
        }
    }
}
