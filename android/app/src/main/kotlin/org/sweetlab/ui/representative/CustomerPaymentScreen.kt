package org.sweetlab.ui.representative

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

private data class PaymentCustomerItem(
    val id: String,
    val name: String,
    val city: String,
    val mobile: String,
    val totalDebt: Double,
    val overdueDays: Int
)

private data class PaymentWalletItem(
    val id: String,
    val name: String,
    val walletType: String,
    val currentBalance: Double
)

private data class DebtRecordItem(
    val id: String,
    val saleId: String,
    val originalAmount: Double,
    val remainingAmount: Double,
    val saleDate: String,
    val overdueDays: Int,
    val isCritical: Boolean
)

/**
 * Customer Payment screen — Representative role.
 *
 * Provides a payment form with customer picker, debt summary display,
 * payment amount entry, wallet selector, and confirmation dialog.
 * Applies payment to oldest outstanding debt records first (FIFO).
 *
 * Requirement 9.5: Credit specified wallet and reduce customer's outstanding debt.
 * Requirement 10.3: Apply payment to oldest outstanding debt records first (FIFO).
 * Requirement 10.4: When a debt record is fully paid, mark as settled and remove from active debts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPaymentScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Data lists
    val customers = remember { mutableStateListOf<PaymentCustomerItem>() }
    val wallets = remember { mutableStateListOf<PaymentWalletItem>() }
    val activeDebts = remember { mutableStateListOf<DebtRecordItem>() }

    // Form state
    var selectedCustomer by remember { mutableStateOf<PaymentCustomerItem?>(null) }
    var customerExpanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<PaymentWalletItem?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    // Dialog state
    var showConfirmDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val customerDebt = selectedCustomer?.totalDebt ?: 0.0
    val exceedsDebt = amount > customerDebt && customerDebt > 0
    val activeDebtCount = activeDebts.size
    val oldestDebtDate = activeDebts.minByOrNull { it.saleDate }?.saleDate
    val maxOverdueDays = activeDebts.maxOfOrNull { it.overdueDays } ?: 0
    val hasCriticalDebt = activeDebts.any { it.isCritical }

    val canSubmit = selectedCustomer != null &&
            amount > 0 &&
            !exceedsDebt &&
            selectedWallet != null &&
            customerDebt > 0

    LaunchedEffect(Unit) {
        // TODO: Replace with SweetLabCore calls
        // val customerList = SweetLabApp.core?.getCustomers() ?: emptyList()
        // customers.addAll(customerList.map {
        //     PaymentCustomerItem(it.id, it.name, it.city, it.mobile, it.totalDebt, it.overdueDays)
        // })
        // val walletList = SweetLabApp.core?.getWallets() ?: emptyList()
        // wallets.addAll(walletList.map {
        //     PaymentWalletItem(it.id, it.name, it.walletType.name, it.currentBalance)
        // })
    }

    // Load active debts when customer is selected
    LaunchedEffect(selectedCustomer) {
        activeDebts.clear()
        if (selectedCustomer != null) {
            // TODO: Replace with SweetLabCore call
            // val debts = SweetLabApp.core?.getActiveDebtsForCustomer(selectedCustomer!!.id) ?: emptyList()
            // activeDebts.addAll(debts.map {
            //     DebtRecordItem(
            //         it.id, it.saleId, it.originalAmount, it.remainingAmount,
            //         it.saleDate.toString(), it.overdueDays, it.isCritical
            //     )
            // })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تحصيل المدفوعات") }, // "Customer Payment"
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Error / success messages
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // ── Customer Picker ──
            Text(text = "العميل", style = MaterialTheme.typography.titleMedium) // "Customer"
            ExposedDropdownMenuBox(
                expanded = customerExpanded,
                onExpandedChange = { customerExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.let {
                        "${it.name} — دين: ${"%.2f".format(it.totalDebt)}"
                    } ?: "",
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
                            text = {
                                Text("${customer.name} — دين: ${"%.2f".format(customer.totalDebt)}")
                            },
                            onClick = {
                                selectedCustomer = customer
                                customerExpanded = false
                                amountText = ""
                                errorMessage = null
                                successMessage = null
                            }
                        )
                    }
                }
            }

            // ── Debt Summary Card (shown when customer is selected) ──
            if (selectedCustomer != null) {
                DebtSummaryCard(
                    totalDebt = customerDebt,
                    activeDebtCount = activeDebtCount,
                    oldestDebtDate = oldestDebtDate,
                    maxOverdueDays = maxOverdueDays,
                    hasCriticalDebt = hasCriticalDebt
                )
            }

            HorizontalDivider()

            // ── Payment Amount ──
            Text(text = "مبلغ الدفع", style = MaterialTheme.typography.titleMedium) // "Payment Amount"
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("المبلغ") }, // "Amount"
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = exceedsDebt,
                supportingText = {
                    if (exceedsDebt) {
                        Text("المبلغ يتجاوز إجمالي الدين") // "Amount exceeds total debt"
                    } else if (selectedCustomer != null && customerDebt <= 0) {
                        Text("لا يوجد دين مستحق لهذا العميل") // "No outstanding debt for this customer"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Wallet Selector ──
            Text(text = "المحفظة", style = MaterialTheme.typography.titleMedium) // "Wallet"
            ExposedDropdownMenuBox(
                expanded = walletExpanded,
                onExpandedChange = { walletExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedWallet?.let {
                        "${it.name} (${"%.2f".format(it.currentBalance)})"
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("اختر المحفظة") }, // "Select Wallet"
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
                            text = { Text("${wallet.name} (${"%.2f".format(wallet.currentBalance)})") },
                            onClick = {
                                selectedWallet = wallet
                                walletExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Record Payment Button ──
            Button(
                onClick = { showConfirmDialog = true },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تسجيل الدفعة", style = MaterialTheme.typography.titleMedium) // "Record Payment"
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Confirmation Dialog ──
    if (showConfirmDialog && selectedCustomer != null && selectedWallet != null) {
        PaymentConfirmationDialog(
            customerName = selectedCustomer!!.name,
            amount = amount,
            walletName = selectedWallet!!.name,
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                scope.launch {
                    try {
                        // TODO: Replace with SweetLabCore call
                        // val payment = SweetLabApp.core?.recordDebtPayment(
                        //     selectedCustomer!!.id, amount, selectedWallet!!.id
                        // )

                        // Update local state after successful payment
                        val newDebt = customerDebt - amount
                        val idx = customers.indexOfFirst { it.id == selectedCustomer!!.id }
                        if (idx >= 0) {
                            customers[idx] = customers[idx].copy(totalDebt = newDebt)
                            selectedCustomer = customers[idx]
                        }

                        successMessage = "تم تسجيل الدفعة بنجاح — ${"%.2f".format(amount)}" // "Payment recorded successfully"
                        errorMessage = null
                        amountText = ""
                    } catch (e: Exception) {
                        errorMessage = "فشل تسجيل الدفعة: ${e.message}" // "Payment recording failed"
                        successMessage = null
                    }
                }
            }
        )
    }
}


// ── Debt Summary Card ────────────────────────────────────────────────

/**
 * Card displaying the selected customer's debt summary:
 * total outstanding debt, number of active debt records,
 * oldest debt date, overdue days, and critical flag.
 *
 * Requirement 10.1: Overdue days = (current_date - sale_date).
 * Requirement 10.5: Critical flag if overdue > 30 days.
 */
