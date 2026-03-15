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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.sweetlab.SweetLabApp
import org.sweetlab.core.AppException
import org.sweetlab.core.Customer
import org.sweetlab.core.DebtRecord
import org.sweetlab.core.Wallet
import org.sweetlab.ui.util.toMoneyCents
import org.sweetlab.ui.util.toMoneyDisplay

/**
 * Customer Payment screen — Representative role.
 *
 * Provides a payment form with customer picker, debt summary display,
 * payment amount entry, wallet selector, and confirmation dialog.
 * Applies payment to oldest outstanding debt records first (FIFO).
 *
 * Requirement 24.12: Representative navigates to payments screen,
 * records debt payments via the Core_Engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPaymentScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Data lists — using UniFFI types directly
    val customers = remember { mutableStateListOf<Customer>() }
    val wallets = remember { mutableStateListOf<Wallet>() }
    val activeDebts = remember { mutableStateListOf<DebtRecord>() }

    // Form state
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerExpanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    // Dialog state
    var showConfirmDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Money conversion: user enters decimal, we work in i64 cents
    val amountCents = (amountText.toDoubleOrNull() ?: 0.0).toMoneyCents()
    val customerDebtCents = selectedCustomer?.totalDebt ?: 0L
    val exceedsDebt = amountCents > customerDebtCents && customerDebtCents > 0L
    val activeDebtCount = activeDebts.size
    val oldestDebtDate = activeDebts.minByOrNull { it.saleDate }?.saleDate
    val maxOverdueDays = activeDebts.maxOfOrNull { it.overdueDays } ?: 0
    val hasCriticalDebt = activeDebts.any { it.isCritical }

    val canSubmit = selectedCustomer != null &&
            amountCents > 0 &&
            !exceedsDebt &&
            selectedWallet != null &&
            !isSubmitting

    // Load initial data: customers and wallets
    suspend fun loadData() {
        val token = SweetLabApp.currentSession?.sessionId ?: return
        val core = SweetLabApp.core ?: return
        try {
            isLoading = true
            errorMessage = null

            val customerList = core.getCustomers(token, null)
            customers.clear()
            customers.addAll(customerList)

            val walletList = core.getWallets(token, null)
            wallets.clear()
            wallets.addAll(walletList)
        } catch (e: Exception) {
            errorMessage = "فشل تحميل البيانات" // "Failed to load data"
        } finally {
            isLoading = false
        }
    }

    // Load active debts for a specific customer
    suspend fun loadDebtsForCustomer(customerId: String) {
        val token = SweetLabApp.currentSession?.sessionId ?: return
        val core = SweetLabApp.core ?: return
        try {
            val allDebts = core.getActiveDebts(token, null)
            activeDebts.clear()
            activeDebts.addAll(allDebts.filter { it.customerId == customerId })
        } catch (e: Exception) {
            errorMessage = "فشل تحميل الديون" // "Failed to load debts"
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // When customer selection changes, load their debts
    LaunchedEffect(selectedCustomer?.id) {
        val customer = selectedCustomer
        if (customer != null) {
            loadDebtsForCustomer(customer.id)
        } else {
            activeDebts.clear()
        }
    }

    Scaffold { innerPadding ->
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

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Customer Selector ──
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
                                    text = {
                                        Column {
                                            Text(customer.name)
                                            Text(
                                                text = "${customer.city} — دين: ${customer.totalDebt.toMoneyDisplay()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomer = customer
                                        customerExpanded = false
                                        // Reset amount when customer changes
                                        amountText = ""
                                        successMessage = null
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    // ── Debt Summary Card ──
                    if (selectedCustomer != null) {
                        HorizontalDivider()
                        Text(text = "ملخص الديون", style = MaterialTheme.typography.titleMedium) // "Debt Summary"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasCriticalDebt)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي الدين") // "Total Debt"
                                    Text(
                                        text = "${customerDebtCents.toMoneyDisplay()}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("عدد الديون النشطة") // "Active Debts Count"
                                    Text("$activeDebtCount")
                                }
                                if (maxOverdueDays > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("أقصى تأخير (أيام)") // "Max Overdue (days)"
                                        Text("$maxOverdueDays")
                                    }
                                }
                                if (oldestDebtDate != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("أقدم دين") // "Oldest Debt"
                                        Text(oldestDebtDate)
                                    }
                                }
                                if (hasCriticalDebt) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = " ديون حرجة!",  // "Critical debts!"
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // ── Active Debt Records List ──
                        if (activeDebts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "تفاصيل الديون", style = MaterialTheme.typography.titleSmall) // "Debt Details"
                            activeDebts.forEach { debt ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (debt.isCritical)
                                            MaterialTheme.colorScheme.errorContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "المتبقي: ${debt.remainingAmount.toMoneyDisplay()}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "تاريخ البيع: ${debt.saleDate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (debt.overdueDays > 0) {
                                            Text(
                                                text = "${debt.overdueDays} يوم",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // ── Amount Input ──
                    Text(text = "مبلغ الدفعة", style = MaterialTheme.typography.titleMedium) // "Payment Amount"
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
                            value = selectedWallet?.let { "${it.name} (${it.currentBalance.toMoneyDisplay()})" } ?: "",
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
                                    text = { Text("${wallet.name} (${wallet.currentBalance.toMoneyDisplay()})") },
                                    onClick = {
                                        selectedWallet = wallet
                                        walletExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Submit Payment Button ──
                    Button(
                        onClick = { showConfirmDialog = true },
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text("تسجيل الدفعة") // "Record Payment"
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // ── Confirmation Dialog ──
    if (showConfirmDialog && selectedCustomer != null && selectedWallet != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("تأكيد الدفعة") }, // "Confirm Payment"
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("العميل: ${selectedCustomer!!.name}")
                    Text("المبلغ: ${amountCents.toMoneyDisplay()}")
                    Text("المحفظة: ${selectedWallet!!.name}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        scope.launch {
                            val token = SweetLabApp.currentSession?.sessionId
                            val core = SweetLabApp.core
                            if (token == null || core == null) {
                                errorMessage = "الجلسة غير متوفرة" // "Session not available"
                                return@launch
                            }
                            try {
                                isSubmitting = true
                                errorMessage = null
                                successMessage = null

                                val payment = core.recordDebtPayment(
                                    token,
                                    selectedCustomer!!.id,
                                    amountCents,
                                    selectedWallet!!.id
                                )

                                // Build success message including unallocated info
                                val allocated = payment.amount
                                val unallocated = payment.unallocated
                                successMessage = if (unallocated > 0L) {
                                    "تم تسجيل الدفعة بنجاح — مبلغ مخصص: ${allocated.toMoneyDisplay()}، متبقي غير مخصص: ${unallocated.toMoneyDisplay()}"
                                } else {
                                    "تم تسجيل الدفعة بنجاح — ${allocated.toMoneyDisplay()}"
                                }

                                // Reset form
                                amountText = ""

                                // Refresh: reload customers (for updated totalDebt), wallets, and debts
                                loadData()
                                // Update selected customer from refreshed list
                                val custId = selectedCustomer!!.id
                                selectedCustomer = customers.find { it.id == custId }
                                // Debts will refresh via LaunchedEffect on selectedCustomer change;
                                // but since the id didn't change, manually reload
                                loadDebtsForCustomer(custId)
                            } catch (e: AppException.NotFound) {
                                errorMessage = "العميل أو المحفظة غير موجود" // "Customer or wallet not found"
                                successMessage = null
                            } catch (e: AppException.Validation) {
                                errorMessage = "خطأ في البيانات: ${e.message}" // "Validation error"
                                successMessage = null
                            } catch (e: Exception) {
                                errorMessage = "فشل تسجيل الدفعة: ${e.message}" // "Payment recording failed"
                                successMessage = null
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                ) {
                    Text("تأكيد") // "Confirm"
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("إلغاء") // "Cancel"
                }
            }
        )
    }
}
