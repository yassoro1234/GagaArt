package com.robotdraw.engine

/**
 * Parameters for one brush. Sizes are in document pixels; spacing is a fraction of the size.
 * Hardness 1 gives a crisp edge, 0 gives a fully soft falloff.
 */
data class BrushSettings(
    val name: String,
    val size: Float,
    val opacity: Float,
    val hardness: Float,
    val spacing: Float,
    val smoothing: Float,
    val erase: Boolean = false
) {
    companion object {
        val PRESETS: List<BrushSettings> = listOf(
            BrushSettings("Pencil", 4f, 0.9f, 1f, 0.10f, 0.20f),
            BrushSettings("Ink pen", 3f, 1f, 1f, 0.08f, 0.40f),
            BrushSettings("Marker", 26f, 0.55f, 1f, 0.10f, 0.30f),
            BrushSettings("Soft brush", 22f, 0.9f, 0.2f, 0.10f, 0.30f),
            BrushSettings("Airbrush", 60f, 0.35f, 0f, 0.05f, 0.20f),
            BrushSettings("Eraser", 36f, 1f, 1f, 0.10f, 0.20f, erase = true)
        )
    }
}
