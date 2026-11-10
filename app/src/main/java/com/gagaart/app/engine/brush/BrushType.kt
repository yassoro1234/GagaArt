package com.gagaart.app.engine.brush

/**
 * Professional brush types for Gaga Art.
 */
enum class BrushType(
    val displayName: String,
    val category: BrushCategory,
    val defaultSize: Float = 12f,
    val defaultOpacity: Float = 1f,
    val defaultHardness: Float = 0.8f,
    val supportsSpacing: Boolean = true
) {
    // Basic
    PENCIL("Pencil", BrushCategory.BASIC, 4f, 0.85f, 0.6f),
    PEN("Pen", BrushCategory.BASIC, 3f, 1f, 1f),
    MARKER("Marker", BrushCategory.BASIC, 18f, 0.7f, 0.9f),

    // Artistic / Painting
    PAINT_BRUSH("Paint Brush", BrushCategory.PAINTING, 24f, 0.9f, 0.5f),
    AIRBRUSH("Airbrush", BrushCategory.PAINTING, 40f, 0.35f, 0.2f),
    WATERCOLOR("Watercolor", BrushCategory.PAINTING, 28f, 0.45f, 0.3f),
    OIL_PAINT("Oil Paint", BrushCategory.PAINTING, 22f, 0.95f, 0.7f),
    CALLIGRAPHY("Calligraphy", BrushCategory.ARTISTIC, 14f, 1f, 0.95f),

    // Special
    SPRAY("Spray Brush", BrushCategory.SPECIAL, 50f, 0.4f, 0.15f),
    GLOW("Glow Brush", BrushCategory.SPECIAL, 30f, 0.6f, 0.25f),
    PATTERN("Pattern Brush", BrushCategory.DECORATIVE, 20f, 0.8f, 0.8f),

    // Effects
    SMUDGE("Smudge", BrushCategory.EFFECTS, 25f, 0.7f, 0.4f, supportsSpacing = false),
    BLUR("Blur", BrushCategory.EFFECTS, 30f, 0.5f, 0.3f, supportsSpacing = false),
    DODGE("Dodge", BrushCategory.EFFECTS, 20f, 0.4f, 0.5f, supportsSpacing = false),
    BURN("Burn", BrushCategory.EFFECTS, 20f, 0.4f, 0.5f, supportsSpacing = false);

    companion object {
        fun byCategory(category: BrushCategory) = entries.filter { it.category == category }
    }
}

enum class BrushCategory(val displayName: String) {
    BASIC("Basic Brushes"),
    ARTISTIC("Artistic Brushes"),
    PAINTING("Painting Brushes"),
    TEXTURE("Texture Brushes"),
    SPECIAL("Special Effects"),
    DECORATIVE("Decorative Brushes"),
    EFFECTS("Effects")
}
