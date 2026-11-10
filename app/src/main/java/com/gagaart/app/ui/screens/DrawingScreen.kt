package com.gagaart.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gagaart.app.engine.DrawingEngine
import com.gagaart.app.engine.brush.BrushType
import com.gagaart.app.engine.tool.DrawingTool
import com.gagaart.app.ui.canvas.DrawingCanvas
import com.gagaart.app.ui.theme.GagaPrimary

@Composable
fun DrawingScreen(
    engine: DrawingEngine,
    onBack: () -> Unit
) {
    var showBrushPanel by remember { mutableStateOf(false) }
    var showColorPanel by remember { mutableStateOf(false) }
    var showLayerPanel by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas takes most of the space
        DrawingCanvas(
            engine = engine,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp, top = 56.dp)
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = engine.project.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { engine.undo() }) {
                Icon(Icons.Default.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = { engine.redo() }) {
                Icon(Icons.Default.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = { engine.clearCanvas() }) {
                Icon(Icons.Outlined.Clear, contentDescription = "Clear")
            }
            IconButton(onClick = { /* TODO: save */ }) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
        }

        // Bottom toolbar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .shadow(12.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ToolButton(
                icon = Icons.Default.Brush,
                label = "Brush",
                selected = engine.currentTool == DrawingTool.BRUSH,
                onClick = {
                    engine.currentTool = DrawingTool.BRUSH
                    showBrushPanel = !showBrushPanel
                    showColorPanel = false
                    showLayerPanel = false
                }
            )
            ToolButton(
                icon = Icons.Default.Palette,
                label = "Color",
                selected = showColorPanel,
                onClick = {
                    showColorPanel = !showColorPanel
                    showBrushPanel = false
                    showLayerPanel = false
                }
            )
            ToolButton(
                icon = Icons.Default.Layers,
                label = "Layers",
                selected = showLayerPanel,
                onClick = {
                    showLayerPanel = !showLayerPanel
                    showBrushPanel = false
                    showColorPanel = false
                }
            )
            // Eraser quick toggle
            ToolButton(
                icon = Icons.Outlined.Clear,
                label = "Eraser",
                selected = engine.currentTool == DrawingTool.ERASER,
                onClick = {
                    engine.currentTool = DrawingTool.ERASER
                    showBrushPanel = false
                    showColorPanel = false
                    showLayerPanel = false
                }
            )
        }

        // Brush size / opacity panel
        if (showBrushPanel) {
            BrushPanel(
                engine = engine,
                onDismiss = { showBrushPanel = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }

        // Simple color panel
        if (showColorPanel) {
            ColorPanel(
                engine = engine,
                onDismiss = { showColorPanel = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }

        // Layers panel
        if (showLayerPanel) {
            LayerPanel(
                engine = engine,
                onDismiss = { showLayerPanel = false },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp, top = 60.dp, bottom = 80.dp)
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) GagaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) GagaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BrushPanel(
    engine: DrawingEngine,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("Brushes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(BrushType.entries.toList()) { brush ->
                val selected = engine.brushSettings.type == brush
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) GagaPrimary.copy(0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { engine.setBrush(brush) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = brush.displayName,
                        fontSize = 13.sp,
                        color = if (selected) GagaPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Size: ${engine.brushSettings.size.toInt()}", fontSize = 13.sp)
        Slider(
            value = engine.brushSettings.size,
            onValueChange = { engine.brushSettings.size = it },
            valueRange = 1f..80f
        )
        Text("Opacity: ${(engine.brushSettings.opacity * 100).toInt()}%", fontSize = 13.sp)
        Slider(
            value = engine.brushSettings.opacity,
            onValueChange = { engine.brushSettings.opacity = it },
            valueRange = 0.05f..1f
        )
    }
}

@Composable
private fun ColorPanel(
    engine: DrawingEngine,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color.Black, Color.White, Color.Red, Color.Blue, Color.Green,
        Color.Yellow, Color.Magenta, Color.Cyan, Color(0xFFFF6B4A),
        Color(0xFF7AA2F7), Color(0xFF9ECE6A), Color(0xFFE0AF68)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("Colors", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { engine.setColor(color) }
                )
            }
        }
    }
}

@Composable
private fun LayerPanel(
    engine: DrawingEngine,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layers = engine.project.layerManager.layers
    Column(
        modifier = modifier
            .width(200.dp)
            .fillMaxHeight(0.7f)
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text("Layers", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        layers.forEachIndexed { index, layer ->
            val isActive = index == engine.project.layerManager.activeLayerIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) GagaPrimary.copy(0.15f) else Color.Transparent)
                    .clickable { engine.project.layerManager.setActiveLayer(index) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = layer.name,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (layer.isVisible) "👁" else "—",
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "+ Add",
                color = GagaPrimary,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    engine.project.layerManager.addLayer()
                }
            )
        }
    }
}
