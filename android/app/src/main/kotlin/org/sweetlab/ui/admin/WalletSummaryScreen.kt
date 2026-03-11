package org.sweetlab.ui.admin

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
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

// Placeholder data classes until UniFFI bindings are generated
private data class WalletItem(
    val id: String,
    val name: String,
    val walletType: String, // "Bank", "Cash", "Representative"
    val currentBalance: Double
)

private data class TransactionItem(
    val id: String,
    val walletId: String,
    val amount: Double,
    val description: String,
    val relatedEntityId: String?,
    val timestamp: String
)

private val WALLET_TYPE_LABELS = mapOf(
    "Bank" to "بنك",
    "Cash" to "نقدي",
    "Representative" to "مندوب"
)

/**
 * Wallet Summary screen — wallet list with balances, transaction history,
 * and fund transfer dialog.
 *
 * Requirement 9.1: Three wallet types with current balance.
 * Requirement 9.2: Atomic fund transfers between wallets.
 * Requirement 9.6: Display wallets with name, balance, and transaction history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSummaryScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val wallets = remember { mutableStateListOf<WalletItem>() }
    var selectedWallet by remember { mutableStateOf<WalletItem?>(null) }
    val transactions = remember { mutableStateListOf<TransactionItem>() }
    var showTransferDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // TODO: Replace with SweetLabCore calls
        // val result = SweetLabApp.core?.getWallets() ?: emptyList()
        // wallets.addAll(result.map { WalletItem(it.id, it.name, it.walletType.name, it.currentBalance) })
    }

    // Load transactions when a wallet is selected
    LaunchedEffect(selectedWallet) {
        transactions.clear()
        if (selectedWallet != null) {
            // TODO: Replace with SweetLabCore call
            // val txns = SweetLabApp.core?.getTransactionHistory(selectedWallet!!.id) ?: emptyList()
            // transactions.addAll(txns.map { TransactionItem(it.id, it.walletId, it.amount, it.description, it.relatedEntityId, it.timestamp.toString()) })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ملخص المحافظ") }, // "Wallet Summary"
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
            if (wallets.size >= 2) {
                FloatingActionButton(onClick = { showTransferDialog = true }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "تحويل أموال") // "Transfer funds"
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ── Wallet cards ──
            Text(text = "المحافظ", style = MaterialTheme.typography.titleMedium) // "Wallets"
            Spacer(modifier = Modifier.height(8.dp))

            wallets.forEach { wallet ->
                WalletCard(
                    wallet = wallet,
                    isSelected = selectedWallet?.id == wallet.id,
                    onClick = { selectedWallet = wallet }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Transaction history for selected wallet ──
            if (selectedWallet != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "سجل المعاملات — ${selectedWallet!!.name}",
                    // "Transaction History — [wallet name]"
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (transactions.isEmpty()) {
                    Text(
                        text = "لا توجد معاملات بعد", // "No transactions yet"
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(transactions, key = { it.id }) { txn ->
                            TransactionRow(txn)
                        }
                    }
                }
            }
        }
    }

    // ── Fund Transfer Dialog ──
    if (showTransferDialog) {
        FundTransferDialog(
            wallets = wallets,
            onDismiss = { showTransferDialog = false },
            onTransfer = { sourceId, destId, amount ->
                scope.launch {
                    try {
                        // TODO: SweetLabApp.core?.transferFunds(sourceId, destId, amount)
                        showTransferDialog = false
                        errorMessage = null
                        // Refresh wallets
                    } catch (e: Exception) {
                        errorMessage = "فشل التحويل: ${e.message}" // "Transfer failed"
                    }
                }
            }
        )
    }
}

@Composable
private fun WalletCard(
    wallet: WalletItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = wallet.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = WALLET_TYPE_LABELS[wallet.walletType] ?: wallet.walletType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "%.2f".format(wallet.currentBalance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TransactionRow(txn: TransactionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = txn.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = txn.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "%+.2f".format(txn.amount),
                style = MaterialTheme.typography.titleSmall,
                color = if (txn.amount >= 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FundTransferDialog(
    wallets: List<WalletItem>,
    onDismiss: () -> Unit,
    onTransfer: (sourceId: String, destId: String, amount: Double) -> Unit
) {
    var sourceWallet by remember { mutableStateOf<WalletItem?>(null) }
    var destWallet by remember { mutableStateOf<WalletItem?>(null) }
    var amount by remember { mutableStateOf("") }
    var sourceExpanded by remember { mutableStateOf(false) }
    var destExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحويل أموال") }, // "Transfer Funds"
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Source wallet
                ExposedDropdownMenuBox(
                    expanded = sourceExpanded,
                    onExpandedChange = { sourceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = sourceWallet?.let { "${it.name} (%.2f)".format(it.currentBalance) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("من محفظة") }, // "From wallet"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false }
                    ) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text("${wallet.name} (%.2f)".format(wallet.currentBalance)) },
                                onClick = {
                                    sourceWallet = wallet
                                    sourceExpanded = false
                                }
                            )
                        }
                    }
                }

                // Destination wallet
                ExposedDropdownMenuBox(
                    expanded = destExpanded,
                    onExpandedChange = { destExpanded = it }
                ) {
                    OutlinedTextField(
                        value = destWallet?.let { "${it.name} (%.2f)".format(it.currentBalance) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("إلى محفظة") }, // "To wallet"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = destExpanded,
                        onDismissRequest = { destExpanded = false }
                    ) {
                        wallets.filter { it.id != sourceWallet?.id }.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text("${wallet.name} (%.2f)".format(wallet.currentBalance)) },
                                onClick = {
                                    destWallet = wallet
                                    destExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ") }, // "Amount"
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val transferAmount = amount.toDoubleOrNull()
                    if (sourceWallet != null && destWallet != null && transferAmount != null && transferAmount > 0) {
                        onTransfer(sourceWallet!!.id, destWallet!!.id, transferAmount)
                    }
                },
                enabled = sourceWallet != null && destWallet != null &&
                        sourceWallet?.id != destWallet?.id &&
                        (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("تحويل") // "Transfer"
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء") // "Cancel"
            }
        }
    )
}
