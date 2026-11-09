package com.robotdraw.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Premium graphite / titanium palette. No neon, no saturated glows.
val Graphite = Color(0xFF121418)
val Panel = Color(0xFF1B1F26)
val PanelRaised = Color(0xFF232932)
val Edge = Color(0xFF3A4350)
val Titanium = Color(0xFFB9C2CE)
val TextPrimary = Color(0xFFE7EAF0)
val TextMuted = Color(0xFF8C96A5)
val SteelBlue = Color(0xFF7D93AD)
val Pressed = Color(0xFF171B21)

@Composable
fun RobotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SteelBlue,
            onPrimary = Graphite,
            background = Graphite,
            onBackground = TextPrimary,
            surface = Panel,
            onSurface = TextPrimary,
            surfaceVariant = PanelRaised,
            onSurfaceVariant = TextMuted,
            outline = Edge
        ),
        content = content
    )
}

/** A layered, rounded panel with a soft shadow and a fine metallic edge. */
@Composable
fun PanelSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = Panel,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Edge)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

/** A raised, tactile button: it lifts with a shadow and tints when pressed or selected. */
@Composable
fun RaisedButton(
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = if (selected) SteelBlue else PanelRaised,
        contentColor = if (selected) Graphite else TextPrimary,
        shadowElevation = if (selected) 1.dp else 5.dp,
        border = BorderStroke(1.dp, if (selected) SteelBlue else Edge)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.Unspecified else TextMuted
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}
