package com.robotdraw.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotdraw.engine.Editor
import com.robotdraw.engine.ProjectInfo
import com.robotdraw.engine.Storage
import com.robotdraw.model.Document
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/** Which top-level screen is showing. */
sealed class Screen {
    object Home : Screen()
    object NewProject : Screen()
    object Editing : Screen()
}

/** Holds navigation and the currently open editor for the whole app. */
class AppState {
    var screen by mutableStateOf<Screen>(Screen.Home)
    var editor by mutableStateOf<Editor?>(null)
    var listVersion by mutableStateOf(0)

    fun open(editor: Editor) {
        this.editor = editor
        screen = Screen.Editing
    }

    fun close(context: android.content.Context) {
        editor?.let { Storage.saveNow(context, it.doc) }
        editor = null
        screen = Screen.Home
        listVersion++
    }
}

@Composable
fun HomeScreen(state: AppState) {
    val context = LocalContext.current
    val projects = remember(state.listVersion) { Storage.list(context) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("ROBOT DRAW", fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("Professional digital drawing studio", color = TextMuted, fontSize = 13.sp, maxLines = 2)
                RaisedButton("New project", selected = true, modifier = Modifier.fillMaxWidth()) {
                    state.screen = Screen.NewProject
                }
            }

            SectionTitle("RECENT PROJECTS")

            if (projects.isEmpty()) {
                PanelSurface(modifier = Modifier.fillMaxWidth()) {
                    Text("No projects yet. Create one to start drawing.", color = TextMuted)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(projects, key = { it.id }) { info ->
                        ProjectRow(info, onOpen = {
                            Storage.load(context, info.id)?.let { doc ->
                                state.open(Editor(doc))
                            }
                        }, onDelete = {
                            Storage.delete(context, info.id)
                            state.listVersion++
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(info: ProjectInfo, onOpen: () -> Unit, onDelete: () -> Unit) {
    val thumb = remember(info.id, info.updated) {
        info.thumb?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
    }
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Panel,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Edge)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(PanelRaised, RoundedCornerShape(10.dp))
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(info.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(info.updated)),
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
            }
        }
    }
}

private enum class Measure(val label: String) { PX("px"), MM("mm"), IN("in") }

private data class Preset(val name: String, val w: Double, val h: Double, val unit: Measure)

private val PRESETS = listOf(
    Preset("A4", 210.0, 297.0, Measure.MM),
    Preset("A3", 297.0, 420.0, Measure.MM),
    Preset("A5", 148.0, 210.0, Measure.MM),
    Preset("A6", 105.0, 148.0, Measure.MM),
    Preset("Square 1080", 1080.0, 1080.0, Measure.PX),
    Preset("Portrait 1080×1920", 1080.0, 1920.0, Measure.PX),
    Preset("Landscape 1920×1080", 1920.0, 1080.0, Measure.PX)
)

@Composable
fun NewProjectScreen(state: AppState) {
    val context = LocalContext.current
    var preset by remember { mutableStateOf(PRESETS[0]) }
    var unit by remember { mutableStateOf(preset.unit) }
    var widthText by remember { mutableStateOf(preset.w.toString()) }
    var heightText by remember { mutableStateOf(preset.h.toString()) }
    var dpiText by remember { mutableStateOf("300") }
    var landscape by remember { mutableStateOf(false) }
    var background by remember { mutableStateOf(0xFFFFFFFF.toInt()) }
    var info by remember { mutableStateOf<String?>(null) }

    fun toPx(value: Double, dpi: Double): Int = when (unit) {
        Measure.PX -> value
        Measure.MM -> value / 25.4 * dpi
        Measure.IN -> value * dpi
    }.roundToInt().coerceAtLeast(1)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RaisedButton("Back") { state.screen = Screen.Home }
                Spacer(Modifier.width(12.dp))
                Text("New project", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            SectionTitle("PRESET")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESETS.forEach { p ->
                    FilterChip(
                        selected = preset == p,
                        onClick = {
                            preset = p
                            unit = p.unit
                            widthText = p.w.toString()
                            heightText = p.h.toString()
                        },
                        label = { Text(p.name) }
                    )
                }
            }

            PanelSurface(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("SIZE")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { widthText = it },
                        label = { Text("Width") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("Height") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                SectionTitle("UNIT")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Measure.values().forEach { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u.label) })
                    }
                }
                OutlinedTextField(
                    value = dpiText,
                    onValueChange = { dpiText = it },
                    label = { Text("DPI (for mm / in)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !landscape, onClick = { landscape = false }, label = { Text("Portrait") })
                    FilterChip(selected = landscape, onClick = { landscape = true }, label = { Text("Landscape") })
                }
            }

            PanelSurface(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("BACKGROUND")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = background == 0xFFFFFFFF.toInt(),
                        onClick = { background = 0xFFFFFFFF.toInt() },
                        label = { Text("White") }
                    )
                    FilterChip(
                        selected = background == 0,
                        onClick = { background = 0 },
                        label = { Text("Transparent") }
                    )
                    FilterChip(
                        selected = background == 0xFF000000.toInt(),
                        onClick = { background = 0xFF000000.toInt() },
                        label = { Text("Black") }
                    )
                }
            }

            info?.let { Text(it, color = TextMuted, fontSize = 12.sp) }

            RaisedButton("Create", selected = true, modifier = Modifier.fillMaxWidth()) {
                val dpi = dpiText.toDoubleOrNull()?.takeIf { it > 0 }
                val w = widthText.toDoubleOrNull()
                val h = heightText.toDoubleOrNull()
                if (dpi == null || w == null || h == null || w <= 0 || h <= 0) {
                    info = "Enter valid numbers for width, height and DPI."
                    return@RaisedButton
                }
                var pw = toPx(w, dpi)
                var ph = toPx(h, dpi)
                if (landscape && pw < ph) {
                    val t = pw; pw = ph; ph = t
                } else if (!landscape && pw > ph && preset.unit != Measure.PX) {
                    val t = pw; pw = ph; ph = t
                }
                val (cw, ch) = Storage.clampedSize(pw, ph)
                if (cw != pw || ch != ph) {
                    info = "Reduced to ${cw}×${ch} px to keep memory use safe."
                }
                val doc = Document(
                    id = System.currentTimeMillis().toString(),
                    width = cw,
                    height = ch,
                    background = background
                )
                doc.addLayer("Layer 1")
                Storage.saveNow(context, doc)
                state.open(Editor(doc))
            }
        }
    }
}
