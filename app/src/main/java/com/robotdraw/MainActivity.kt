package com.robotdraw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.robotdraw.engine.Storage
import com.robotdraw.ui.AppState
import com.robotdraw.ui.EditorScreen
import com.robotdraw.ui.HomeScreen
import com.robotdraw.ui.NewProjectScreen
import com.robotdraw.ui.RobotTheme
import com.robotdraw.ui.Screen

class MainActivity : ComponentActivity() {

    private val appState = AppState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The interface is English-first; keep a fixed direction so layouts do not
            // reflow when the phone language is Arabic.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                RobotTheme {
                    when (appState.screen) {
                        Screen.Home -> HomeScreen(appState)
                        Screen.NewProject -> NewProjectScreen(appState)
                        Screen.Editing -> EditorScreen(appState)
                    }
                }
            }
        }
    }

    override fun onStop() {
        appState.editor?.let { Storage.saveNow(applicationContext, it.doc) }
        super.onStop()
    }
}
