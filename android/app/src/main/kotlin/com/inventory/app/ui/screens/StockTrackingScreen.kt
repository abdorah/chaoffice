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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.inventory.app.viewmodel.StockViewModel
import com.inventory.ffi.FfiMovementType
import com.inventory.ffi.FfiRecordStockMovementDto

@Composable
fun StockTrackingScreen(
    stockViewModel: StockViewModel,
    sessionToken: String?
) {
    val summary by stockViewModel.summary.collectAsState()
    val history by stockViewModel.history.collectAsState()
    val isLoading by stockViewModel.isLoading.collectAsState()
    val error by stockViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMovementDialog by remember { mutableStateOf(false) }
    var showHistoryProductId by remember { mutableStateOf<ULong?>(null) }

    LaunchedEffect(sessionToken) {
        sessionToken?.let { stockViewModel.loadSummary(it) }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            stockViewModel.clearError()
        }
    }

    if (showMovementDialog) {
        RecordMovementDialog(
            onDismiss = { showMovementDialog = false },
            onConfirm = { dto ->
                sessionToken?.let { token ->
                    stockViewModel.recordMovement(token, dto) {
                        showMovementDialog = false
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading && summary == null) {
                LoadingIndicator()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Stock Summary Section
                    item {
                        Text("Stock Summary", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val s = summary
                    if (s != null && s.productIds.isNotEmpty()) {
                        itemsIndexed(s.productIds) { index, _ ->
                            val name = s.productNames.getOrElse(index) { "Unknown" }
                            val qty = s.currentQuantities.getOrElse(index) { 0 }
                            val inbound = s.inbound30d.getOrElse(index) { 0 }
                            val outbound = s.outbound30d.getOrElse(index) { 0 }
                            EntityListItem(
                                title = name,
                                subtitle = "Qty: $qty · In(30d): $inbound · Out(30d): $outbound",
                                onClick = {
                                    val pid = s.productIds[index].toULong()
                                    showHistoryProductId = pid
                                    sessionToken?.let { stockViewModel.loadHistory(it, pid) }
                                }
                            )
                        }
                    } else {
                        item {
                            Text(
                                "No stock data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Record Movement Button
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { showMovementDialog = true }) {
                            Text("Record Stock Movement")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // History Section
                    val h = history
                    if (showHistoryProductId != null && h != null && h.movementIds.isNotEmpty()) {
                        item {
                            Text("Movement History", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(h.movementIds) { index, _ ->
                            val type = h.movementTypes.getOrElse(index) { "" }
                            val qty = h.quantities.getOrElse(index) { 0 }
                            val date = h.dates.getOrElse(index) { "" }
                            val running = h.runningTotals.getOrElse(index) { 0 }
                            EntityListItem(
                                title = "$type · Qty: $qty",
                                subtitle = "$date · Running: $running"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordMovementDialog(
    onDismiss: () -> Unit,
    onConfirm: (FfiRecordStockMovementDto) -> Unit
) {
    val movementTypes = listOf(
        FfiMovementType.INBOUND, FfiMovementType.OUTBOUND,
        FfiMovementType.TRANSFER, FfiMovementType.ADJUSTMENT, FfiMovementType.RETURN
    )
    var selectedType by remember { mutableStateOf(FfiMovementType.INBOUND) }
    var productId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var fromLocationId by remember { mutableStateOf("") }
    var toLocationId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Stock Movement") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                AppDropdown(
                    items = movementTypes,
                    selectedItem = selectedType,
                    onItemSelected = { selectedType = it },
                    label = "Movement Type",
                    itemLabel = { it.name }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = productId, onValueChange = { productId = it }, label = "Product ID")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = quantity, onValueChange = { quantity = it }, label = "Quantity")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = fromLocationId, onValueChange = { fromLocationId = it }, label = "From Location ID")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = toLocationId, onValueChange = { toLocationId = it }, label = "To Location ID")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = note, onValueChange = { note = it }, label = "Note")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    FfiRecordStockMovementDto(
                        productId = productId.toULongOrNull() ?: 0u,
                        movementType = selectedType,
                        quantity = quantity.toLongOrNull() ?: 0L,
                        fromLocationId = fromLocationId.toULongOrNull() ?: 0u,
                        toLocationId = toLocationId.toULongOrNull() ?: 0u,
                        note = note
                    )
                )
            }) { Text("Record") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
