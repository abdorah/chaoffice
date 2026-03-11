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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Admin Dashboard — overview cards for wallet balances, active debts,
 * low-stock alerts, and navigation to all admin modules.
 *
 * Requirement 2.6: Full admin dashboard with access to all modules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    onNavigateToUsers: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    // Summary state — will be populated from SweetLabCore once bindings are ready
    var totalWalletBalance by remember { mutableDoubleStateOf(0.0) }
    var activeDebtsCount by remember { mutableIntStateOf(0) }
    var lowStockCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // TODO: Replace with actual SweetLabCore calls
        // val wallets = SweetLabApp.core?.getWallets() ?: emptyList()
        // totalWalletBalance = wallets.sumOf { it.currentBalance }
        // val debts = SweetLabApp.core?.getDebtAgingReport() ?: emptyList()
        // activeDebtsCount = debts.size
        // val report = SweetLabApp.core?.getInventoryReport(10.0)
        // lowStockCount = (report?.rawMaterials?.count { it.isLowStock } ?: 0) +
        //                 (report?.finishedGoods?.count { it.isLowStock } ?: 0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة تحكم المدير") }, // "Admin Dashboard"
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Overview cards ──
            Text(
                text = "نظرة عامة", // "Overview"
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "رصيد المحافظ", // "Wallet Balance"
                    value = "%.2f".format(totalWalletBalance),
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "ديون نشطة", // "Active Debts"
                    value = "$activeDebtsCount",
                    icon = Icons.Default.Warning,
                    isAlert = activeDebtsCount > 0,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "مخزون منخفض", // "Low Stock"
                    value = "$lowStockCount",
                    icon = Icons.Default.Inventory2,
                    isAlert = lowStockCount > 0,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Navigation modules ──
            Text(
                text = "الأقسام", // "Modules"
                style = MaterialTheme.typography.titleMedium
            )

            NavigationCard(
                title = "إدارة المستخدمين", // "User Management"
                description = "إنشاء وتعديل حسابات المستخدمين والأدوار", // "Create and edit user accounts and roles"
                icon = Icons.Default.People,
                onClick = onNavigateToUsers
            )
            NavigationCard(
                title = "المخزون", // "Inventory"
                description = "المواد الخام والمنتجات النهائية", // "Raw materials and finished goods"
                icon = Icons.Default.Inventory2,
                onClick = onNavigateToInventory
            )
            NavigationCard(
                title = "إدارة الوصفات", // "Recipe Management"
                description = "إنشاء وتعديل وصفات الإنتاج", // "Create and edit production recipes"
                icon = Icons.Default.MenuBook,
                onClick = onNavigateToRecipes
            )
            NavigationCard(
                title = "ملخص المحافظ", // "Wallet Summary"
                description = "أرصدة المحافظ وسجل المعاملات", // "Wallet balances and transaction history"
                icon = Icons.Default.AccountBalance,
                onClick = onNavigateToWallets
            )
            NavigationCard(
                title = "التقارير", // "Reports"
                description = "التقارير المالية والمخزون والديون", // "Financial, inventory, and debt reports"
                icon = Icons.Default.Assessment,
                onClick = onNavigateToReports
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = if (isAlert) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = if (isAlert) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NavigationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
