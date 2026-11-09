package com.robotdraw.ui

import android.graphics.Paint as NativePaint
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotdraw.engine.BrushSettings
import com.robotdraw.engine.Editor
import com.robotdraw.engine.Storage
import com.robotdraw.engine.Tool
import com.robotdraw.model.Document
import com.robotdraw.model.Layer
import android.widget.Toast

private val PaperDark = Color(0xFF2A303A)
private val CanvasBackdrop = Color(0xFF0E1116)

private val PALETTE = listOf(
    0xFF111111, 0xFF3C3F45, 0xFF7A7F87, 0xFFD4D8DE, 0xFFFFFFFF,
    0xFF7D93AD, 0xFF4E6A88, 0xFF2F4157, 0xFFA0907A, 0xFF6B5B4B,
    0xFF8E6C5A, 0xFFC9B79C, 0xFF5E7B6A, 0xFF8BA08F, 0xFF7A5C6B, 0xFFB08C9B
).map { it.toInt() }

@Composable
fun EditorScreen(state: AppState) {
    val context = LocalContext.current
    val editor = state.editor ?: return
    val doc = editor.doc

    var panelTab by remember { mutableStateOf(0) }
    var fullscreen by remember { mutableStateOf(false) }
    var exportOpen by remember { mutableStateOf(false) }

    // Autosave after each finished edit.
    androidx.compose.runtime.DisposableEffect(editor) {
        editor.onChanged = { Storage.scheduleSave(context, doc) }
        onDispose { editor.onChanged = null }
    }

    val importPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val ok = Storage.importImage(context, doc, uri)
            Toast.makeText(
                context,
                if (ok) "Image imported as a new layer" else "Could not open that image",
                Toast.LENGTH_SHORT
            ).show()
            if (ok) Storage.scheduleSave(context, doc)
        }
    }

    BackHandler {
        if (fullscreen) fullscreen = false else state.close(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(if (fullscreen) 0.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!fullscreen) {
            TopBar(
                editor = editor,
                onHome = { state.close(context) },
                onImport = { importPicker.launch("image/*") },
                exportOpen = exportOpen,
                onToggleExport = { exportOpen = !exportOpen },
                onExport = { jpeg ->
                    exportOpen = false
                    val uri = Storage.exportToGallery(context, doc, jpeg)
                    Toast.makeText(
                        context,
                        if (uri != null) "Saved to Pictures/ROBOT DRAW" else "Export failed",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFullscreen = { fullscreen = true }
            )
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CanvasArea(editor, Modifier.weight(1f).fillMaxSize(), fullscreen)
                    if (!fullscreen) {
                        SidePanel(
                            editor = editor,
                            tab = panelTab,
                            onTab = { panelTab = it },
                            modifier = Modifier.width(340.dp).fillMaxSize()
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CanvasArea(editor, Modifier.weight(1f).fillMaxWidth(), fullscreen)
                    if (!fullscreen) {
                        SidePanel(
                            editor = editor,
                            tab = panelTab,
                            onTab = { panelTab = it },
                            modifier = Modifier.fillMaxWidth().height(330.dp)
                        )
                    }
                }
            }
        }

        if (fullscreen) {
            RaisedButton("Exit full screen", modifier = Modifier.padding(8.dp)) {
                fullscreen = false
            }
        }
    }
}

@Composable
private fun TopBar(
    editor: Editor,
    onHome: () -> Unit,
    onImport: () -> Unit,
    exportOpen: Boolean,
    onToggleExport: () -> Unit,
    onExport: (jpeg: Boolean) -> Unit,
    onFullscreen: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onHome) { Icon(Icons.Default.Home, "Home", tint = TextPrimary) }
            Text(editor.doc.name, color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            RaisedButton("Undo", enabled = editor.canUndo) { editor.undo() }
            RaisedButton("Redo", enabled = editor.canRedo) { editor.redo() }
            RaisedButton("Fit") { editor.fit() }
            RaisedButton("Reset angle") { editor.resetRotation() }
            RaisedButton("Import") { onImport() }
            RaisedButton("Export") { onToggleExport() }
            if (exportOpen) {
                RaisedButton("PNG", selected = true) { onExport(false) }
                RaisedButton("JPEG", selected = true) { onExport(true) }
            }
            IconButton(onClick = onFullscreen) { Icon(Icons.Default.Fullscreen, "Full screen", tint = TextPrimary) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = editor.tool == Tool.BRUSH,
                onClick = { editor.tool = Tool.BRUSH },
                label = { Text("Brush") }
            )
            FilterChip(
                selected = editor.tool == Tool.ERASER,
                onClick = { editor.tool = Tool.ERASER },
                label = { Text("Eraser") }
            )
            FilterChip(
                selected = editor.tool == Tool.PICKER,
                onClick = { editor.tool = Tool.PICKER },
                label = { Text("Eyedropper") }
            )
        }
    }
}

