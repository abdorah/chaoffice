package com.inventory.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.AppTextField
import com.inventory.app.ui.components.EntityListItem
import com.inventory.app.ui.components.LoadingIndicator
import com.inventory.app.viewmodel.LocationViewModel
import com.inventory.ffi.FfiCreateLocationDto
import com.inventory.ffi.FfiUpdateLocationDto

@Composable
fun LocationListScreen(locationViewModel: LocationViewModel) {
    val locations by locationViewModel.locations.collectAsState()
    val isLoading by locationViewModel.isLoading.collectAsState()
    val error by locationViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { locationViewModel.loadLocations() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            locationViewModel.clearError()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingId != null) "Edit Location" else "New Location") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    AppTextField(value = name, onValueChange = { name = it }, label = "Name")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = address, onValueChange = { address = it }, label = "Address")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = latitude, onValueChange = { latitude = it }, label = "Latitude")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = longitude, onValueChange = { longitude = it }, label = "Longitude")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = capacity, onValueChange = { capacity = it }, label = "Capacity")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val lat = latitude.toDoubleOrNull() ?: 0.0
                    val lng = longitude.toDoubleOrNull() ?: 0.0
                    val cap = capacity.toLongOrNull() ?: 0L
                    if (editingId != null) {
                        locationViewModel.updateLocation(
                            FfiUpdateLocationDto(
                                id = editingId!!,
                                name = name,
                                address = address,
                                latitude = lat,
                                longitude = lng,
                                capacity = cap,
                                managerId = null
                            )
                        ) { showDialog = false }
                    } else {
                        locationViewModel.createLocation(
                            FfiCreateLocationDto(
                                name = name,
                                address = address,
                                latitude = lat,
                                longitude = lng,
                                capacity = cap,
                                managerId = null
                            )
                        ) { showDialog = false }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingId = null; name = ""; address = ""
                latitude = ""; longitude = ""; capacity = ""
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add location")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                isLoading && locations.isEmpty() -> LoadingIndicator()
                locations.isEmpty() -> Text(
                    text = "No locations yet",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(locations, key = { it.id }) { location ->
                        EntityListItem(
                            title = location.name,
                            subtitle = "${location.address} · Cap: ${location.capacity}",
                            onClick = {
                                editingId = location.id
                                name = location.name
                                address = location.address
                                latitude = location.latitude.toString()
                                longitude = location.longitude.toString()
                                capacity = location.capacity.toString()
                                showDialog = true
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    locationViewModel.removeLocation(location.id)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete location",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
