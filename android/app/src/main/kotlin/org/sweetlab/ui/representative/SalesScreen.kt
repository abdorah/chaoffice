package org.sweetlab.ui.representative

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.sweetlab.SweetLabApp
import org.sweetlab.core.Customer
import org.sweetlab.core.FinishedGood
import org.sweetlab.core.Receipt
import org.sweetlab.core.Sale
import org.sweetlab.core.SaleLineItem
import org.sweetlab.core.Wallet
import org.sweetlab.ui.util.toMoneyCents
import org.sweetlab.ui.util.toMoneyDisplay

/**
 * Local form entry for building line items before submission.
 * Uses UniFFI FinishedGood directly and stores price as Money (i64 cents).
 */
private data class LineItemEntry(
    val finishedGood: FinishedGood,
    val quantity: Int,
    val unitPriceCents: Long
) {
    val subtotalCents: Long get() = quantity.toLong() * unitPriceCents
}

/**
 * Sales screen — Representative role.
 *
 * Provides sale creation with customer picker, line item builder,
 * payment entry, wallet selector, auto-calculated total, and
 * sales history with receipt generation.
 *
 * Requirement 24.9: Representative navigates to sales screen, creates sales
 * with customer selection, line item entry, and payment via the Core_Engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Data lists — using UniFFI types directly
    val customers = remember { mutableStateListOf<Customer>() }
    val finishedGoods = remember { mutableStateListOf<FinishedGood>() }
    val wallets = remember { mutableStateListOf<Wallet>() }
    val salesHistory = remember { mutableStateListOf<Sale>() }

    // Dialog state
    var showCreateSaleDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf<Receipt?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    suspend fun loadData() {
        val token = SweetLabApp.currentSession?.sessionId ?: return
        val core = SweetLabApp.core ?: return
        try {
            isLoading = true
            errorMessage = null

            val customerList = core.getCustomers(token, null)
            customers.clear()
            customers.addAll(customerList)

            val goods = core.getFinishedGoods(token, null)
            finishedGoods.clear()
            finishedGoods.addAll(goods)

            val walletList = core.getWallets(token, null)
            wallets.clear()
            wallets.addAll(walletList)

            val sales = core.getSalesHistory(token, null)
            salesHistory.clear()
            salesHistory.addAll(sales)
        } catch (e: Exception) {
            errorMessage = "فشل تحميل بيانات المبيعات" // "Failed to load sales data"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val tabs = listOf("إنشاء عملية بيع", "سجل المبيعات") // "Create Sale", "Sales History"

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showCreateSaleDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "إنشاء عملية بيع جديدة")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error / success messages
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> SaleCreationTab(
                        customers = customers,
                        finishedGoods = finishedGoods,
                        wallets = wallets,
                        isSubmitting = isSubmitting,
                        onCreateSale = { customerId, lineItems, amountPaidCents, walletId ->
                            scope.launch {
                                val token = SweetLabApp.currentSession?.sessionId
                                val core = SweetLabApp.core
                                if (token == null || core == null) {
                                    errorMessage = "الجلسة غير متوفرة"
                                    return@launch
                                }
                                try {
                                    isSubmitting = true
                                    // Construct SaleLineItem list from form entries
                                    val saleLineItems = lineItems.map { entry ->
                                        SaleLineItem(
                                            finishedGoodId = entry.finishedGood.id,
                                            finishedGoodName = entry.finishedGood.name,
                                            quantity = entry.quantity,
                                            unitPrice = entry.unitPriceCents
                                        )
                                    }
                                    core.createSale(
                                        token,
                                        customerId,
                                        saleLineItems,
                                        amountPaidCents,
                                        walletId
                                    )
                                    successMessage = "تم إنشاء عملية البيع بنجاح" // "Sale created successfully"
                                    errorMessage = null
                                    // Refresh sales history and inventory after sale creation
                                    loadData()
                                } catch (e: Exception) {
                                    errorMessage = "فشل إنشاء عملية البيع: ${e.message}" // "Sale creation failed"
                                    successMessage = null
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    )
                    1 -> SalesHistoryTab(
                        sales = salesHistory,
                        onGenerateReceipt = { saleId ->
                            scope.launch {
                                val token = SweetLabApp.currentSession?.sessionId
                                val core = SweetLabApp.core
                                if (token == null || core == null) {
                                    errorMessage = "الجلسة غير متوفرة"
                                    return@launch
                                }
                                try {
                                    val receipt = core.generateReceipt(token, saleId)
                                    showReceiptDialog = receipt
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = "فشل إنشاء الإيصال: ${e.message}" // "Receipt generation failed"
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Create Sale Dialog ──
    if (showCreateSaleDialog) {
        CreateSaleDialog(
            customers = customers,
            finishedGoods = finishedGoods,
            wallets = wallets,
            onDismiss = { showCreateSaleDialog = false },
            onConfirm = { customerId, lineItems, amountPaidCents, walletId ->
                scope.launch {
                    val token = SweetLabApp.currentSession?.sessionId
                    val core = SweetLabApp.core
                    if (token == null || core == null) {
                        errorMessage = "الجلسة غير متوفرة"
                        showCreateSaleDialog = false
                        return@launch
                    }
                    try {
                        isSubmitting = true
                        val saleLineItems = lineItems.map { entry ->
                            SaleLineItem(
                                finishedGoodId = entry.finishedGood.id,
                                finishedGoodName = entry.finishedGood.name,
                                quantity = entry.quantity,
                                unitPrice = entry.unitPriceCents
                            )
                        }
                        core.createSale(
                            token,
                            customerId,
                            saleLineItems,
                            amountPaidCents,
                            walletId
                        )
                        successMessage = "تم إنشاء عملية البيع بنجاح"
                        errorMessage = null
                        showCreateSaleDialog = false
                        // Refresh sales history and inventory after sale creation
                        loadData()
                    } catch (e: Exception) {
                        errorMessage = "فشل إنشاء عملية البيع: ${e.message}"
                        successMessage = null
                    } finally {
                        isSubmitting = false
                    }
                }
            }
        )
    }

    // ── Receipt Dialog ──
    if (showReceiptDialog != null) {
        ReceiptDialog(
            receipt = showReceiptDialog!!,
            onDismiss = { showReceiptDialog = null }
        )
    }
}

// ── Sale Creation Tab ────────────────────────────────────────────────

/**
 * Inline sale creation form with customer picker, line item builder,
 * auto-calculated total, payment entry, and wallet selector.
 *
 * Requirement 24.9: Customer, line items (goods + quantities + prices), total, payment wallet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaleCreationTab(
    customers: List<Customer>,
    finishedGoods: List<FinishedGood>,
    wallets: List<Wallet>,
    isSubmitting: Boolean,
    onCreateSale: (customerId: String, lineItems: List<LineItemEntry>, amountPaidCents: Long, walletId: String) -> Unit
) {
    // Form state
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerExpanded by remember { mutableStateOf(false) }
    val lineItems = remember { mutableStateListOf<LineItemEntry>() }
    var amountPaidText by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    // Line item builder state
    var selectedGood by remember { mutableStateOf<FinishedGood?>(null) }
    var goodExpanded by remember { mutableStateOf(false) }
    var lineQtyText by remember { mutableStateOf("") }
    var linePriceText by remember { mutableStateOf("") }

    // Auto-calculated total in cents
    val saleTotalCents = lineItems.sumOf { it.subtotalCents }
    val amountPaidCents = (amountPaidText.toDoubleOrNull() ?: 0.0).toMoneyCents()
    val remainingBalanceCents = saleTotalCents - amountPaidCents

    val canSubmit = selectedCustomer != null &&
            lineItems.isNotEmpty() &&
            selectedWallet != null &&
            amountPaidCents >= 0 &&
            amountPaidCents <= saleTotalCents &&
            !isSubmitting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Customer Picker ──
        Text(text = "العميل", style = MaterialTheme.typography.titleMedium) // "Customer"
        ExposedDropdownMenuBox(
            expanded = customerExpanded,
            onExpandedChange = { customerExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedCustomer?.let { "${it.name} — ${it.city}" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("اختر العميل") }, // "Select Customer"
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = customerExpanded,
                onDismissRequest = { customerExpanded = false }
            ) {
                customers.forEach { customer ->
                    DropdownMenuItem(
                        text = { Text("${customer.name} — ${customer.city} (${customer.mobile})") },
                        onClick = {
                            selectedCustomer = customer
                            customerExpanded = false
                        }
                    )
                }
            }
        }

        HorizontalDivider()

        // ── Line Item Builder ──
        Text(text = "بنود البيع", style = MaterialTheme.typography.titleMedium) // "Sale Line Items"

        // Add line item controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Finished good picker
                ExposedDropdownMenuBox(
                    expanded = goodExpanded,
                    onExpandedChange = { goodExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedGood?.let { "${it.name} (متوفر: ${"%.0f".format(it.currentQuantity)})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المنتج") }, // "Product"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = goodExpanded,
                        onDismissRequest = { goodExpanded = false }
                    ) {
                        finishedGoods.forEach { good ->
                            DropdownMenuItem(
                                text = { Text("${good.name} (متوفر: ${"%.0f".format(good.currentQuantity)}) — ${good.unitPrice.toMoneyDisplay()}") },
                                onClick = {
                                    selectedGood = good
                                    linePriceText = good.unitPrice.toMoneyDisplay()
                                    goodExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = lineQtyText,
                        onValueChange = { lineQtyText = it },
                        label = { Text("الكمية") }, // "Quantity"
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = linePriceText,
                        onValueChange = { linePriceText = it },
                        label = { Text("سعر الوحدة") }, // "Unit Price"
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                val lineQty = lineQtyText.toIntOrNull() ?: 0
                val linePrice = linePriceText.toDoubleOrNull() ?: 0.0
                val canAddLine = selectedGood != null && lineQty > 0 && linePrice > 0

                TextButton(
                    onClick = {
                        if (canAddLine && selectedGood != null) {
                            val priceCents = (linePrice * 100).toLong()
                            lineItems.add(
                                LineItemEntry(
                                    finishedGood = selectedGood!!,
                                    quantity = lineQty,
                                    unitPriceCents = priceCents
                                )
                            )
                            selectedGood = null
                            lineQtyText = ""
                            linePriceText = ""
                        }
                    },
                    enabled = canAddLine,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("إضافة بند") // "Add Line Item"
                }
            }
        }

        // Current line items list
        if (lineItems.isNotEmpty()) {
            lineItems.forEachIndexed { index, item ->
                LineItemCard(
                    item = item,
                    onRemove = { lineItems.removeAt(index) }
                )
            }
        } else {
            Text(
                text = "لم يتم إضافة بنود بعد", // "No line items added yet"
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        // ── Sale Total (auto-calculated) ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "إجمالي البيع:", style = MaterialTheme.typography.titleMedium) // "Sale Total:"
                    Text(
                        text = saleTotalCents.toMoneyDisplay(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (amountPaidCents > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "المبلغ المدفوع:", style = MaterialTheme.typography.bodyMedium) // "Amount Paid:"
                        Text(text = amountPaidCents.toMoneyDisplay(), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الرصيد المتبقي:", // "Remaining Balance:"
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remainingBalanceCents > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = remainingBalanceCents.toMoneyDisplay(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remainingBalanceCents > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ── Payment Entry ──
        Text(text = "الدفع", style = MaterialTheme.typography.titleMedium) // "Payment"

        OutlinedTextField(
            value = amountPaidText,
            onValueChange = { amountPaidText = it },
            label = { Text("المبلغ المدفوع") }, // "Amount Paid"
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = amountPaidCents > saleTotalCents,
            supportingText = {
                if (amountPaidCents > saleTotalCents) {
                    Text("المبلغ المدفوع لا يمكن أن يتجاوز الإجمالي") // "Amount paid cannot exceed total"
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // ── Wallet Selector ──
        ExposedDropdownMenuBox(
            expanded = walletExpanded,
            onExpandedChange = { walletExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedWallet?.let { "${it.name} (${it.currentBalance.toMoneyDisplay()})" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("محفظة الدفع") }, // "Payment Wallet"
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = walletExpanded,
                onDismissRequest = { walletExpanded = false }
            ) {
                wallets.forEach { wallet ->
                    DropdownMenuItem(
                        text = { Text("${wallet.name} (${wallet.currentBalance.toMoneyDisplay()})") },
                        onClick = {
                            selectedWallet = wallet
                            walletExpanded = false
                        }
                    )
                }
            }
        }

        // ── Create Sale Button ──
        TextButton(
            onClick = {
                if (canSubmit && selectedCustomer != null && selectedWallet != null) {
                    onCreateSale(
                        selectedCustomer!!.id,
                        lineItems.toList(),
                        amountPaidCents,
                        selectedWallet!!.id
                    )
                    // Reset form
                    selectedCustomer = null
                    lineItems.clear()
                    amountPaidText = ""
                    selectedWallet = null
                }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("إنشاء عملية البيع", style = MaterialTheme.typography.titleMedium) // "Create Sale"
        }

        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
    }
}

/**
 * Card displaying a single line item with product name, quantity, unit price,
 * subtotal, and a remove button.
 */
