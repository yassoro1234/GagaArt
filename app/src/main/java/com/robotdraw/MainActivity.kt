package com.robotdraw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.robotdraw.ui.AppState
import com.robotdraw.ui.EditorScreen
import com.robotdraw.ui.HomeScreen
import com.robotdraw.ui.NewProjectScreen
import com.robotdraw.ui.RobotTheme
import com.robotdraw.ui.Screen
import com.robotdraw.engine.Storage

class MainActivity : ComponentActivity() {

    private val appState = AppState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RobotTheme {
                when (appState.screen) {
                    Screen.Home -> HomeScreen(appState)
                    Screen.NewProject -> NewProjectScreen(appState)
                    Screen.Editing -> EditorScreen(appState)
                }
            }
        }
    }

    override fun onStop() {
        // Make sure the open project is written to disk when the app goes to the background.
        appState.editor?.let { Storage.saveNow(applicationContext, it.doc) }
        super.onStop()
    }
}
