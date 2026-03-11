package org.sweetlab.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Placeholder data classes until UniFFI bindings are generated
private data class FinancialSummaryData(
    val totalRevenue: Double,
    val totalExpenses: Double,
    val netProfit: Double
)

private data class InventoryReportItem(
    val name: String,
    val quantity: Double,
    val unit: String,
    val isLowStock: Boolean,
    val isRawMaterial: Boolean
)

private data class DebtReportItem(
    val customerName: String,
    val totalOwed: Double,
    val overdueDays: Int,
    val isCritical: Boolean
)

private data class ExpenseReportGroup(
    val category: String,
    val categoryLabel: String,
    val expenses: List<ExpenseReportItem>,
    val subtotal: Double
)

private data class ExpenseReportItem(
    val description: String,
    val amount: Double,
    val walletName: String,
    val timestamp: String
)

private fun formatDate(millis: Long?): String {
    if (millis == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

/**
 * Reporting screen — tabbed view for financial summary, inventory report,
 * debt aging report, and expense report with PDF export.
 *
 * Requirement 10.2: Debt aging report sorted by overdue days.
 * Requirement 11.4: Expense report grouped by category.
 * Requirement 12.1: Financial summary with revenue, expenses, net profit.
 * Requirement 12.2: Inventory report with low-stock highlights.
 * Requirement 12.4: PDF export support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "مالي",      // "Financial"
        "المخزون",   // "Inventory"
        "الديون",    // "Debts"
        "المصروفات"  // "Expenses"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير") }, // "Reports"
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> FinancialSummaryTab()
                1 -> InventoryReportTab()
                2 -> DebtAgingReportTab()
                3 -> ExpenseReportTab()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinancialSummaryTab() {
    val scope = rememberCoroutineScope()
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<FinancialSummaryData?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "الملخص المالي", style = MaterialTheme.typography.titleMedium) // "Financial Summary"

        // Date range picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(startDateMillis?.let { "من: ${formatDate(it)}" } ?: "تاريخ البداية")
                // "From: date" / "Start date"
            }
            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(endDateMillis?.let { "إلى: ${formatDate(it)}" } ?: "تاريخ النهاية")
                // "To: date" / "End date"
            }
        }

        Button(
            onClick = {
                scope.launch {
                    // TODO: SweetLabApp.core?.getFinancialSummary(startDate, endDate)
                    summary = FinancialSummaryData(0.0, 0.0, 0.0) // placeholder
                }
            },
            enabled = startDateMillis != null && endDateMillis != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("عرض التقرير") // "Show Report"
        }

        if (summary != null) {
            SummaryRow("إجمالي الإيرادات", summary!!.totalRevenue) // "Total Revenue"
            SummaryRow("إجمالي المصروفات", summary!!.totalExpenses) // "Total Expenses"
            HorizontalDivider()
            SummaryRow(
                "صافي الربح", // "Net Profit"
                summary!!.netProfit,
                isHighlight = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // PDF export button (Req 12.4)
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isExporting = true
                        // TODO: SweetLabApp.core?.exportToPdf(...)
                        isExporting = false
                    }
                },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تصدير PDF") // "Export PDF"
            }
        }
    }

    // Date picker dialogs
    if (showStartPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDateMillis = state.selectedDateMillis
                    showStartPicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("إلغاء") }
            }
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDateMillis = state.selectedDateMillis
                    showEndPicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("إلغاء") }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SummaryRow(label: String, value: Double, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "%.2f".format(value),
            style = if (isHighlight) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodyMedium,
            color = if (isHighlight && value >= 0) MaterialTheme.colorScheme.primary
            else if (isHighlight) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
private fun InventoryReportTab() {
    val items = remember { mutableStateListOf<InventoryReportItem>() }

    LaunchedEffect(Unit) {
        // TODO: SweetLabApp.core?.getInventoryReport(10.0)
        // items.addAll(report.rawMaterials.map { ... } + report.finishedGoods.map { ... })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "تقرير المخزون", style = MaterialTheme.typography.titleMedium) // "Inventory Report"
        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            Text(
                text = "لا توجد بيانات مخزون", // "No inventory data"
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isLowStock) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.isLowStock) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "مخزون منخفض",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (item.isRawMaterial) "مادة خام" else "منتج نهائي",
                                    // "Raw material" / "Finished good"
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${item.quantity} ${item.unit}",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (item.isLowStock) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtAgingReportTab() {
    val debts = remember { mutableStateListOf<DebtReportItem>() }

    LaunchedEffect(Unit) {
        // TODO: SweetLabApp.core?.getDebtAgingReport()
        // debts.addAll(result.map { DebtReportItem(it.customerName, it.remainingAmount, it.overdueDays, it.isCritical) })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "تقرير أعمار الديون", style = MaterialTheme.typography.titleMedium) // "Debt Aging Report"
        Spacer(modifier = Modifier.height(8.dp))

        if (debts.isEmpty()) {
            Text(
                text = "لا توجد ديون نشطة", // "No active debts"
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(debts) { debt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (debt.isCritical) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = debt.customerName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "متأخر ${debt.overdueDays} يوم", // "Overdue X days"
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (debt.isCritical) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (debt.isCritical) {
                                    Text(
                                        text = "⚠️ حرج", // "Critical"
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = "%.2f".format(debt.totalOwed),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseReportTab() {
    val groups = remember { mutableStateListOf<ExpenseReportGroup>() }
    var grandTotal by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(Unit) {
        // TODO: SweetLabApp.core?.getExpensesByCategory(startDate, endDate)
        // groups.addAll(result.map { ... })
        // grandTotal = groups.sumOf { it.subtotal }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "تقرير المصروفات", style = MaterialTheme.typography.titleMedium) // "Expense Report"
        Spacer(modifier = Modifier.height(8.dp))

        if (groups.isEmpty()) {
            Text(
                text = "لا توجد مصروفات مسجلة", // "No expenses recorded"
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(groups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = group.categoryLabel,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "%.2f".format(group.subtotal),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            group.expenses.forEach { expense ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = expense.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${expense.walletName} — ${expense.timestamp}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "%.2f".format(expense.amount),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Grand total
                item {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الإجمالي الكلي", // "Grand Total"
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "%.2f".format(grandTotal),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
