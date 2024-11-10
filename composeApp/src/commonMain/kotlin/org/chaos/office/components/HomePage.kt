package org.chaos.office.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.chaos.office.style.AppTheme

@Composable
fun HomePage(screenSize: PageSize) {
    AppTheme {
        when (screenSize) {
            PageSize.Compact -> CompactHomeLayout()
            PageSize.Medium -> MediumHomeLayout()
            PageSize.Expanded -> ExpandedHomeLayout()
        }
    }
}

@Composable
fun CompactHomeLayout() {
    var showSidebar by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { Header(onMenuClick = { showSidebar = !showSidebar }) }
    ) { paddingValues ->
        Row(Modifier.padding(paddingValues)) {
            if (showSidebar) {
                Sidebar()
            }
            MainContent()
        }
    }
}

@Composable
fun MediumHomeLayout() {
    Row {
        Sidebar()
        Column {
            Header()
            MainContent()
        }
    }
}

@Composable
fun ExpandedHomeLayout() {
    Row {
        Sidebar()
        Column {
            Header()
            MainContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(onMenuClick: () -> Unit = {}) {
    TopAppBar(
        title = { Text("Home") },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Implement search */ }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { /* TODO: Implement notifications */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
        }
    )
}

@Composable
fun MainContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Main Content Area")
    }
}