@Composable
private fun DebtSummaryCard(
    totalDebt: Double,
    activeDebtCount: Int,
    oldestDebtDate: String?,
    maxOverdueDays: Int,
    hasCriticalDebt: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasCriticalDebt)
                MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ملخص الديون", // "Debt Summary"
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Total outstanding debt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "إجمالي الدين المستحق:", // "Total Outstanding Debt:"
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "%.2f".format(totalDebt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (totalDebt > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }

            // Number of active debt records
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "عدد سجلات الدين النشطة:", // "Active Debt Records:"
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$activeDebtCount",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Oldest debt date
            if (oldestDebtDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "أقدم تاريخ دين:", // "Oldest Debt Date:"
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = oldestDebtDate,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Overdue days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "أيام التأخير:", // "Overdue Days:"
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (maxOverdueDays > 0) "$maxOverdueDays يوم" else "لا يوجد", // "X days" or "None"
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (maxOverdueDays > 30) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }

            // Critical flag
            if (hasCriticalDebt) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.height(18.dp)
                    )
                    Text(
                        text = " ⚠ دين حرج — متأخر أكثر من 30 يوم", // "Critical debt — overdue more than 30 days"
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

// ── Payment Confirmation Dialog ──────────────────────────────────────

/**
 * Confirmation dialog shown before processing a payment.
 * Displays customer name, payment amount, and target wallet.
 */
@Composable
private fun PaymentConfirmationDialog(
    customerName: String,
    amount: Double,
    walletName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد الدفعة") }, // "Confirm Payment"
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "هل تريد تسجيل الدفعة التالية؟", // "Do you want to record the following payment?"
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "العميل:", style = MaterialTheme.typography.bodyMedium) // "Customer:"
                    Text(text = customerName, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "المبلغ:", style = MaterialTheme.typography.bodyMedium) // "Amount:"
                    Text(
                        text = "%.2f".format(amount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "المحفظة:", style = MaterialTheme.typography.bodyMedium) // "Wallet:"
                    Text(text = walletName, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "سيتم تطبيق الدفعة على أقدم الديون أولاً (FIFO)", // "Payment will be applied to oldest debts first (FIFO)"
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("تأكيد") // "Confirm"
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء") // "Cancel"
            }
        }
    )
}
