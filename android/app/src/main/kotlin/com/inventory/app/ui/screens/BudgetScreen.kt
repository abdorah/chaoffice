package com.inventory.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.AppDropdown
import com.inventory.app.ui.components.AppTextField
import com.inventory.app.ui.components.LoadingIndicator
import com.inventory.app.viewmodel.BudgetViewModel
import com.inventory.ffi.FfiBudgetEntryType
import com.inventory.ffi.FfiRecordBudgetEntryDto

@Composable
fun BudgetScreen(
    budgetViewModel: BudgetViewModel,
    sessionToken: String?
) {
    val summary by budgetViewModel.summary.collectAsState()
    val projection by budgetViewModel.projection.collectAsState()
    val isLoading by budgetViewModel.isLoading.collectAsState()
    val error by budgetViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEntryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionToken) {
        sessionToken?.let {
            budgetViewModel.loadSummary(it, "2000-01-01T00:00:00Z", "2099-12-31T23:59:59Z")
            budgetViewModel.loadProjection(it, 6)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            budgetViewModel.clearError()
        }
    }

    if (showEntryDialog) {
        RecordBudgetEntryDialog(
            onDismiss = { showEntryDialog = false },
            onConfirm = { dto ->
                sessionToken?.let { token ->
                    budgetViewModel.recordEntry(token, dto) {
                        showEntryDialog = false
                        budgetViewModel.loadSummary(token, "2000-01-01T00:00:00Z", "2099-12-31T23:59:59Z")
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
                    // Summary Section
                    item {
                        Text("Budget Summary", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        val s = summary
                        if (s != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Purchases: ${String.format("%.2f", s.totalPurchases)}")
                                    Text("Sales: ${String.format("%.2f", s.totalSales)}")
                                    Text("Expenses: ${String.format("%.2f", s.totalExpenses)}")
                                    Text(
                                        "Net Balance: ${String.format("%.2f", s.netBalance)}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        } else {
                            Text(
                                "No budget data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Record Entry Button
                    item {
                        TextButton(onClick = { showEntryDialog = true }) {
                            Text("Record Budget Entry")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Projection Section
                    val p = projection
                    if (p != null && p.monthLabels.isNotEmpty()) {
                        item {
                            Text("Projection (6 months)", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(p.monthLabels) { index, label ->
                            val income = p.projectedIncome.getOrElse(index) { 0.0 }
                            val expenses = p.projectedExpenses.getOrElse(index) { 0.0 }
                            val balance = p.projectedBalance.getOrElse(index) { 0.0 }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(label, style = MaterialTheme.typography.titleSmall)
                                    Text("Income: ${String.format("%.2f", income)} · Expenses: ${String.format("%.2f", expenses)}")
                                    Text("Balance: ${String.format("%.2f", balance)}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordBudgetEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (FfiRecordBudgetEntryDto) -> Unit
) {
    val entryTypes = listOf(
        FfiBudgetEntryType.PURCHASE, FfiBudgetEntryType.SALE,
        FfiBudgetEntryType.EXPENSE, FfiBudgetEntryType.FORECAST
    )
    var selectedType by remember { mutableStateOf(FfiBudgetEntryType.PURCHASE) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf("") }
    var productId by remember { mutableStateOf("") }
    var dealId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Budget Entry") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                AppDropdown(
                    items = entryTypes,
                    selectedItem = selectedType,
                    onItemSelected = { selectedType = it },
                    label = "Entry Type",
                    itemLabel = { it.name }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = amount, onValueChange = { amount = it }, label = "Amount")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = description, onValueChange = { description = it }, label = "Description")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = entryDate, onValueChange = { entryDate = it }, label = "Entry Date (ISO 8601)")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = productId, onValueChange = { productId = it }, label = "Product ID")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = dealId, onValueChange = { dealId = it }, label = "Deal ID")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    FfiRecordBudgetEntryDto(
                        entryType = selectedType,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        description = description,
                        entryDate = entryDate.ifEmpty { "2024-01-01T00:00:00Z" },
                        productId = productId.toLongOrNull() ?: 0L,
                        dealId = dealId.toLongOrNull() ?: 0L
                    )
                )
            }) { Text("Record") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
