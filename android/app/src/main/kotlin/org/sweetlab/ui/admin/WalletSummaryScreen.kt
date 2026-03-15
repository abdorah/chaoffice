package org.sweetlab.ui.admin

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
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
import org.sweetlab.SweetLabApp
import org.sweetlab.core.Wallet
import org.sweetlab.core.WalletTransaction
import org.sweetlab.core.WalletType
import org.sweetlab.ui.util.toMoneyDisplay
import org.sweetlab.ui.util.toMoneyDisplayWithSign

private val WALLET_TYPE_LABELS = mapOf(
    WalletType.BANK to "بنك",
    WalletType.CASH to "نقدي",
    WalletType.REPRESENTATIVE to "مندوب"
)

/**
 * Wallet Summary screen — wallet list with balances, transaction history,
 * and fund transfer dialog.
 *
 * Requirement 24.5: Admin navigates to wallets screen, fetches wallet balances
 * and transaction history from the Core_Engine, and supports fund transfers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSummaryScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val wallets = remember { mutableStateListOf<Wallet>() }
    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    val transactions = remember { mutableStateListOf<WalletTransaction>() }
    var showTransferDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    /** Re-fetch the wallet list from the core engine. */
    fun refreshWallets() {
        scope.launch {
            val token = SweetLabApp.currentSession?.sessionId ?: return@launch
            val core = SweetLabApp.core ?: return@launch
            try {
                val result = core.getWallets(token, null)
                wallets.clear()
                wallets.addAll(result)
                // Update selectedWallet reference if it was set
                selectedWallet = selectedWallet?.let { sel ->
                    result.find { it.id == sel.id }
                }
                errorMessage = null
            } catch (_: Exception) { /* keep stale list visible */ }
        }
    }

    // Fetch wallets on screen entry
    LaunchedEffect(Unit) {
        val token = SweetLabApp.currentSession?.sessionId
        val core = SweetLabApp.core
        if (token == null || core == null) {
            errorMessage = "الجلسة غير متوفرة" // "Session not available"
            isLoading = false
            return@LaunchedEffect
        }
        try {
            isLoading = true
            errorMessage = null
            val result = core.getWallets(token, null)
            wallets.clear()
            wallets.addAll(result)
        } catch (e: Exception) {
            errorMessage = "فشل تحميل بيانات المحافظ" // "Failed to load wallet data"
        } finally {
            isLoading = false
        }
    }

    // Load transactions when a wallet is selected
    LaunchedEffect(selectedWallet?.id) {
        transactions.clear()
        val wallet = selectedWallet ?: return@LaunchedEffect
        val token = SweetLabApp.currentSession?.sessionId ?: return@LaunchedEffect
        val core = SweetLabApp.core ?: return@LaunchedEffect
        try {
            val txns = core.getTransactionHistory(token, wallet.id, null)
            transactions.addAll(txns)
        } catch (_: Exception) {
            // Transaction fetch failure is non-critical; list stays empty
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

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
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
    }

    // ── Fund Transfer Dialog ──
    if (showTransferDialog) {
        FundTransferDialog(
            wallets = wallets,
            onDismiss = { showTransferDialog = false },
            onTransfer = { sourceId, destId, amountCents ->
                scope.launch {
                    val token = SweetLabApp.currentSession?.sessionId
                    val core = SweetLabApp.core
                    if (token == null || core == null) {
                        errorMessage = "الجلسة غير متوفرة"
                        return@launch
                    }
                    try {
                        isSubmitting = true
                        core.transferFunds(token, sourceId, destId, amountCents)
                        showTransferDialog = false
                        errorMessage = null
                        refreshWallets()
                    } catch (e: Exception) {
                        errorMessage = "فشل التحويل: ${e.message}" // "Transfer failed"
                    } finally {
                        isSubmitting = false
                    }
                }
            }
        )
    }
}


@Composable
private fun WalletCard(
    wallet: Wallet,
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
                    text = WALLET_TYPE_LABELS[wallet.walletType] ?: wallet.walletType.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = wallet.currentBalance.toMoneyDisplay(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TransactionRow(txn: WalletTransaction) {
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
                text = txn.amount.toMoneyDisplayWithSign(),
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
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onTransfer: (sourceId: String, destId: String, amountCents: Long) -> Unit
) {
    var sourceWallet by remember { mutableStateOf<Wallet?>(null) }
    var destWallet by remember { mutableStateOf<Wallet?>(null) }
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
                        value = sourceWallet?.let { "${it.name} (${it.currentBalance.toMoneyDisplay()})" } ?: "",
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
                                text = { Text("${wallet.name} (${wallet.currentBalance.toMoneyDisplay()})") },
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
                        value = destWallet?.let { "${it.name} (${it.currentBalance.toMoneyDisplay()})" } ?: "",
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
                                text = { Text("${wallet.name} (${wallet.currentBalance.toMoneyDisplay()})") },
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
                        val amountCents = (transferAmount * 100).toLong()
                        onTransfer(sourceWallet!!.id, destWallet!!.id, amountCents)
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
