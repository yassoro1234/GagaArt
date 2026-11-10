package com.gagaart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gagaart.app.engine.DrawingEngine
import com.gagaart.app.engine.project.CanvasBackground
import com.gagaart.app.engine.project.CanvasSize
import com.gagaart.app.engine.project.Project
import com.gagaart.app.ui.screens.DrawingScreen
import com.gagaart.app.ui.screens.HomeScreen
import com.gagaart.app.ui.theme.GagaArtTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GagaArtTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GagaArtApp()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun GagaArtApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var engine by remember { mutableStateOf<DrawingEngine?>(null) }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                onNewDrawing = {
                    // Create a new project with reasonable default size for mobile
                    val project = Project(
                        canvasSize = CanvasSize(1080, 1440, "Mobile Portrait"),
                        background = CanvasBackground.WHITE,
                        name = "Untitled Drawing"
                    )
                    engine = DrawingEngine(project)
                    currentScreen = Screen.Drawing
                },
                onOpenProject = { /* TODO */ },
                onGallery = { /* TODO */ },
                onSettings = { /* TODO */ },
                onTutorials = { /* TODO */ }
            )
        }
        is Screen.Drawing -> {
            engine?.let { eng ->
                DrawingScreen(
                    engine = eng,
                    onBack = {
                        currentScreen = Screen.Home
                        engine = null
                    }
                )
            }
        }
    }
}

sealed class Screen {
    data object Home : Screen()
    data object Drawing : Screen()
}
