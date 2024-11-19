package org.chaos.office.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.chaos.office.configuration.NavigationItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(
    currentNavItem: NavigationItem,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row {
                Icon(
                    if (currentNavItem == NavigationItem.Home) Icons.Default.Home else Icons.Default.Search,
                    contentDescription = currentNavItem.name
                )
                Text(currentNavItem.name)
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { /* TODO: Implement notifications */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
        }
    )
}
