package org.sweetlab.ui.admin

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Placeholder data classes until UniFFI bindings are generated
private data class RawMaterialItem(
    val id: String,
    val name: String,
    val unit: String,
    val currentQuantity: Double,
    val lastUpdated: String
)

private data class FinishedGoodItem(
    val id: String,
    val name: String,
    val currentQuantity: Double,
    val unitPrice: Double,
    val lastUpdated: String
)

/**
 * Inventory screen — tabbed view for raw materials and finished goods.
 *
 * Requirement 3.5: Display raw materials with name, unit, quantity, last-updated.
 * Requirement 6.5: Display finished goods with name, quantity, unit price, last-updated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val rawMaterials = remember { mutableStateListOf<RawMaterialItem>() }
    val finishedGoods = remember { mutableStateListOf<FinishedGoodItem>() }

    LaunchedEffect(Unit) {
        // TODO: Replace with SweetLabCore calls
        // val rms = SweetLabApp.core?.getRawMaterials() ?: emptyList()
        // rawMaterials.addAll(rms.map { RawMaterialItem(it.id, it.name, it.unit, it.currentQuantity, it.lastUpdated.toString()) })
        // val fgs = SweetLabApp.core?.getFinishedGoods() ?: emptyList()
        // finishedGoods.addAll(fgs.map { FinishedGoodItem(it.id, it.name, it.currentQuantity, it.unitPrice, it.lastUpdated.toString()) })
    }

    val tabs = listOf("المواد الخام", "المنتجات النهائية") // "Raw Materials", "Finished Goods"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المخزون") }, // "Inventory"
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
        ) {
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
                0 -> RawMaterialsList(rawMaterials)
                1 -> FinishedGoodsList(finishedGoods)
            }
        }
    }
}

@Composable
private fun RawMaterialsList(materials: List<RawMaterialItem>) {
    if (materials.isEmpty()) {
        EmptyInventoryMessage("لا توجد مواد خام مسجلة") // "No raw materials recorded"
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(materials, key = { it.id }) { material ->
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
                            Text(text = material.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${material.currentQuantity} ${material.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "آخر تحديث: ${material.lastUpdated}", // "Last updated:"
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishedGoodsList(goods: List<FinishedGoodItem>) {
    if (goods.isEmpty()) {
        EmptyInventoryMessage("لا توجد منتجات نهائية مسجلة") // "No finished goods recorded"
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(goods, key = { it.id }) { good ->
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
                            Text(text = good.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${good.currentQuantity}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "سعر الوحدة: %.2f".format(good.unitPrice), // "Unit price:"
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "آخر تحديث: ${good.lastUpdated}", // "Last updated:"
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

@Composable
private fun EmptyInventoryMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