@Composable
private fun CanvasArea(editor: Editor, modifier: Modifier, fullscreen: Boolean) {
    val doc = editor.doc
    val layerPaint = remember { NativePaint(NativePaint.FILTER_BITMAP_FLAG) }

    Box(
        modifier = modifier
            .background(CanvasBackdrop, RoundedCornerShape(if (fullscreen) 0.dp else 16.dp))
            .onSizeChanged { editor.setViewport(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(doc) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    var drawing = false
                    var multi = false
                    var moved = false

                    if (editor.tool != Tool.PICKER) {
                        drawing = editor.beginStroke(downPos.x, downPos.y, pressureOf(down))
                    }

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            if (drawing) {
                                editor.endStroke()
                                drawing = false
                            }
                            multi = true
                            moved = true
                            editor.gesture(
                                zoomDelta = event.calculateZoom(),
                                pan = event.calculatePan(),
                                rotationDelta = event.calculateRotation(),
                                centroid = event.calculateCentroid(useCurrent = true)
                            )
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (pressed.size == 1 && !multi) {
                            val change: PointerInputChange = pressed[0]
                            if (change.positionChanged()) {
                                moved = true
                                if (drawing) {
                                    editor.moveStroke(
                                        change.position.x,
                                        change.position.y,
                                        pressureOf(change)
                                    )
                                }
                                change.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }

                    if (drawing) editor.endStroke()
                    if (!moved && editor.tool == Tool.PICKER) {
                        editor.pick(downPos.x, downPos.y)
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val revision = doc.revision
            val s = editor.scale
            withTransform({
                translate(editor.viewW / 2f + editor.panX, editor.viewH / 2f + editor.panY)
                rotate(editor.rotation, pivot = Offset.Zero)
                scale(s, s, pivot = Offset.Zero)
                translate(-doc.width / 2f, -doc.height / 2f)
            }) {
                val paper = if (doc.background != 0) Color(doc.background) else PaperDark
                drawRect(paper, size = Size(doc.width.toFloat(), doc.height.toFloat()))
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    doc.layers.forEach { layer ->
                        if (layer.visible) {
                            layerPaint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                            native.drawBitmap(layer.bitmap, 0f, 0f, layerPaint)
                        }
                    }
                }
            }
        }
    }
}

private fun pressureOf(change: PointerInputChange): Float =
    if (change.type == PointerType.Stylus) change.pressure else 1f

@Composable
private fun SidePanel(
    editor: Editor,
    tab: Int,
    onTab: (Int) -> Unit,
    modifier: Modifier
) {
    PanelSurface(modifier = modifier) {
        TabRow(selectedTabIndex = tab, containerColor = androidx.compose.ui.graphics.Color.Transparent) {
            listOf("Brush", "Color", "Layers").forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { onTab(i) }, text = { Text(title) })
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tab) {
                0 -> BrushControls(editor)
                1 -> ColorControls(editor)
                else -> LayerControls(editor)
            }
        }
    }
}

@Composable
private fun BrushControls(editor: Editor) {
    val b = editor.brush
    SectionTitle("PRESET")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BrushSettings.PRESETS.forEach { preset ->
            FilterChip(
                selected = b.name == preset.name,
                onClick = {
                    editor.brush = preset
                    if (preset.erase) editor.tool = Tool.ERASER
                    else if (editor.tool == Tool.ERASER) editor.tool = Tool.BRUSH
                },
                label = { Text(preset.name) }
            )
        }
    }

    BrushPreview(editor)

    SliderRow("Size", b.size, 1f..200f, "%.0f px") { editor.brush = b.copy(size = it) }
    SliderRow("Opacity", b.opacity, 0.05f..1f, "%.0f%%", 100f) { editor.brush = b.copy(opacity = it) }
    SliderRow("Hardness", b.hardness, 0f..1f, "%.0f%%", 100f) { editor.brush = b.copy(hardness = it) }
    SliderRow("Spacing", b.spacing, 0.03f..0.5f, "%.0f%%", 100f) { editor.brush = b.copy(spacing = it) }
    SliderRow("Smoothing", b.smoothing, 0f..0.9f, "%.0f%%", 100f) { editor.brush = b.copy(smoothing = it) }
}

