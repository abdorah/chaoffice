package org.chaos.office.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import org.chaos.office.configuration.NavigationItem


@Composable
fun SideBar(currentNavItem: NavigationItem, onNavItemSelected: (NavigationItem) -> Unit) {
    NavigationRail {
        NavigationRailItem(
            icon = { Icon(if (currentNavItem == NavigationItem.Home) Icons.Filled.Home else Icons.Outlined.Home, "Home") },
            label = { Text("Home") },
            selected = currentNavItem == NavigationItem.Home,
            onClick = { onNavItemSelected(NavigationItem.Home) }
        )
        NavigationRailItem(
            icon = { Icon(if (currentNavItem == NavigationItem.Search) Icons.Filled.Search else Icons.Outlined.Search, "Search") },
            label = { Text("Search") },
            selected = currentNavItem == NavigationItem.Search,
            onClick = { onNavItemSelected(NavigationItem.Search) }
        )
    }
}

@Composable
fun BottomBar(currentNavItem: NavigationItem, onNavItemSelected: (NavigationItem) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(if (currentNavItem == NavigationItem.Home) Icons.Filled.Home else Icons.Outlined.Home, "Home") },
            label = { Text("Home") },
            selected = currentNavItem == NavigationItem.Home,
            onClick = { onNavItemSelected(NavigationItem.Home) }
        )
        NavigationBarItem(
            icon = { Icon(if (currentNavItem == NavigationItem.Search) Icons.Filled.Search else Icons.Outlined.Search, "Search") },
            label = { Text("Search") },
            selected = currentNavItem == NavigationItem.Search,
            onClick = { onNavItemSelected(NavigationItem.Search) }
        )
    }
}