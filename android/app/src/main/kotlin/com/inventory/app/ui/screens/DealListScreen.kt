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
import com.inventory.app.ui.components.AppDropdown
import com.inventory.app.ui.components.AppTextField
import com.inventory.app.ui.components.EntityListItem
import com.inventory.app.ui.components.LoadingIndicator
import com.inventory.app.viewmodel.DealViewModel
import com.inventory.ffi.FfiCreateDealDto
import com.inventory.ffi.FfiDealFrequency
import com.inventory.ffi.FfiDealStatus
import com.inventory.ffi.FfiUpdateDealDto
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun DealListScreen(dealViewModel: DealViewModel) {
    val deals by dealViewModel.deals.collectAsState()
    val isLoading by dealViewModel.isLoading.collectAsState()
    val error by dealViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var unitCost by remember { mutableStateOf("") }
    var totalValue by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(FfiDealFrequency.ONE_TIME) }
    var status by remember { mutableStateOf(FfiDealStatus.DRAFT) }

    LaunchedEffect(Unit) { dealViewModel.loadDeals() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            dealViewModel.clearError()
        }
    }

    fun nowIso(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingId != null) "Edit Deal" else "New Deal") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    AppTextField(value = title, onValueChange = { title = it }, label = "Title")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = description, onValueChange = { description = it }, label = "Description")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = unitCost, onValueChange = { unitCost = it }, label = "Unit Cost")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = totalValue, onValueChange = { totalValue = it }, label = "Total Value")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = startDate, onValueChange = { startDate = it }, label = "Start Date (ISO)")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = endDate, onValueChange = { endDate = it }, label = "End Date (ISO)")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppDropdown(
                        items = FfiDealFrequency.entries,
                        selectedItem = frequency,
                        onItemSelected = { frequency = it },
                        label = "Frequency",
                        itemLabel = { it.name.lowercase().replace("_", " ")
                            .replaceFirstChar { c -> c.uppercase() } }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppDropdown(
                        items = FfiDealStatus.entries,
                        selectedItem = status,
                        onItemSelected = { status = it },
                        label = "Status",
                        itemLabel = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cost = unitCost.toDoubleOrNull() ?: 0.0
                    val total = totalValue.toDoubleOrNull() ?: 0.0
                    val start = startDate.ifBlank { nowIso() }
                    val end = endDate.ifBlank { nowIso() }
                    if (editingId != null) {
                        dealViewModel.updateDeal(
                            FfiUpdateDealDto(
                                id = editingId!!,
                                title = title,
                                description = description,
                                unitCost = cost,
                                totalValue = total,
                                startDate = start,
                                endDate = end,
                                frequency = frequency,
                                status = status
                            )
                        ) { showDialog = false }
                    } else {
                        dealViewModel.createDeal(
                            FfiCreateDealDto(
                                title = title,
                                description = description,
                                unitCost = cost,
                                totalValue = total,
                                startDate = start,
                                endDate = end,
                                frequency = frequency,
                                status = status,
                                productId = null,
                                supplierId = null,
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
                editingId = null; title = ""; description = ""
                unitCost = ""; totalValue = ""
                startDate = ""; endDate = ""
                frequency = FfiDealFrequency.ONE_TIME; status = FfiDealStatus.DRAFT
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add deal")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                isLoading && deals.isEmpty() -> LoadingIndicator()
                deals.isEmpty() -> Text(
                    text = "No deals yet",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(deals, key = { it.id }) { deal ->
                        EntityListItem(
                            title = deal.title,
                            subtitle = "${deal.status.name.lowercase()
                                .replaceFirstChar { it.uppercase() }} · ${deal.frequency.name.lowercase()
                                .replace("_", " ").replaceFirstChar { it.uppercase() }}",
                            onClick = {
                                editingId = deal.id
                                title = deal.title
                                description = deal.description
                                unitCost = deal.unitCost.toString()
                                totalValue = deal.totalValue.toString()
                                startDate = deal.startDate
                                endDate = deal.endDate
                                frequency = deal.frequency
                                status = deal.status
                                showDialog = true
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    dealViewModel.removeDeal(deal.id)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete deal",
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
