package com.robotdraw.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotdraw.engine.Editor
import com.robotdraw.engine.Tool
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private val HUE_COLORS = listOf(
    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
)

private val BUILT_IN_PALETTE: List<Int> = listOf(
    0xFF000000, 0xFF1B1F27, 0xFF3C3F45, 0xFF6B7280, 0xFF9CA3AF, 0xFFD1D5DB, 0xFFFFFFFF,
    0xFF7D93AD, 0xFF4E6A88, 0xFF2F4157, 0xFF1E3A5F, 0xFFA0907A, 0xFF6B5B4B, 0xFF8E6C5A,
    0xFFC9B79C, 0xFFEADBC8, 0xFF5E7B6A, 0xFF8BA08F, 0xFF7A5C6B, 0xFFB08C9B, 0xFF8A2F2F,
    0xFFC0643B, 0xFFD4A24C, 0xFF3F7D5C
).map { it.toInt() }

private const val PREFS_NAME = "robotdraw_prefs"
private const val PREFS_CUSTOM = "custom_palette"
private const val MAX_CUSTOM = 24

fun hsvToArgb(h: Float, s: Float, v: Float): Int =
    AndroidColor.HSVToColor(floatArrayOf(h, s, v))

fun argbToHsv(argb: Int): FloatArray {
    val out = FloatArray(3)
    AndroidColor.colorToHSV(argb, out)
    return out
}

private fun loadCustom(context: Context): List<Int> = try {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREFS_CUSTOM, "")
        .orEmpty()
        .split(",")
        .filter { it.isNotBlank() }
        .map { it.trim().toLong().toInt() }
} catch (e: Exception) {
    emptyList()
}

private fun saveCustom(context: Context, colors: List<Int>) {
    try {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_CUSTOM, colors.joinToString(","))
            .apply()
    } catch (e: Exception) {
        // Palette is a convenience; losing a save is not fatal.
    }
}

/**
 * Full colour panel: foreground/background slots, hue-saturation wheel, brightness,
 * RGB sliders, HEX input, eyedropper, built-in and custom palettes, and recent colours.
 */
