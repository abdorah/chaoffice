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
import org.sweetlab.core.AppException
import org.sweetlab.core.Expense
import org.sweetlab.core.ExpenseCategory
import org.sweetlab.core.Wallet
import org.sweetlab.ui.util.toMoneyCents
import org.sweetlab.ui.util.toMoneyDisplay

/** Map ExpenseCategory enum to Arabic display label. */
private fun expenseCategoryLabel(category: ExpenseCategory): String = when (category) {
    ExpenseCategory.PURCHASE -> "شراء"
    ExpenseCategory.OPERATING_COST -> "تكاليف تشغيلية"
}

/**
 * Expense Recording screen — Representative role.
 *
 * Provides expense recording form with description, amount, category
 * (Purchase / Operating Cost), wallet selector, and expense history list.
 *
 * Requirement 24.11: Representative navigates to expenses screen, records expenses via Core_Engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseRecordingScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Data lists — using UniFFI types directly
    val wallets = remember { mutableStateListOf<Wallet>() }
    val expenseHistory = remember { mutableStateListOf<Expense>() }

    // UI state
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showCreateExpenseDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Use a wide date range to fetch all expenses
    val startDate = "2000-01-01T00:00:00Z"
    val endDate = "2099-12-31T23:59:59Z"

    suspend fun loadData() {
        val token = SweetLabApp.currentSession?.sessionId ?: return
        val core = SweetLabApp.core ?: return
        try {
            isLoading = true
            errorMessage = null

            val walletList = core.getWallets(token, null)
            wallets.clear()
            wallets.addAll(walletList)

            val expenses = core.getExpenses(token, startDate, endDate, null)
            expenseHistory.clear()
            expenseHistory.addAll(expenses)
        } catch (e: Exception) {
            errorMessage = "فشل تحميل بيانات المصروفات" // "Failed to load expense data"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val tabs = listOf("تسجيل مصروف", "سجل المصروفات") // "Record Expense", "Expense History"

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showCreateExpenseDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "تسجيل مصروف جديد")
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
                    0 -> ExpenseFormTab(
                        wallets = wallets,
                        isSubmitting = isSubmitting,
                        onRecordExpense = { description, amountCents, category, walletId ->
                            scope.launch {
                                val token = SweetLabApp.currentSession?.sessionId
                                val core = SweetLabApp.core
                                val userId = SweetLabApp.currentUserId
                                if (token == null || core == null || userId == null) {
                                    errorMessage = "الجلسة غير متوفرة" // "Session not available"
                                    return@launch
                                }
                                try {
                                    isSubmitting = true
                                    core.recordExpense(
                                        token, description, amountCents, category, walletId, userId
                                    )
                                    successMessage = "تم تسجيل المصروف بنجاح" // "Expense recorded successfully"
                                    errorMessage = null
                                    // Refresh expense history and wallet balances
                                    loadData()
                                } catch (e: AppException.InsufficientFunds) {
                                    errorMessage = "رصيد المحفظة غير كافٍ" // "Insufficient wallet balance"
                                    successMessage = null
                                } catch (e: Exception) {
                                    errorMessage = "فشل تسجيل المصروف: ${e.message}" // "Expense recording failed"
                                    successMessage = null
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    )
                    1 -> ExpenseHistoryTab(expenses = expenseHistory)
                }
            }
        }
    }

    // ── Create Expense Dialog ──
    if (showCreateExpenseDialog) {
        CreateExpenseDialog(
            wallets = wallets,
            onDismiss = { showCreateExpenseDialog = false },
            onConfirm = { description, amountCents, category, walletId ->
                scope.launch {
                    val token = SweetLabApp.currentSession?.sessionId
                    val core = SweetLabApp.core
                    val userId = SweetLabApp.currentUserId
                    if (token == null || core == null || userId == null) {
                        errorMessage = "الجلسة غير متوفرة"
                        showCreateExpenseDialog = false
                        return@launch
                    }
                    try {
                        isSubmitting = true
                        core.recordExpense(
                            token, description, amountCents, category, walletId, userId
                        )
                        successMessage = "تم تسجيل المصروف بنجاح"
                        errorMessage = null
                        showCreateExpenseDialog = false
                        // Refresh expense history and wallet balances
                        loadData()
                    } catch (e: AppException.InsufficientFunds) {
                        errorMessage = "رصيد المحفظة غير كافٍ"
                        successMessage = null
                    } catch (e: Exception) {
                        errorMessage = "فشل تسجيل المصروف: ${e.message}"
                        successMessage = null
                    } finally {
                        isSubmitting = false
                    }
                }
            }
        )
    }
}


// ── Expense Form Tab ─────────────────────────────────────────────────

/**
 * Inline expense recording form with description, amount, category dropdown,
 * and wallet selector. Uses UniFFI Wallet type directly.
 *
 * Wallet balance is displayed in decimal (cents / 100) to help user avoid insufficient funds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormTab(
    wallets: List<Wallet>,
    isSubmitting: Boolean,
    onRecordExpense: (description: String, amountCents: Long, category: ExpenseCategory, walletId: String) -> Unit
) {
    // Form state
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    // Convert user-entered decimal to cents for comparison
    val amountCents = (amountText.toDoubleOrNull() ?: 0.0).toMoneyCents()
    val exceedsBalance = selectedWallet != null && amountCents > selectedWallet!!.currentBalance

    val canSubmit = description.isNotBlank() &&
            amountCents > 0 &&
            selectedCategory != null &&
            selectedWallet != null &&
            !exceedsBalance &&
            !isSubmitting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Description ──
        Text(text = "الوصف", style = MaterialTheme.typography.titleMedium) // "Description"
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("وصف المصروف") }, // "Expense description"
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Amount ──
        Text(text = "المبلغ", style = MaterialTheme.typography.titleMedium) // "Amount"
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("مبلغ المصروف") }, // "Expense amount"
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = exceedsBalance,
            supportingText = {
                if (exceedsBalance) {
                    Text("المبلغ يتجاوز رصيد المحفظة") // "Amount exceeds wallet balance"
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // ── Category Dropdown ──
        Text(text = "التصنيف", style = MaterialTheme.typography.titleMedium) // "Category"
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedCategory?.let { expenseCategoryLabel(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("اختر التصنيف") }, // "Select Category"
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                ExpenseCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(expenseCategoryLabel(category)) },
                        onClick = {
                            selectedCategory = category
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

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

        // ── Record Expense Button ──
        TextButton(
            onClick = {
                if (canSubmit && selectedCategory != null && selectedWallet != null) {
                    onRecordExpense(
                        description,
                        amountCents,
                        selectedCategory!!,
                        selectedWallet!!.id
                    )
                    // Reset form
                    description = ""
                    amountText = ""
                    selectedCategory = null
                    selectedWallet = null
                }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تسجيل المصروف", style = MaterialTheme.typography.titleMedium) // "Record Expense"
        }

        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
    }
}

// ── Expense History Tab ──────────────────────────────────────────────

/**
 * Displays the list of past expenses with description, amount, category,
 * wallet, and timestamp. Uses UniFFI Expense type directly.
 */
