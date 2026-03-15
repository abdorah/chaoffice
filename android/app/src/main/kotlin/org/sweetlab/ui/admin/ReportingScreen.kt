package org.sweetlab.ui.admin

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import org.sweetlab.SweetLabApp
import org.sweetlab.core.DebtRecord
import org.sweetlab.core.ExpenseCategory
import org.sweetlab.core.ExpenseCategoryGroup
import org.sweetlab.core.FinancialSummary
import org.sweetlab.core.InventoryReport
import org.sweetlab.core.Invoice
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.sweetlab.ui.util.toMoneyDisplay

private fun millisToIsoDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
}

private fun formatDate(millis: Long?): String {
    if (millis == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun expenseCategoryLabel(category: ExpenseCategory): String = when (category) {
    ExpenseCategory.PURCHASE -> "مشتريات"
    ExpenseCategory.OPERATING_COST -> "تكاليف تشغيل"
}

private fun sharePdf(context: Context, pdfBytes: List<UByte>, fileName: String) {
    val reportsDir = File(context.cacheDir, "reports")
    reportsDir.mkdirs()
    val pdfFile = File(reportsDir, fileName)
    pdfFile.writeBytes(pdfBytes.map { it.toByte() }.toByteArray())
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "مشاركة التقرير"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreen(onNavigateBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("مالي", "المخزون", "الديون", "المصروفات")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير") },
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
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


// ── Financial Summary Tab ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinancialSummaryTab() {
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf<FinancialSummary?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Date range state — default to last 30 days
    val now = System.currentTimeMillis()
    var startDateMillis by remember { mutableStateOf(now - 30L * 24 * 60 * 60 * 1000) }
    var endDateMillis by remember { mutableStateOf(now) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    fun loadSummary() {
        scope.launch {
            val token = SweetLabApp.currentSession?.sessionId ?: return@launch
            val core = SweetLabApp.core ?: return@launch
            try {
                isLoading = true
                errorMessage = null
                val startIso = millisToIsoDateTime(startDateMillis)
                val endIso = millisToIsoDateTime(endDateMillis)
                summary = core.getFinancialSummary(token, startIso, endIso)
            } catch (e: Exception) {
                errorMessage = "فشل تحميل الملخص المالي"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadSummary() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Date range selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                Text("من: ${formatDate(startDateMillis)}")
            }
            OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                Text("إلى: ${formatDate(endDateMillis)}")
            }
            Button(onClick = { loadSummary() }) { Text("تحديث") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (summary != null) {
            val s = summary!!
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("الملخص المالي", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryRow("إجمالي الإيرادات", s.totalRevenue.toMoneyDisplay())
                    SummaryRow("إجمالي المصروفات", s.totalExpenses.toMoneyDisplay())
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SummaryRow("صافي الربح", s.netProfit.toMoneyDisplay())
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wallet balances
            if (s.walletBalances.isNotEmpty()) {
                Text("أرصدة المحافظ", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                s.walletBalances.forEach { wallet ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Text(wallet.name, modifier = Modifier.weight(1f))
                            Text(wallet.currentBalance.toMoneyDisplay(), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Date picker dialogs
    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startDateMillis = it }
                    showStartPicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("إلغاء") } }
        ) { DatePicker(state = state) }
    }
    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { endDateMillis = it }
                    showEndPicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("إلغاء") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}


// ── Inventory Report Tab ──

@Composable
private fun InventoryReportTab() {
    var report by remember { mutableStateOf<InventoryReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = SweetLabApp.currentSession?.sessionId
        val core = SweetLabApp.core
        if (token == null || core == null) {
            errorMessage = "الجلسة غير متوفرة"
            isLoading = false
            return@LaunchedEffect
        }
        try {
            isLoading = true
            errorMessage = null
            report = core.getInventoryReport(token, 10.0)
        } catch (e: Exception) {
            errorMessage = "فشل تحميل تقرير المخزون"
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val r = report ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Raw materials section
        item {
            Text("المواد الخام", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (r.rawMaterials.isEmpty()) {
            item { Text("لا توجد مواد خام", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(r.rawMaterials, key = { it.material.id }) { rm ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (rm.isLowStock) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rm.material.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "الكمية: ${"%.1f".format(rm.material.currentQuantity)} ${rm.material.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (rm.isLowStock) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "مخزون منخفض",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Finished goods section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("المنتجات النهائية", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (r.finishedGoods.isEmpty()) {
            item { Text("لا توجد منتجات نهائية", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(r.finishedGoods, key = { it.good.id }) { fg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (fg.isLowStock) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fg.good.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "الكمية: ${"%.1f".format(fg.good.currentQuantity)} — السعر: ${fg.good.unitPrice.toMoneyDisplay()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (fg.isLowStock) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "مخزون منخفض",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}


// ── Debt Aging Report Tab ──

@Composable
private fun DebtAgingReportTab() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val debts = remember { mutableStateListOf<DebtRecord>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pdfExportingSaleId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = SweetLabApp.currentSession?.sessionId
        val core = SweetLabApp.core
        if (token == null || core == null) {
            errorMessage = "الجلسة غير متوفرة"
            isLoading = false
            return@LaunchedEffect
        }
        try {
            isLoading = true
            errorMessage = null
            val result = core.getDebtAgingReport(token, null)
            debts.clear()
            debts.addAll(result)
        } catch (e: Exception) {
            errorMessage = "فشل تحميل تقرير الديون"
        } finally {
            isLoading = false
        }
    }

    fun exportInvoicePdf(saleId: String) {
        scope.launch {
            val token = SweetLabApp.currentSession?.sessionId ?: return@launch
            val core = SweetLabApp.core ?: return@launch
            try {
                pdfExportingSaleId = saleId
                val invoice = core.generateInvoice(token, saleId)
                val pdfBytes = core.exportToPdf(invoice)
                sharePdf(context, pdfBytes, "invoice_${invoice.invoiceNumber}.pdf")
            } catch (e: Exception) {
                errorMessage = "فشل تصدير الفاتورة: ${e.message}"
            } finally {
                pdfExportingSaleId = null
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    if (debts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("لا توجد ديون نشطة", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("تقرير أعمار الديون", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(debts, key = { it.id }) { debt ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (debt.isCritical) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            debt.customerName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        // PDF export button for this debt's sale invoice
                        IconButton(
                            onClick = { exportInvoicePdf(debt.saleId) },
                            enabled = pdfExportingSaleId == null
                        ) {
                            if (pdfExportingSaleId == debt.saleId) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(20.dp).height(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "تصدير فاتورة PDF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (debt.isCritical) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "دين حرج",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "المبلغ الأصلي: ${debt.originalAmount.toMoneyDisplay()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "المتبقي: ${debt.remainingAmount.toMoneyDisplay()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (debt.isCritical) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "تاريخ البيع: ${debt.saleDate} — متأخر ${debt.overdueDays} يوم",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


// ── Expense Report Tab ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseReportTab() {
    val scope = rememberCoroutineScope()
    val groups = remember { mutableStateListOf<ExpenseCategoryGroup>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Date range — default to last 30 days
    val now = System.currentTimeMillis()
    var startDateMillis by remember { mutableStateOf(now - 30L * 24 * 60 * 60 * 1000) }
    var endDateMillis by remember { mutableStateOf(now) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    fun loadExpenses() {
        scope.launch {
            val token = SweetLabApp.currentSession?.sessionId ?: return@launch
            val core = SweetLabApp.core ?: return@launch
            try {
                isLoading = true
                errorMessage = null
                val startIso = millisToIsoDateTime(startDateMillis)
                val endIso = millisToIsoDateTime(endDateMillis)
                val result = core.getExpensesByCategory(token, startIso, endIso, null)
                groups.clear()
                groups.addAll(result)
            } catch (e: Exception) {
                errorMessage = "فشل تحميل تقرير المصروفات"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadExpenses() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Date range selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                Text("من: ${formatDate(startDateMillis)}")
            }
            OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                Text("إلى: ${formatDate(endDateMillis)}")
            }
            Button(onClick = { loadExpenses() }) { Text("تحديث") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (groups.isEmpty()) {
            Text("لا توجد مصروفات في هذه الفترة", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { it.category.name }) { group ->
                    val categoryTotal = group.expenses.sumOf { it.amount }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    expenseCategoryLabel(group.category),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    categoryTotal.toMoneyDisplay(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            group.expenses.forEach { expense ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(expense.description, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            expense.timestamp,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        expense.amount.toMoneyDisplay(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Date picker dialogs
    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startDateMillis = it }
                    showStartPicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("إلغاء") } }
        ) { DatePicker(state = state) }
    }
    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { endDateMillis = it }
                    showEndPicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("إلغاء") } }
        ) { DatePicker(state = state) }
    }
}
