package com.gagaart.app.engine.brush

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Mutable brush configuration used by the drawing engine.
 */
class BrushSettings(
    initialType: BrushType = BrushType.PENCIL,
    initialColor: Color = Color.Black
) {
    var type by mutableStateOf(initialType)
    var color by mutableStateOf(initialColor)
    var size by mutableFloatStateOf(initialType.defaultSize)
    var opacity by mutableFloatStateOf(initialType.defaultOpacity)
    var hardness by mutableFloatStateOf(initialType.defaultHardness)
    var spacing by mutableFloatStateOf(0.15f) // relative to size

    fun applyType(newType: BrushType) {
        type = newType
        size = newType.defaultSize
        opacity = newType.defaultOpacity
        hardness = newType.defaultHardness
    }

    fun copy(): BrushSettings = BrushSettings(type, color).also {
        it.size = size
        it.opacity = opacity
        it.hardness = hardness
        it.spacing = spacing
    }
}
