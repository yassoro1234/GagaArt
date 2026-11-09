package com.robotdraw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.robotdraw.engine.Storage
import com.robotdraw.ui.AppState
import com.robotdraw.ui.RobotApp
import com.robotdraw.ui.RobotTheme

class MainActivity : ComponentActivity() {

    private val appState = AppState()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The interface is English-first; keep a fixed direction so layouts do not
            // reflow when the phone language is Arabic.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                RobotTheme {
                    RobotApp(appState)
                }
            }
        }
    }

    override fun onStop() {
        appState.editor?.let { Storage.saveNow(applicationContext, it.doc) }
        super.onStop()
    }
}