@Composable
fun ColorPanel(editor: Editor) {
    val context = LocalContext.current
    var target by remember { mutableStateOf(0) } // 0 = foreground, 1 = background
    val shown = if (target == 0) editor.color else editor.backgroundColor

    val init = argbToHsv(shown)
    var hue by remember { mutableFloatStateOf(init[0]) }
    var sat by remember { mutableFloatStateOf(init[1]) }
    var value by remember { mutableFloatStateOf(init[2]) }

    val custom = remember {
        mutableStateListOf<Int>().apply { addAll(loadCustom(context)) }
    }

    fun applyColor(argb: Int) {
        if (target == 0) editor.pickColor(argb) else editor.backgroundColor = argb
    }

    fun adopt(argb: Int) {
        val f = argbToHsv(argb)
        if (f[1] > 0.001f) hue = f[0]
        sat = f[1]
        value = f[2]
        applyColor(argb)
    }

    fun setHsv(h: Float, s: Float, v: Float) {
        hue = h
        sat = s
        value = v
        applyColor(hsvToArgb(h, s, v))
    }

    // Keep the HSV state in sync when the colour changes from somewhere else.
    LaunchedEffect(shown, target) {
        if (hsvToArgb(hue, sat, value) != shown) {
            val f = argbToHsv(shown)
            if (f[1] > 0.001f) hue = f[0]
            sat = f[1]
            value = f[2]
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ColorSlot(
                label = "Foreground",
                argb = editor.color,
                selected = target == 0,
                modifier = Modifier.weight(1f)
            ) { target = 0 }
            ColorSlot(
                label = "Background",
                argb = editor.backgroundColor,
                selected = target == 1,
                modifier = Modifier.weight(1f)
            ) { target = 1 }
            IconButton(onClick = { editor.swapColors() }) {
                Icon(Icons.Default.SwapHoriz, "Swap colours", tint = TextPrimary)
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HueSatWheel(hue, sat) { h, s ->
                hue = h
                sat = s
                applyColor(hsvToArgb(h, s, value))
            }
        }

        LabeledSlider("Brightness", value, 0f..1f, "${(value * 100).toInt()}%") {
            setHsv(hue, sat, it)
        }
        LabeledSlider("Saturation", sat, 0f..1f, "${(sat * 100).toInt()}%") {
            setHsv(hue, it, value)
        }

        val r = (shown shr 16) and 0xFF
        val g = (shown shr 8) and 0xFF
        val b = shown and 0xFF
        LabeledSlider("Red", r.toFloat(), 0f..255f, "$r") {
            adopt((0xFF shl 24) or (it.toInt() shl 16) or (g shl 8) or b)
        }
        LabeledSlider("Green", g.toFloat(), 0f..255f, "$g") {
            adopt((0xFF shl 24) or (r shl 16) or (it.toInt() shl 8) or b)
        }
        LabeledSlider("Blue", b.toFloat(), 0f..255f, "$b") {
            adopt((0xFF shl 24) or (r shl 16) or (g shl 8) or it.toInt())
        }

        var hex by remember(shown) { mutableStateOf(String.format("%06X", shown and 0xFFFFFF)) }
        OutlinedTextField(
            value = hex,
            onValueChange = { input ->
                val clean = input.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                hex = clean
                if (clean.length == 6) {
                    adopt(clean.toLong(16).toInt() or 0xFF000000.toInt())
                }
            },
            label = { Text("HEX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        RaisedButton(
            label = "Eyedropper",
            selected = editor.tool == Tool.PICKER,
            modifier = Modifier.fillMaxWidth()
        ) {
            editor.tool = Tool.PICKER
        }

        SectionTitle("PALETTE")
        BUILT_IN_PALETTE.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c -> Swatch(c) { adopt(c) } }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("CUSTOM")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                if (!custom.contains(shown)) {
                    if (custom.size >= MAX_CUSTOM) custom.removeAt(0)
                    custom.add(shown)
                    saveCustom(context, custom.toList())
                }
            }) {
                Icon(Icons.Default.Add, "Add to custom palette", tint = TextPrimary)
            }
            IconButton(onClick = {
                custom.clear()
                saveCustom(context, emptyList())
            }) {
                Icon(Icons.Default.Delete, "Clear custom palette", tint = TextMuted)
            }
        }
        if (custom.isEmpty()) {
            Text("Press + to save the current colour here.", color = TextMuted, fontSize = 12.sp)
        } else {
            custom.chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { c -> Swatch(c) { adopt(c) } }
                }
            }
        }

        val history = editor.recentColors
        if (history.isNotEmpty()) {
            SectionTitle("RECENT")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.forEach { c -> Swatch(c) { adopt(c) } }
            }
        }
    }
}

@Composable
private fun ColorSlot(
    label: String,
    argb: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(argb), RoundedCornerShape(10.dp))
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) SteelBlue else Edge,
                    RoundedCornerShape(10.dp)
                )
        )
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) TextPrimary else TextMuted
        )
    }
}

@Composable
private fun HueSatWheel(hue: Float, sat: Float, onChange: (Float, Float) -> Unit) {
    var half by remember { mutableFloatStateOf(100f) }
    Surface(
        shape = CircleShape,
        shadowElevation = 8.dp,
        color = Panel,
        modifier = Modifier.size(200.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { half = it.width / 2f }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        reportWheel(down.position, half, onChange)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            reportWheel(change.position, half, onChange)
                            change.consume()
                        }
                    }
                }
        ) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = min(size.width, size.height) / 2f
            drawCircle(brush = Brush.sweepGradient(HUE_COLORS, c), radius = r, center = c)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), c, r),
                radius = r,
                center = c
            )
            val rad = Math.toRadians(hue.toDouble())
            val d = sat * r
            val p = Offset(c.x + (cos(rad) * d).toFloat(), c.y + (sin(rad) * d).toFloat())
            drawCircle(color = Color.Black, radius = 10f.dp.toPx(), center = p, style = Stroke(1.5f.dp.toPx()))
            drawCircle(color = Color.White, radius = 8f.dp.toPx(), center = p, style = Stroke(3f.dp.toPx()))
        }
    }
}

private fun reportWheel(p: Offset, half: Float, onChange: (Float, Float) -> Unit) {
    if (half <= 0f) return
    val dx = p.x - half
    val dy = p.y - half
    val s = min(1f, sqrt(dx * dx + dy * dy) / half)
    var h = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (h < 0f) h += 360f
    onChange(h, s)
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(display, color = TextMuted, fontSize = 12.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun Swatch(argb: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(Color(argb), RoundedCornerShape(10.dp))
            .border(1.dp, Edge, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    )
}