@Composable
private fun LineItemCard(
    item: LineItemEntry,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.finishedGood.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${item.quantity} × ${item.unitPriceCents.toMoneyDisplay()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.subtotalCents.toMoneyDisplay(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف البند", // "Remove line item"
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Sales History Tab ────────────────────────────────────────────────

/**
 * Displays the list of past sales with receipt generation button per sale.
 */
@Composable
private fun SalesHistoryTab(
    sales: List<Sale>,
    onGenerateReceipt: (saleId: String) -> Unit
) {
    if (sales.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "لا توجد مبيعات بعد", // "No sales yet"
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sales, key = { it.id }) { sale ->
                SaleHistoryCard(sale = sale, onGenerateReceipt = { onGenerateReceipt(sale.id) })
            }
        }
    }
}

@Composable
private fun SaleHistoryCard(
    sale: Sale,
    onGenerateReceipt: () -> Unit
) {
    val remainingCents = sale.totalAmount - sale.amountPaid
    val isFullyPaid = remainingCents <= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = sale.customerName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${sale.lineItems.size} بنود", // "X items"
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Receipt generation button
                IconButton(onClick = onGenerateReceipt) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = "إنشاء إيصال", // "Generate Receipt"
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "الإجمالي:", style = MaterialTheme.typography.bodyMedium) // "Total:"
                Text(
                    text = sale.totalAmount.toMoneyDisplay(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "المدفوع:", style = MaterialTheme.typography.bodyMedium) // "Paid:"
                Text(text = sale.amountPaid.toMoneyDisplay(), style = MaterialTheme.typography.bodyMedium)
            }
            if (!isFullyPaid) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المتبقي:", // "Remaining:"
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = remainingCents.toMoneyDisplay(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = sale.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ── Create Sale Dialog ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSaleDialog(
    customers: List<Customer>,
    finishedGoods: List<FinishedGood>,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onConfirm: (customerId: String, lineItems: List<LineItemEntry>, amountPaidCents: Long, walletId: String) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerExpanded by remember { mutableStateOf(false) }
    val lineItems = remember { mutableStateListOf<LineItemEntry>() }
    var amountPaidText by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    // Line item builder state
    var selectedGood by remember { mutableStateOf<FinishedGood?>(null) }
    var goodExpanded by remember { mutableStateOf(false) }
    var lineQtyText by remember { mutableStateOf("") }
    var linePriceText by remember { mutableStateOf("") }

    val saleTotalCents = lineItems.sumOf { it.subtotalCents }
    val amountPaidCents = (amountPaidText.toDoubleOrNull() ?: 0.0).toMoneyCents()

    val canSubmit = selectedCustomer != null &&
            lineItems.isNotEmpty() &&
            selectedWallet != null &&
            amountPaidCents >= 0 &&
            amountPaidCents <= saleTotalCents

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء عملية بيع جديدة") }, // "Create New Sale"
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Customer picker
                ExposedDropdownMenuBox(
                    expanded = customerExpanded,
                    onExpandedChange = { customerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.let { "${it.name} — ${it.city}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("العميل") }, // "Customer"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = { Text("${customer.name} — ${customer.city}") },
                                onClick = {
                                    selectedCustomer = customer
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()
                Text(text = "بنود البيع", style = MaterialTheme.typography.labelLarge) // "Line Items"

                // Add line item — product picker
                ExposedDropdownMenuBox(
                    expanded = goodExpanded,
                    onExpandedChange = { goodExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedGood?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المنتج") }, // "Product"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = goodExpanded,
                        onDismissRequest = { goodExpanded = false }
                    ) {
                        finishedGoods.forEach { good ->
                            DropdownMenuItem(
                                text = { Text("${good.name} (${good.unitPrice.toMoneyDisplay()})") },
                                onClick = {
                                    selectedGood = good
                                    linePriceText = good.unitPrice.toMoneyDisplay()
                                    goodExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lineQtyText,
                        onValueChange = { lineQtyText = it },
                        label = { Text("الكمية") }, // "Qty"
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = linePriceText,
                        onValueChange = { linePriceText = it },
                        label = { Text("السعر") }, // "Price"
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                val lineQty = lineQtyText.toIntOrNull() ?: 0
                val linePrice = linePriceText.toDoubleOrNull() ?: 0.0

                TextButton(
                    onClick = {
                        if (selectedGood != null && lineQty > 0 && linePrice > 0) {
                            val priceCents = (linePrice * 100).toLong()
                            lineItems.add(LineItemEntry(selectedGood!!, lineQty, priceCents))
                            selectedGood = null
                            lineQtyText = ""
                            linePriceText = ""
                        }
                    },
                    enabled = selectedGood != null && lineQty > 0 && linePrice > 0
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("إضافة") // "Add"
                }

                // Current line items
                lineItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.finishedGood.name}: ${item.quantity}×${item.unitPriceCents.toMoneyDisplay()} = ${item.subtotalCents.toMoneyDisplay()}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { lineItems.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (lineItems.isNotEmpty()) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "الإجمالي:", style = MaterialTheme.typography.titleSmall) // "Total:"
                        Text(
                            text = saleTotalCents.toMoneyDisplay(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider()

                // Payment amount
                OutlinedTextField(
                    value = amountPaidText,
                    onValueChange = { amountPaidText = it },
                    label = { Text("المبلغ المدفوع") }, // "Amount Paid"
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Wallet selector
                ExposedDropdownMenuBox(
                    expanded = walletExpanded,
                    onExpandedChange = { walletExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWallet?.let { "${it.name} (${it.currentBalance.toMoneyDisplay()})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("محفظة الدفع") }, // "Payment Wallet"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = walletExpanded,
                        onDismissRequest = { walletExpanded = false }
                    ) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text("${wallet.name} (${wallet.currentBalance.toMoneyDisplay()})") },
                                onClick = {
                                    selectedWallet = wallet
                                    walletExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canSubmit && selectedCustomer != null && selectedWallet != null) {
                        onConfirm(selectedCustomer!!.id, lineItems.toList(), amountPaidCents, selectedWallet!!.id)
                    }
                },
                enabled = canSubmit
            ) {
                Text("إنشاء") // "Create"
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء") // "Cancel"
            }
        }
    )
}

// ── Receipt Dialog ───────────────────────────────────────────────────

/**
 * Displays a receipt populated from the UniFFI Receipt type.
 * Shows customer info, itemized list, total, amount paid, remaining balance, date.
 */
@Composable
private fun ReceiptDialog(
    receipt: Receipt,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إيصال البيع") }, // "Sale Receipt"
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Business name
                Text(
                    text = receipt.businessName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                HorizontalDivider()

                // Customer info
                Text(text = "العميل: ${receipt.customerName}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "المدينة: ${receipt.customerCity}", style = MaterialTheme.typography.bodySmall)
                Text(text = "الهاتف: ${receipt.customerMobile}", style = MaterialTheme.typography.bodySmall)
                Text(text = "التاريخ: ${receipt.formattedDate}", style = MaterialTheme.typography.bodySmall)

                HorizontalDivider()

                // Itemized list
                Text(text = "البنود:", style = MaterialTheme.typography.labelLarge) // "Items:"
                receipt.sale.lineItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.finishedGoodName} (${item.quantity}×${item.unitPrice.toMoneyDisplay()})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = (item.quantity.toLong() * item.unitPrice).toMoneyDisplay(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider()

                // Totals
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "الإجمالي:", style = MaterialTheme.typography.titleSmall) // "Total:"
                    Text(
                        text = receipt.sale.totalAmount.toMoneyDisplay(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "المدفوع:", style = MaterialTheme.typography.bodyMedium) // "Paid:"
                    Text(text = receipt.sale.amountPaid.toMoneyDisplay(), style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المتبقي:", // "Remaining:"
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (receipt.remainingBalance > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = receipt.remainingBalance.toMoneyDisplay(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (receipt.remainingBalance > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق") // "Close"
            }
        }
    )
}
