package com.robotdraw.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotdraw.R
import com.robotdraw.engine.Screen
import kotlinx.coroutines.delay

private const val SPLASH_MILLIS = 1400L

/** Root of the app: shows the splash first, then the current screen. */
@Composable
fun RobotApp(appState: AppState) {
    var splashDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SPLASH_MILLIS)
        splashDone = true
    }

    Crossfade(targetState = splashDone, animationSpec = tween(450)) { done ->
        if (!done) {
            SplashContent()
        } else {
            when (appState.screen) {
                Screen.Home -> HomeScreen(appState)
                Screen.NewProject -> NewProjectScreen(appState)
                Screen.Editing -> EditorScreen(appState)
            }
        }
    }
}

@Composable
fun SplashContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Graphite),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = "ROBOT DRAW logo",
            modifier = Modifier.size(150.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text("ROBOT DRAW", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Professional digital drawing studio", fontSize = 13.sp, color = TextMuted)
    }
}
