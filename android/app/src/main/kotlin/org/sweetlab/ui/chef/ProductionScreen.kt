package org.sweetlab.ui.chef

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

// Placeholder data classes until UniFFI bindings are generated
private data class IngredientInfo(
    val rawMaterialName: String,
    val requiredQuantity: Double
)

private data class RecipeAvailabilityItem(
    val recipeId: String,
    val recipeName: String,
    val finishedGoodName: String,
    val maxProducible: Int,
    val ingredients: List<IngredientInfo>,
    val insufficientMaterials: List<String>
)

private data class ProductionLogItem(
    val id: String,
    val recipeName: String,
    val chefName: String,
    val productionQuantity: Int,
    val materialsConsumed: Map<String, Double>,
    val timestamp: String
)

/**
 * Production screen — Chef role.
 *
 * Displays available recipes with availability status, allows executing
 * production runs, and shows production history.
 *
 * Requirement 5.1: Chef selects recipe + quantity, system calculates raw material requirements.
 * Requirement 5.2: Atomic deduction of raw materials + increment of finished goods.
 * Requirement 5.3: Reject production if insufficient stock, list insufficient materials.
 * Requirement 5.4: Log production with chef identity, recipe, quantity, timestamp, materials consumed.
 * Requirement 5.5: Display available recipes with current availability status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val recipeAvailabilities = remember { mutableStateListOf<RecipeAvailabilityItem>() }
    val productionHistory = remember { mutableStateListOf<ProductionLogItem>() }
    var showProductionDialog by remember { mutableStateOf<RecipeAvailabilityItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // TODO: Replace with SweetLabCore calls
        // val availabilities = SweetLabApp.core?.getRecipeAvailability() ?: emptyList()
        // recipeAvailabilities.addAll(availabilities.map {
        //     RecipeAvailabilityItem(
        //         recipeId = it.recipe.id,
        //         recipeName = it.recipe.name,
        //         finishedGoodName = it.recipe.finishedGoodName,
        //         maxProducible = it.maxProducible,
        //         ingredients = it.recipe.ingredients.map { ing ->
        //             IngredientInfo(ing.rawMaterialName, ing.requiredQuantity)
        //         },
        //         insufficientMaterials = it.insufficientMaterials
        //     )
        // })
        // val history = SweetLabApp.core?.getProductionHistory() ?: emptyList()
        // productionHistory.addAll(history.map {
        //     ProductionLogItem(
        //         id = it.id,
        //         recipeName = it.recipeName,
        //         chefName = it.chefName,
        //         productionQuantity = it.productionQuantity,
        //         materialsConsumed = it.materialsConsumed,
        //         timestamp = it.timestamp.toString()
        //     )
        // })
    }

    val tabs = listOf("الوصفات المتاحة", "سجل الإنتاج") // "Available Recipes", "Production History"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("شاشة الإنتاج") }, // "Production Screen"
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
                0 -> RecipeAvailabilityList(
                    recipes = recipeAvailabilities,
                    onExecute = { recipe -> showProductionDialog = recipe }
                )
                1 -> ProductionHistoryList(history = productionHistory)
            }
        }
    }

    // ── Production Execution Dialog ──
    if (showProductionDialog != null) {
        ProductionExecutionDialog(
            recipe = showProductionDialog!!,
            onDismiss = { showProductionDialog = null },
            onExecute = { recipeId, quantity ->
                scope.launch {
                    try {
                        // TODO: Replace with SweetLabCore call
                        // val chefId = SweetLabApp.currentSession?.userId ?: return@launch
                        // val log = SweetLabApp.core?.executeProduction(recipeId, quantity, chefId)
                        // productionHistory.add(0, ProductionLogItem(
                        //     id = log.id,
                        //     recipeName = log.recipeName,
                        //     chefName = log.chefName,
                        //     productionQuantity = log.productionQuantity,
                        //     materialsConsumed = log.materialsConsumed,
                        //     timestamp = log.timestamp.toString()
                        // ))
                        // // Refresh availability after production
                        // recipeAvailabilities.clear()
                        // val updated = SweetLabApp.core?.getRecipeAvailability() ?: emptyList()
                        // recipeAvailabilities.addAll(updated.map { ... })

                        successMessage = "تم تنفيذ الإنتاج بنجاح" // "Production executed successfully"
                        errorMessage = null
                        showProductionDialog = null
                    } catch (e: Exception) {
                        errorMessage = "فشل تنفيذ الإنتاج: ${e.message}" // "Production execution failed"
                        successMessage = null
                        showProductionDialog = null
                    }
                }
            }
        )
    }
}

/**
 * Displays the list of recipes with their availability status (Req 5.5).
 * Each card shows max producible quantity and ingredient details.
 * Recipes with insufficient materials are visually flagged.
 */
