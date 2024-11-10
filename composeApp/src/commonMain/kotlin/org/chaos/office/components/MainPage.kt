package org.chaos.office.components

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import org.chaos.office.services.Settings

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun mainPage(settings: Settings) {
    val windowSizeClass = calculateWindowSizeClass()
    val screenSize = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> PageSize.Compact
        WindowWidthSizeClass.Medium -> PageSize.Medium
        else -> PageSize.Expanded
    }

    val currentAppVersion = "1.0.0" // Replace with your actual app version
    var showWelcomePage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val lastSeenVersion = settings.lastSeenVersion.first()
        if (lastSeenVersion != currentAppVersion) {
            showWelcomePage = true
        }
    }

    if (showWelcomePage) {
        WelcomePage(
            appVersion = currentAppVersion,
            onFinish = {
                showWelcomePage = false
                kotlinx.coroutines.runBlocking {
                    settings.setLastSeenVersion(currentAppVersion)
                }
            }
        )
    } else {
//        LoginPage(screenSize)
        HomePage(screenSize)
    }
}

enum class PageSize {
    Compact, Medium, Expanded
}