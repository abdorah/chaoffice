package org.chaos.office.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.chaos.office.components.BottomBar
import org.chaos.office.components.Header
import org.chaos.office.components.SideBar
import org.chaos.office.configuration.NavigationItem
import org.chaos.office.configuration.PageSize
import org.chaos.office.services.JqExecutor
import org.chaos.office.style.AppTheme

@Composable
fun HomePage(screenSize: PageSize) {
    AppTheme {
        var jqInput by remember { mutableStateOf("") }
        var jqQuery by remember { mutableStateOf(".") }
        var jqOutput by remember { mutableStateOf("") }
        var currentNavItem by remember { mutableStateOf(NavigationItem.Home) }

        when (screenSize) {
            PageSize.Compact, PageSize.Medium -> CompactHomeLayout(
                jqInput, jqQuery, jqOutput, currentNavItem,
                onJqInputChange = { jqInput = it },
                onJqQueryChange = { jqQuery = it },
                onJqSubmit = { jqOutput = JqExecutor.execute(jqInput, jqQuery) },
                onNavItemSelected = { currentNavItem = it }
            )
            PageSize.Expanded -> ExpandedHomeLayout(
                jqInput, jqQuery, jqOutput, currentNavItem,
                onJqInputChange = { jqInput = it },
                onJqQueryChange = { jqQuery = it },
                onJqSubmit = { jqOutput = JqExecutor.execute(jqInput, jqQuery) },
                onNavItemSelected = { currentNavItem = it }
            )
        }
    }
}

@Composable
fun CompactHomeLayout(
    jqInput: String,
    jqQuery: String,
    jqOutput: String,
    currentNavItem: NavigationItem,
    onJqInputChange: (String) -> Unit,
    onJqQueryChange: (String) -> Unit,
    onJqSubmit: () -> Unit,
    onNavItemSelected: (NavigationItem) -> Unit
) {
    Scaffold(
        topBar = {
            Header(currentNavItem) {
                onNavItemSelected(NavigationItem.Search)
            }
        },
        bottomBar = { BottomBar(currentNavItem, onNavItemSelected) }
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            MainContent(jqInput, jqQuery, jqOutput, onJqInputChange, onJqQueryChange, onJqSubmit, currentNavItem)
        }
    }
}

@Composable
fun ExpandedHomeLayout(
    jqInput: String,
    jqQuery: String,
    jqOutput: String,
    currentNavItem: NavigationItem,
    onJqInputChange: (String) -> Unit,
    onJqQueryChange: (String) -> Unit,
    onJqSubmit: () -> Unit,
    onNavItemSelected: (NavigationItem) -> Unit
) {
    Scaffold(
        topBar = {
            Header(currentNavItem) {
                onNavItemSelected(NavigationItem.Search)
            }
        }
    ) { paddingValues ->
        Row(Modifier.padding(paddingValues)) {
            SideBar(currentNavItem, onNavItemSelected)
            MainContent(jqInput, jqQuery, jqOutput, onJqInputChange, onJqQueryChange, onJqSubmit, currentNavItem)
        }
    }
}

@Composable
fun QueryInputAndDisplay(
    jqInput: String,
    jqQuery: String,
    jqOutput: String,
    onJqInputChange: (String) -> Unit,
    onJqQueryChange: (String) -> Unit,
    onJqSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = jqInput,
            onValueChange = onJqInputChange,
            label = { Text("JSON Input") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = jqQuery,
            onValueChange = onJqQueryChange,
            label = { Text("jq Query") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onJqSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run jq Query")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "jq Output:",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = jqOutput,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun MainContent(
    jqInput: String,
    jqQuery: String,
    jqOutput: String,
    onJqInputChange: (String) -> Unit,
    onJqQueryChange: (String) -> Unit,
    onJqSubmit: () -> Unit,
    currentNavItem: NavigationItem
) {
    when(currentNavItem){
        NavigationItem.Search -> QueryInputAndDisplay(jqInput, jqQuery, jqOutput, onJqInputChange, onJqQueryChange, onJqSubmit)
        NavigationItem.Home ->     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Main Content Area")}
    }
}