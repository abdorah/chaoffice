package org.sweetlab.ui.representative

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// ── Placeholder data classes until UniFFI bindings are generated ──

private data class CustomerItem(
    val id: String,
    val name: String,
    val city: String,
    val mobile: String
)

private data class FinishedGoodItem(
    val id: String,
    val name: String,
    val currentQuantity: Double,
    val unitPrice: Double
)

private data class WalletItem(
    val id: String,
    val name: String,
    val walletType: String,
    val currentBalance: Double
)

private data class LineItemEntry(
    val finishedGood: FinishedGoodItem,
    val quantity: Int,
    val unitPrice: Double
) {
    val subtotal: Double get() = quantity * unitPrice
}

private data class SaleItem(
    val id: String,
    val customerName: String,
    val totalAmount: Double,
    val amountPaid: Double,
    val timestamp: String,
    val lineItemCount: Int
)

private data class ReceiptData(
    val customerName: String,
    val customerCity: String,
    val customerMobile: String,
    val businessName: String,
    val lineItems: List<ReceiptLineItem>,
    val totalAmount: Double,
    val amountPaid: Double,
    val remainingBalance: Double,
    val formattedDate: String
)

private data class ReceiptLineItem(
    val finishedGoodName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

/**
 * Sales screen — Representative role.
 *
 * Provides sale creation with customer picker, line item builder,
 * payment entry, wallet selector, auto-calculated total, and
 * sales history with receipt generation.
 *
 * Requirement 8.1: Record sale with customer, line items, total, payment wallet, timestamp.
 * Requirement 8.2: Full payment → credit wallet + deduct inventory.
 * Requirement 8.3: Partial/no payment → create debt record for unpaid balance.
 * Requirement 8.4: Sale total = sum(qty × unit_price).
 * Requirement 8.5: Generate printable receipt with customer name, items, total, paid, remaining, date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Data lists
    val customers = remember { mutableStateListOf<CustomerItem>() }
    val finishedGoods = remember { mutableStateListOf<FinishedGoodItem>() }
    val wallets = remember { mutableStateListOf<WalletItem>() }
    val salesHistory = remember { mutableStateListOf<SaleItem>() }

    // Dialog state
    var showCreateSaleDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf<ReceiptData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // TODO: Replace with SweetLabCore calls
        // val customerList = SweetLabApp.core?.getCustomers() ?: emptyList()
        // customers.addAll(customerList.map { CustomerItem(it.id, it.name, it.city, it.mobile) })
        // val goods = SweetLabApp.core?.getFinishedGoods() ?: emptyList()
        // finishedGoods.addAll(goods.map { FinishedGoodItem(it.id, it.name, it.currentQuantity, it.unitPrice) })
        // val walletList = SweetLabApp.core?.getWallets() ?: emptyList()
        // wallets.addAll(walletList.map { WalletItem(it.id, it.name, it.walletType.name, it.currentBalance) })
        // val sales = SweetLabApp.core?.getSalesHistory() ?: emptyList()
        // salesHistory.addAll(sales.map { SaleItem(it.id, it.customerName, it.totalAmount, it.amountPaid, it.timestamp.toString(), it.lineItems.size) })
    }

    val tabs = listOf("إنشاء عملية بيع", "سجل المبيعات") // "Create Sale", "Sales History"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("شاشة المبيعات") }, // "Sales Screen"
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
        },
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

            when (selectedTab) {
                0 -> SaleCreationTab(
                    customers = customers,
                    finishedGoods = finishedGoods,
                    wallets = wallets,
                    onCreateSale = { customerId, lineItems, amountPaid, walletId ->
                        scope.launch {
                            try {
                                // TODO: Replace with SweetLabCore call
                                // val sale = SweetLabApp.core?.createSale(
                                //     customerId, lineItems.map {
                                //         SaleLineItem(it.finishedGood.id, it.finishedGood.name, it.quantity, it.unitPrice)
                                //     }, amountPaid, walletId
                                // )
                                // salesHistory.add(0, SaleItem(
                                //     sale.id, sale.customerName, sale.totalAmount,
                                //     sale.amountPaid, sale.timestamp.toString(), sale.lineItems.size
                                // ))

                                successMessage = "تم إنشاء عملية البيع بنجاح" // "Sale created successfully"
                                errorMessage = null
                            } catch (e: Exception) {
                                errorMessage = "فشل إنشاء عملية البيع: ${e.message}" // "Sale creation failed"
                                successMessage = null
                            }
                        }
                    }
                )
                1 -> SalesHistoryTab(
                    sales = salesHistory,
                    onGenerateReceipt = { saleId ->
                        scope.launch {
                            try {
                                // TODO: Replace with SweetLabCore call
                                // val receipt = SweetLabApp.core?.generateReceipt(saleId)
                                // showReceiptDialog = ReceiptData(
                                //     customerName = receipt.customerName,
                                //     customerCity = receipt.customerCity,
                                //     customerMobile = receipt.customerMobile,
                                //     businessName = receipt.businessName,
                                //     lineItems = receipt.sale.lineItems.map {
                                //         ReceiptLineItem(it.finishedGoodName, it.quantity, it.unitPrice, it.quantity * it.unitPrice)
                                //     },
                                //     totalAmount = receipt.sale.totalAmount,
                                //     amountPaid = receipt.sale.amountPaid,
                                //     remainingBalance = receipt.remainingBalance,
                                //     formattedDate = receipt.formattedDate
                                // )
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

    // ── Create Sale Dialog ──
    if (showCreateSaleDialog) {
        CreateSaleDialog(
            customers = customers,
            finishedGoods = finishedGoods,
            wallets = wallets,
            onDismiss = { showCreateSaleDialog = false },
            onConfirm = { customerId, lineItems, amountPaid, walletId ->
                scope.launch {
                    try {
                        // TODO: Replace with SweetLabCore call
                        // val sale = SweetLabApp.core?.createSale(customerId, lineItems, amountPaid, walletId)
                        successMessage = "تم إنشاء عملية البيع بنجاح"
                        errorMessage = null
                        showCreateSaleDialog = false
                    } catch (e: Exception) {
                        errorMessage = "فشل إنشاء عملية البيع: ${e.message}"
                        successMessage = null
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
 * Requirement 8.1: Customer, line items (goods + quantities + prices), total, payment wallet.
 * Requirement 8.4: Sale total = sum(qty × unit_price).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaleCreationTab(
    customers: List<CustomerItem>,
    finishedGoods: List<FinishedGoodItem>,
    wallets: List<WalletItem>,
    onCreateSale: (customerId: String, lineItems: List<LineItemEntry>, amountPaid: Double, walletId: String) -> Unit
) {
    // Form state
    var selectedCustomer by remember { mutableStateOf<CustomerItem?>(null) }
    var customerExpanded by remember { mutableStateOf(false) }
    val lineItems = remember { mutableStateListOf<LineItemEntry>() }
    var amountPaidText by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<WalletItem?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    // Line item builder state
    var selectedGood by remember { mutableStateOf<FinishedGoodItem?>(null) }
    var goodExpanded by remember { mutableStateOf(false) }
    var lineQtyText by remember { mutableStateOf("") }
    var linePriceText by remember { mutableStateOf("") }

    // Auto-calculated total (Req 8.4)
    val saleTotal = lineItems.sumOf { it.subtotal }
    val amountPaid = amountPaidText.toDoubleOrNull() ?: 0.0
    val remainingBalance = saleTotal - amountPaid

    val canSubmit = selectedCustomer != null &&
            lineItems.isNotEmpty() &&
            selectedWallet != null &&
            amountPaid >= 0 &&
            amountPaid <= saleTotal

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
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = goodExpanded,
                        onDismissRequest = { goodExpanded = false }
                    ) {
                        finishedGoods.forEach { good ->
                            DropdownMenuItem(
                                text = { Text("${good.name} (متوفر: ${"%.0f".format(good.currentQuantity)}) — ${"%.2f".format(good.unitPrice)}") },
                                onClick = {
                                    selectedGood = good
                                    linePriceText = "%.2f".format(good.unitPrice)
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
                            lineItems.add(
                                LineItemEntry(
                                    finishedGood = selectedGood!!,
                                    quantity = lineQty,
                                    unitPrice = linePrice
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

        // ── Sale Total (auto-calculated, Req 8.4) ──
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
                        text = "%.2f".format(saleTotal),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (amountPaid > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "المبلغ المدفوع:", style = MaterialTheme.typography.bodyMedium) // "Amount Paid:"
                        Text(text = "%.2f".format(amountPaid), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الرصيد المتبقي:", // "Remaining Balance:"
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remainingBalance > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "%.2f".format(remainingBalance),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remainingBalance > 0) MaterialTheme.colorScheme.error
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
            isError = amountPaid > saleTotal,
            supportingText = {
                if (amountPaid > saleTotal) {
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
                value = selectedWallet?.let { "${it.name} (${"%.2f".format(it.currentBalance)})" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("محفظة الدفع") }, // "Payment Wallet"
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = walletExpanded,
                onDismissRequest = { walletExpanded = false }
            ) {
                wallets.forEach { wallet ->
                    DropdownMenuItem(
                        text = { Text("${wallet.name} (${"%.2f".format(wallet.currentBalance)})") },
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
                        amountPaid,
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
                    text = "${item.quantity} × ${"%.2f".format(item.unitPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "%.2f".format(item.subtotal),
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
    sales: List<SaleItem>,
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
    sale: SaleItem,
    onGenerateReceipt: () -> Unit
) {
    val remaining = sale.totalAmount - sale.amountPaid
    val isFullyPaid = remaining <= 0

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
                        text = "${sale.lineItemCount} بنود", // "X items"
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Receipt generation button (Req 8.5)
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
                    text = "%.2f".format(sale.totalAmount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "المدفوع:", style = MaterialTheme.typography.bodyMedium) // "Paid:"
                Text(text = "%.2f".format(sale.amountPaid), style = MaterialTheme.typography.bodyMedium)
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
                        text = "%.2f".format(remaining),
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

/**
 * Dialog for creating a new sale — customer picker, line item builder,
 * payment amount, wallet selector.
 *
 * Requirement 8.1: Record sale with customer, line items, total, payment wallet, timestamp.
 * Requirement 8.4: Sale total = sum(qty × unit_price).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSaleDialog(
    customers: List<CustomerItem>,
    finishedGoods: List<FinishedGoodItem>,
    wallets: List<WalletItem>,
    onDismiss: () -> Unit,
    onConfirm: (customerId: String, lineItems: List<LineItemEntry>, amountPaid: Double, walletId: String) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<CustomerItem?>(null) }
    var customerExpanded by remember { mutableStateOf(false) }
    val lineItems = remember { mutableStateListOf<LineItemEntry>() }
    var amountPaidText by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<WalletItem?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    // Line item builder state
    var selectedGood by remember { mutableStateOf<FinishedGoodItem?>(null) }
    var goodExpanded by remember { mutableStateOf(false) }
    var lineQtyText by remember { mutableStateOf("") }
    var linePriceText by remember { mutableStateOf("") }

    val saleTotal = lineItems.sumOf { it.subtotal }
    val amountPaid = amountPaidText.toDoubleOrNull() ?: 0.0

    val canSubmit = selectedCustomer != null &&
            lineItems.isNotEmpty() &&
            selectedWallet != null &&
            amountPaid >= 0 &&
            amountPaid <= saleTotal

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
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

                // Add line item
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
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = goodExpanded,
                        onDismissRequest = { goodExpanded = false }
                    ) {
                        finishedGoods.forEach { good ->
                            DropdownMenuItem(
                                text = { Text("${good.name} (${"%.2f".format(good.unitPrice)})") },
                                onClick = {
                                    selectedGood = good
                                    linePriceText = "%.2f".format(good.unitPrice)
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
                            lineItems.add(LineItemEntry(selectedGood!!, lineQty, linePrice))
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
                            text = "${item.finishedGood.name}: ${item.quantity}×${"%.2f".format(item.unitPrice)} = ${"%.2f".format(item.subtotal)}",
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
                            text = "%.2f".format(saleTotal),
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
                        value = selectedWallet?.let { "${it.name} (${"%.2f".format(it.currentBalance)})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("محفظة الدفع") }, // "Payment Wallet"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = walletExpanded,
                        onDismissRequest = { walletExpanded = false }
                    ) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text("${wallet.name} (${"%.2f".format(wallet.currentBalance)})") },
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
                        onConfirm(selectedCustomer!!.id, lineItems.toList(), amountPaid, selectedWallet!!.id)
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
 * Displays a printable receipt with all required fields (Req 8.5):
 * customer name, itemized list, total, amount paid, remaining balance, date.
 */
@Composable
private fun ReceiptDialog(
    receipt: ReceiptData,
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
                Text(text = "العميل: ${receipt.customerName}", style = MaterialTheme.typography.bodyMedium) // "Customer:"
                Text(text = "المدينة: ${receipt.customerCity}", style = MaterialTheme.typography.bodySmall) // "City:"
                Text(text = "الهاتف: ${receipt.customerMobile}", style = MaterialTheme.typography.bodySmall) // "Phone:"
                Text(text = "التاريخ: ${receipt.formattedDate}", style = MaterialTheme.typography.bodySmall) // "Date:"

                HorizontalDivider()

                // Itemized list
                Text(text = "البنود:", style = MaterialTheme.typography.labelLarge) // "Items:"
                receipt.lineItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.finishedGoodName} (${item.quantity}×${"%.2f".format(item.unitPrice)})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "%.2f".format(item.subtotal),
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
                        text = "%.2f".format(receipt.totalAmount),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "المدفوع:", style = MaterialTheme.typography.bodyMedium) // "Paid:"
                    Text(text = "%.2f".format(receipt.amountPaid), style = MaterialTheme.typography.bodyMedium)
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
                        text = "%.2f".format(receipt.remainingBalance),
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
