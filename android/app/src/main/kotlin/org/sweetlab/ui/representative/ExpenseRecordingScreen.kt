package org.sweetlab.ui.representative

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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

private data class WalletItem(
    val id: String,
    val name: String,
    val walletType: String,
    val currentBalance: Double
)

private enum class ExpenseCategory(val label: String) {
    Purchase("شراء"),           // Purchase
    OperatingCost("تكاليف تشغيلية") // Operating Cost
}

private data class ExpenseItem(
    val id: String,
    val description: String,
    val amount: Double,
    val category: String,
    val walletName: String,
    val timestamp: String
)

/**
 * Expense Recording screen — Representative role.
 *
 * Provides expense recording form with description, amount, category
 * (Purchase / Operating Cost), wallet selector, and expense history list.
 *
 * Requirement 11.1: Record expense with description, amount, category, wallet source, timestamp.
 * Requirement 11.2: Debit the specified wallet by the expense amount.
 * Requirement 11.3: Reject if expense amount exceeds wallet balance (insufficient funds).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseRecordingScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Data lists
    val wallets = remember { mutableStateListOf<WalletItem>() }
    val expenseHistory = remember { mutableStateListOf<ExpenseItem>() }

    // Dialog state
    var showCreateExpenseDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // TODO: Replace with SweetLabCore calls
        // val walletList = SweetLabApp.core?.getWallets() ?: emptyList()
        // wallets.addAll(walletList.map { WalletItem(it.id, it.name, it.walletType.name, it.currentBalance) })
        // val expenses = SweetLabApp.core?.getExpenses(startDate, endDate) ?: emptyList()
        // expenseHistory.addAll(expenses.map { ExpenseItem(it.id, it.description, it.amount, it.category.name, it.walletName, it.timestamp.toString()) })
    }

    val tabs = listOf("تسجيل مصروف", "سجل المصروفات") // "Record Expense", "Expense History"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تسجيل المصروفات") }, // "Expense Recording"
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

            when (selectedTab) {
                0 -> ExpenseFormTab(
                    wallets = wallets,
                    onRecordExpense = { description, amount, category, walletId ->
                        scope.launch {
                            try {
                                // TODO: Replace with SweetLabCore call
                                // val expense = SweetLabApp.core?.recordExpense(
                                //     description, amount, category, walletId, currentUserId
                                // )
                                // expenseHistory.add(0, ExpenseItem(
                                //     expense.id, expense.description, expense.amount,
                                //     expense.category.name, expense.walletName, expense.timestamp.toString()
                                // ))

                                successMessage = "تم تسجيل المصروف بنجاح" // "Expense recorded successfully"
                                errorMessage = null
                            } catch (e: Exception) {
                                errorMessage = "فشل تسجيل المصروف: ${e.message}" // "Expense recording failed"
                                successMessage = null
                            }
                        }
                    }
                )
                1 -> ExpenseHistoryTab(expenses = expenseHistory)
            }
        }
    }

    // ── Create Expense Dialog ──
    if (showCreateExpenseDialog) {
        CreateExpenseDialog(
            wallets = wallets,
            onDismiss = { showCreateExpenseDialog = false },
            onConfirm = { description, amount, category, walletId ->
                scope.launch {
                    try {
                        // TODO: Replace with SweetLabCore call
                        // val expense = SweetLabApp.core?.recordExpense(
                        //     description, amount, category, walletId, currentUserId
                        // )
                        // expenseHistory.add(0, ExpenseItem(
                        //     expense.id, expense.description, expense.amount,
                        //     expense.category.name, expense.walletName, expense.timestamp.toString()
                        // ))

                        successMessage = "تم تسجيل المصروف بنجاح"
                        errorMessage = null
                        showCreateExpenseDialog = false
                    } catch (e: Exception) {
                        errorMessage = "فشل تسجيل المصروف: ${e.message}"
                        successMessage = null
                    }
                }
            }
        )
    }
}

// ── Expense Form Tab ─────────────────────────────────────────────────

/**
 * Inline expense recording form with description, amount, category dropdown,
 * and wallet selector.
 *
 * Requirement 11.1: Description, amount, category (purchase or operating cost), wallet source.
 * Requirement 11.3: Wallet balance shown to help user avoid insufficient funds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormTab(
    wallets: List<WalletItem>,
    onRecordExpense: (description: String, amount: Double, category: String, walletId: String) -> Unit
) {
    // Form state
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedWallet by remember { mutableStateOf<WalletItem?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val exceedsBalance = selectedWallet != null && amount > selectedWallet!!.currentBalance

    val canSubmit = description.isNotBlank() &&
            amount > 0 &&
            selectedCategory != null &&
            selectedWallet != null &&
            !exceedsBalance

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
                value = selectedCategory?.label ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("اختر التصنيف") }, // "Select Category"
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                ExpenseCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.label) },
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
                value = selectedWallet?.let { "${it.name} (${"%.2f".format(it.currentBalance)})" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("اختر المحفظة") }, // "Select Wallet"
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

        Spacer(modifier = Modifier.height(8.dp))

        // ── Record Expense Button ──
        TextButton(
            onClick = {
                if (canSubmit && selectedCategory != null && selectedWallet != null) {
                    onRecordExpense(
                        description,
                        amount,
                        selectedCategory!!.name,
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
 * wallet, and timestamp.
 */
@Composable
private fun ExpenseHistoryTab(
    expenses: List<ExpenseItem>
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
private fun ExpenseHistoryCard(expense: ExpenseItem) {
    val categoryLabel = when (expense.category) {
        "Purchase" -> "شراء"
        "OperatingCost" -> "تكاليف تشغيلية"
        else -> expense.category
    }

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
                    text = "%.2f".format(expense.amount),
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
                    text = categoryLabel,
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
 * category dropdown, and wallet selector.
 *
 * Requirement 11.1: Description, amount, category, wallet source.
 * Requirement 11.3: Validates amount does not exceed wallet balance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateExpenseDialog(
    wallets: List<WalletItem>,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amount: Double, category: String, walletId: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedWallet by remember { mutableStateOf<WalletItem?>(null) }
    var walletExpanded by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val exceedsBalance = selectedWallet != null && amount > selectedWallet!!.currentBalance

    val canSubmit = description.isNotBlank() &&
            amount > 0 &&
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
                        value = selectedCategory?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("التصنيف") }, // "Category"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ExpenseCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.label) },
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
                        value = selectedWallet?.let { "${it.name} (${"%.2f".format(it.currentBalance)})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المحفظة") }, // "Wallet"
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
                    if (canSubmit && selectedCategory != null && selectedWallet != null) {
                        onConfirm(description, amount, selectedCategory!!.name, selectedWallet!!.id)
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
