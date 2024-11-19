package org.chaos.office

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.chaos.office.pages.mainPage
import org.chaos.office.services.Settings
import org.chaos.office.services.createDataStore

fun main() {

    application {
        val windowState = rememberWindowState()
        val version = "1.2.1"

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "ChaOffice",
        ) {
            val dataStore = createDataStore { System.getProperty("user.home") }
            val settings = Settings(dataStore)

            mainPage(
                settings = settings,
                applicationVersion = version
            )
        }
    }
}