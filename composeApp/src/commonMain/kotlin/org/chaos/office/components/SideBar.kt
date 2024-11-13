package org.chaos.office.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    hasBackNavigation: Boolean = false,
    actions: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
    ),
    title: @Composable () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = title,
        actions = {
            if (actions != null) {
                actions()
            }
        },
        navigationIcon = {
            if (hasBackNavigation) {
                if (navigationIcon != null) {
                    navigationIcon()
                }
            }
        },
        colors = colors,
    )
}

@Composable
fun Sidebar() {
    var selectedItem by remember { mutableIntStateOf(0) }
    var isDrawerExpanded by remember { mutableStateOf(false) }

    val items = listOf("Home", "Search", "Settings")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons = listOf(Icons.TwoTone.Home, Icons.TwoTone.Favorite, Icons.TwoTone.Star)

    val drawerWidth by animateDpAsState(if (isDrawerExpanded) 240.dp else 80.dp)

    NavigationRail (
        modifier = Modifier.width(drawerWidth),
        header = {
            IconButton(onClick = { isDrawerExpanded = !isDrawerExpanded }) {
                Icon(
                    if (isDrawerExpanded) Icons.AutoMirrored.Default.KeyboardArrowLeft else Icons.Default.Menu,
                    contentDescription = if (isDrawerExpanded) "Collapse" else "Expand"
                )
            }
        }
    )
    {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                icon = {
                    Icon(
                        if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                        contentDescription = item
                    )
                },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}


@Composable
fun NavigationBar() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Songs", "Artists", "Playlists")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons =
        listOf(Icons.TwoTone.Home, Icons.TwoTone.Favorite, Icons.TwoTone.Star)

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                        contentDescription = item
                    )
                },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}