@Composable
private fun ExpenseHistoryTab(
    expenses: List<Expense>
) {
    if (expenses.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "لا توجد مصروفات بعد", // "No expenses yet"
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(expenses, key = { it.id }) { expense ->
                ExpenseHistoryCard(expense = expense)
            }
        }
    }
}

@Composable
private fun ExpenseHistoryCard(expense: Expense) {
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
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = expense.amount.toMoneyDisplay(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = expenseCategoryLabel(expense.category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = expense.walletName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = expense.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// ── Create Expense Dialog ────────────────────────────────────────────

/**
 * Dialog for recording a new expense with description, amount,
 * category dropdown, and wallet selector. Uses UniFFI types directly.
 *
 * Validates amount does not exceed wallet balance (client-side check).
 * Server-side InsufficientFunds error is handled by the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateExpenseDialog(
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amountCents: Long, category: ExpenseCategory, walletId: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    val amountCents = (amountText.toDoubleOrNull() ?: 0.0).toMoneyCents()
    val exceedsBalance = selectedWallet != null && amountCents > selectedWallet!!.currentBalance

    val canSubmit = description.isNotBlank() &&
            amountCents > 0 &&
            selectedCategory != null &&
            selectedWallet != null &&
            !exceedsBalance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل مصروف جديد") }, // "Record New Expense"
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف المصروف") }, // "Expense description"
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ") }, // "Amount"
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = exceedsBalance,
                    supportingText = {
                        if (exceedsBalance) {
                            Text("المبلغ يتجاوز رصيد المحفظة") // "Amount exceeds wallet balance"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { expenseCategoryLabel(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("التصنيف") }, // "Category"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ExpenseCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(expenseCategoryLabel(category)) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Wallet selector
                ExposedDropdownMenuBox(
                    expanded = walletExpanded,
                    onExpandedChange = { walletExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWallet?.let { "${it.name} (${it.currentBalance.toMoneyDisplay()})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المحفظة") }, // "Wallet"
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
                    if (canSubmit && selectedCategory != null && selectedWallet != null) {
                        onConfirm(description, amountCents, selectedCategory!!, selectedWallet!!.id)
                    }
                },
                enabled = canSubmit
            ) {
                Text("تسجيل") // "Record"
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء") // "Cancel"
            }
        }
    )
}