@Composable
private fun BrushPreview(editor: Editor) {
    val b = editor.brush
    val c = Color(editor.color)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(PaperDark, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = minOf(b.size / 2f, 44f).coerceAtLeast(1f)
            val alpha = b.opacity.coerceIn(0f, 1f)
            val center = Offset(size.width / 2f, size.height / 2f)
            if (b.erase) {
                drawCircle(Color(0x66FFFFFF), radius, center)
            } else if (b.hardness >= 0.99f) {
                drawCircle(c.copy(alpha = alpha), radius, center)
            } else {
                val h = b.hardness.coerceIn(0.01f, 0.99f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to c.copy(alpha = alpha),
                            h to c.copy(alpha = alpha),
                            1f to c.copy(alpha = 0f)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    displayScale: Float = 1f,
    onChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                String.format(format, value * displayScale),
                color = TextMuted,
                fontSize = 13.sp
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ColorControls(editor: Editor) {
    val argb = editor.color
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val bl = argb and 0xFF
    var hex by remember(argb) { mutableStateOf(String.format("%06X", argb and 0xFFFFFF)) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(argb), RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(
            value = hex,
            onValueChange = { input ->
                val clean = input.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                hex = clean
                if (clean.length == 6) {
                    editor.setColor(clean.toLong(16).toInt() or 0xFF000000.toInt())
                }
            },
            label = { Text("HEX") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }

    SliderRow("Red", r.toFloat(), 0f..255f, "%.0f") { editor.setColor(argbOf(it.toInt(), g, bl)) }
    SliderRow("Green", g.toFloat(), 0f..255f, "%.0f") { editor.setColor(argbOf(r, it.toInt(), bl)) }
    SliderRow("Blue", bl.toFloat(), 0f..255f, "%.0f") { editor.setColor(argbOf(r, g, it.toInt())) }

    SectionTitle("PALETTE")
    PALETTE.chunked(8).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { Swatch(it) { editor.setColor(it) } }
        }
    }

    if (editor.recentColors.isNotEmpty()) {
        SectionTitle("RECENT")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            editor.recentColors.forEach { c -> Swatch(c) { editor.setColor(c) } }
        }
    }
}

private fun argbOf(r: Int, g: Int, b: Int): Int =
    (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

@Composable
private fun Swatch(color: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(Color(color), RoundedCornerShape(10.dp))
            .clickable { onClick() }
    )
}

@Composable
private fun LayerControls(editor: Editor) {
    val doc = editor.doc
    val active = doc.active
    val context = LocalContext.current

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(onClick = { doc.addLayer(); Storage.scheduleSave(context, doc) }) {
            Icon(Icons.Default.Add, "New layer", tint = TextPrimary)
        }
        IconButton(onClick = { active?.let { doc.duplicate(it) } }) {
            Icon(Icons.Default.ContentCopy, "Duplicate", tint = TextPrimary)
        }
        IconButton(onClick = { active?.let { doc.remove(it) } }) {
            Icon(Icons.Default.Delete, "Delete", tint = TextPrimary)
        }
        IconButton(onClick = { active?.let { doc.mergeDown(it) } }) {
            Icon(Icons.Default.FileUpload, "Merge down", tint = TextPrimary)
        }
        IconButton(onClick = { active?.let { doc.move(it, 1) } }) {
            Icon(Icons.Default.ArrowUpward, "Move up", tint = TextPrimary)
        }
        IconButton(onClick = { active?.let { doc.move(it, -1) } }) {
            Icon(Icons.Default.ArrowDownward, "Move down", tint = TextPrimary)
        }
        IconButton(onClick = { editor.clearActiveLayer() }) {
            Icon(Icons.Default.Download, "Clear layer", tint = TextPrimary)
        }
    }

    doc.layers.asReversed().forEach { layer ->
        LayerRow(layer, selected = layer.id == doc.activeId) { doc.activeId = layer.id }
    }

    if (active != null) {
        SectionTitle("ACTIVE LAYER OPACITY")
        Slider(
            value = active.opacity,
            onValueChange = { active.opacity = it },
            valueRange = 0f..1f
        )
        OutlinedTextField(
            value = active.name,
            onValueChange = { active.name = it.take(32) },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LayerRow(layer: Layer, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) SteelBlue.copy(alpha = 0.25f) else PanelRaised,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { layer.visible = !layer.visible }) {
            Icon(
                if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                "Visibility",
                tint = TextPrimary
            )
        }
        IconButton(onClick = { layer.locked = !layer.locked }) {
            Icon(
                if (layer.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                "Lock",
                tint = TextPrimary
            )
        }
        Text(
            layer.name,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
            fontSize = 14.sp,
            color = if (layer.visible) TextPrimary else TextMuted
        )
        IconButton(onClick = onSelect) {
            Icon(Icons.Default.Add, "Select", tint = if (selected) SteelBlue else TextMuted)
        }
    }
}
