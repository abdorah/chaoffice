package org.chaos.office.pages

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.chaos.office.configuration.PageSize
import org.chaos.office.services.Settings
import org.chaos.office.services.router

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun mainPage(settings: Settings, applicationVersion: String) {
    val windowSizeClass = calculateWindowSizeClass()
    val screenSize = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> PageSize.Compact
        WindowWidthSizeClass.Medium -> PageSize.Medium
        else -> PageSize.Expanded
    }

    var showWelcomePage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val lastSeenVersion = settings.lastSeenVersion.first()
        if (lastSeenVersion != applicationVersion) {
            showWelcomePage = true
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) {
        if (showWelcomePage) {
            WelcomePage(
                appVersion = applicationVersion,
                onFinish = {
                    showWelcomePage = false
                    runBlocking {
                        settings.setLastSeenVersion(applicationVersion)
                    }
                }
            )
        } else {
            router(
                navController = rememberNavController(),
                screenSize = screenSize,
                modifier = Modifier
            )
        }
    }
}