@Composable
private fun RecipeAvailabilityList(
    recipes: List<RecipeAvailabilityItem>,
    onExecute: (RecipeAvailabilityItem) -> Unit
) {
    if (recipes.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "لا توجد وصفات متاحة", // "No recipes available"
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recipes, key = { it.recipeId }) { recipe ->
                RecipeAvailabilityCard(recipe = recipe, onExecute = { onExecute(recipe) })
            }
        }
    }
}

@Composable
private fun RecipeAvailabilityCard(
    recipe: RecipeAvailabilityItem,
    onExecute: () -> Unit
) {
    val isAvailable = recipe.maxProducible > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = recipe.recipeName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "المنتج: ${recipe.finishedGoodName}", // "Product:"
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Execute production button
                TextButton(
                    onClick = onExecute,
                    enabled = isAvailable
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "تنفيذ الإنتاج", // "Execute production"
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("إنتاج") // "Produce"
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Availability status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "أقصى كمية يمكن إنتاجها:", // "Max producible quantity:"
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "${recipe.maxProducible}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isAvailable)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            // Insufficient materials warning (Req 5.3)
            if (recipe.insufficientMaterials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ مواد غير كافية:", // "Insufficient materials:"
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                recipe.insufficientMaterials.forEach { material ->
                    Text(
                        text = "• $material",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Ingredients list
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "المكونات (لكل وحدة):", // "Ingredients (per unit):"
                style = MaterialTheme.typography.labelMedium
            )
            recipe.ingredients.forEach { ingredient ->
                Text(
                    text = "• ${ingredient.rawMaterialName}: ${ingredient.requiredQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Production execution dialog — select quantity and confirm (Req 5.1, 5.2).
 * Shows calculated raw material requirements based on entered quantity.
 */
@Composable
private fun ProductionExecutionDialog(
    recipe: RecipeAvailabilityItem,
    onDismiss: () -> Unit,
    onExecute: (recipeId: String, quantity: Int) -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    val quantity = quantityText.toIntOrNull() ?: 0
    val isValid = quantity in 1..recipe.maxProducible

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنفيذ الإنتاج") }, // "Execute Production"
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "الوصفة: ${recipe.recipeName}", // "Recipe:"
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "المنتج: ${recipe.finishedGoodName}", // "Product:"
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "أقصى كمية متاحة: ${recipe.maxProducible}", // "Max available quantity:"
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("كمية الإنتاج") }, // "Production Quantity"
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantityText.isNotEmpty() && !isValid,
                    supportingText = {
                        if (quantityText.isNotEmpty() && !isValid) {
                            Text("يجب أن تكون الكمية بين 1 و ${recipe.maxProducible}")
                            // "Quantity must be between 1 and max"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Calculated material requirements (Req 5.1)
                if (quantity > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "المواد الخام المطلوبة:", // "Required raw materials:"
                        style = MaterialTheme.typography.labelMedium
                    )
                    recipe.ingredients.forEach { ingredient ->
                        val totalRequired = ingredient.requiredQuantity * quantity
                        Text(
                            text = "• ${ingredient.rawMaterialName}: ${"%.2f".format(totalRequired)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onExecute(recipe.recipeId, quantity) },
                enabled = isValid
            ) {
                Text("تنفيذ") // "Execute"
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء") // "Cancel"
            }
        }
    )
}

/**
 * Production history log (Req 5.4).
 * Shows all past production runs with chef, recipe, quantity, timestamp, and materials consumed.
 */
@Composable
private fun ProductionHistoryList(history: List<ProductionLogItem>) {
    if (history.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "لا يوجد سجل إنتاج بعد", // "No production history yet"
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history, key = { it.id }) { log ->
                ProductionLogCard(log = log)
            }
        }
    }
}

@Composable
private fun ProductionLogCard(log: ProductionLogItem) {
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
                Text(text = log.recipeName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "×${log.productionQuantity}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "الطاهي: ${log.chefName}", // "Chef:"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "التاريخ: ${log.timestamp}", // "Date:"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Materials consumed
            if (log.materialsConsumed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "المواد المستهلكة:", // "Materials consumed:"
                    style = MaterialTheme.typography.labelMedium
                )
                log.materialsConsumed.forEach { (materialName, quantity) ->
                    Text(
                        text = "• $materialName: ${"%.2f".format(quantity)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
