package com.inventory.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.AppDropdown
import com.inventory.app.ui.components.AppTextField
import com.inventory.app.ui.components.LoadingIndicator
import com.inventory.app.viewmodel.SyncViewModel
import com.inventory.ffi.FfiSyncConfig
import com.inventory.ffi.FfiSyncStrategy

@Composable
fun SyncSettingsScreen(
    syncViewModel: SyncViewModel,
    sessionToken: String?
) {
    val config by syncViewModel.config.collectAsState()
    val syncResult by syncViewModel.syncResult.collectAsState()
    val isSyncing by syncViewModel.isSyncing.collectAsState()
    val isLoading by syncViewModel.isLoading.collectAsState()
    val error by syncViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val strategies = listOf(
        FfiSyncStrategy.FULL, FfiSyncStrategy.INCREMENTAL,
        FfiSyncStrategy.CONFLICT_RESOLVE_LOCAL, FfiSyncStrategy.CONFLICT_RESOLVE_REMOTE
    )

    var tursoUrl by remember { mutableStateOf("") }
    var tursoAuthToken by remember { mutableStateOf("") }
    var selectedStrategy by remember { mutableStateOf(FfiSyncStrategy.FULL) }
    var syncInterval by remember { mutableStateOf("") }
    var configLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { syncViewModel.loadConfig() }

    LaunchedEffect(config) {
        config?.let {
            if (!configLoaded) {
                tursoUrl = it.tursoUrl
                tursoAuthToken = it.tursoAuthToken
                selectedStrategy = it.defaultStrategy
                syncInterval = it.syncIntervalSeconds.toString()
                configLoaded = true
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            syncViewModel.clearError()
        }
    }

    LaunchedEffect(syncResult) {
        syncResult?.let {
            snackbarHostState.showSnackbar(
                "Pushed: ${it.entitiesPushed} · Pulled: ${it.entitiesPulled} · Conflicts: ${it.conflicts}"
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading && config == null) {
                LoadingIndicator()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Sync Configuration", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    AppTextField(
                        value = tursoUrl,
                        onValueChange = { tursoUrl = it },
                        label = "TursoDB URL",
                        enabled = !isSyncing
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = tursoAuthToken,
                        onValueChange = { tursoAuthToken = it },
                        label = "Auth Token",
                        enabled = !isSyncing
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppDropdown(
                        items = strategies,
                        selectedItem = selectedStrategy,
                        onItemSelected = { selectedStrategy = it },
                        label = "Sync Strategy",
                        itemLabel = { it.name },
                        enabled = !isSyncing
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = syncInterval,
                        onValueChange = { syncInterval = it },
                        label = "Sync Interval (seconds)",
                        enabled = !isSyncing
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            syncViewModel.saveConfig(
                                FfiSyncConfig(
                                    tursoUrl = tursoUrl,
                                    tursoAuthToken = tursoAuthToken,
                                    autoSyncEnabled = true,
                                    syncIntervalSeconds = syncInterval.toULongOrNull() ?: 300u,
                                    defaultStrategy = selectedStrategy
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing
                    ) { Text("Save Configuration") }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Sync Operations", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isSyncing) {
                        LoadingIndicator()
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { sessionToken?.let { syncViewModel.push(it) } },
                            modifier = Modifier.weight(1f),
                            enabled = !isSyncing && sessionToken != null
                        ) { Text("Push") }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { sessionToken?.let { syncViewModel.pull(it) } },
                            modifier = Modifier.weight(1f),
                            enabled = !isSyncing && sessionToken != null
                        ) { Text("Pull") }
                    }

                    // Error with retry
                    error?.let { errMsg ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Error: $errMsg",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = { syncViewModel.clearError() }) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }

                    // Sync result display
                    syncResult?.let { res ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Sync Complete", style = MaterialTheme.typography.titleSmall)
                                Text("Entities Pushed: ${res.entitiesPushed}")
                                Text("Entities Pulled: ${res.entitiesPulled}")
                                Text("Conflicts: ${res.conflicts}")
                            }
                        }
                    }
                }
            }
        }
    }
}
