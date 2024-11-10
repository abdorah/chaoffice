package org.chaos.office

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.chaos.office.components.mainPage
import org.chaos.office.services.Settings
import org.chaos.office.services.createDataStore

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ChaOffice",
        ) {
            val dataStore = createDataStore { System.getProperty("user.home") }
            val settings = Settings(dataStore)

            mainPage(settings)
        }
    }