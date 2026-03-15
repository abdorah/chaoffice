package org.sweetlab.ui.chef

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import org.sweetlab.SweetLabApp
import org.sweetlab.core.AppException
import org.sweetlab.core.ProductionLog
import org.sweetlab.core.RawMaterial
import org.sweetlab.core.RecipeAvailability

/**
 * Production screen — Chef role.
 *
 * Displays available recipes with availability status, allows executing
 * production runs, and shows production history.
 *
 * Requirement 24.8: Chef navigates to production screen, fetches recipe availability,
 * and supports executing production runs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val recipeAvailabilities = remember { mutableStateListOf<RecipeAvailability>() }
    val productionHistory = remember { mutableStateListOf<ProductionLog>() }
    // Map of raw material UUID -> name for resolving materialsConsumed keys
    val rawMaterialNames = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showProductionDialog by remember { mutableStateOf<RecipeAvailability?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    suspend fun loadData() {
        val token = SweetLabApp.currentSession?.sessionId ?: return
        val core = SweetLabApp.core ?: return
        try {
            isLoading = true
            errorMessage = null

            val availabilities = core.getRecipeAvailability(token, null)
            recipeAvailabilities.clear()
            recipeAvailabilities.addAll(availabilities)

            val history = core.getProductionHistory(token, null)
            productionHistory.clear()
            productionHistory.addAll(history)

            // Build raw material name lookup from recipe ingredients
            val nameMap = mutableMapOf<String, String>()
            availabilities.forEach { avail ->
                avail.recipe.ingredients.forEach { ing ->
                    nameMap[ing.rawMaterialId] = ing.rawMaterialName
                }
            }
            // Also fetch raw materials to cover any IDs not in current recipes
            try {
                val rms = core.getRawMaterials(token, null)
                rms.forEach { rm -> nameMap[rm.id] = rm.name }
            } catch (_: Exception) { /* best-effort */ }
            rawMaterialNames.value = nameMap
        } catch (e: Exception) {
            errorMessage = "فشل تحميل بيانات الإنتاج" // "Failed to load production data"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
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

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> RecipeAvailabilityList(
                        recipes = recipeAvailabilities,
                        onExecute = { recipe -> showProductionDialog = recipe }
                    )
                    1 -> ProductionHistoryList(
                        history = productionHistory,
                        rawMaterialNames = rawMaterialNames.value
                    )
                }
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
                    val token = SweetLabApp.currentSession?.sessionId
                    val core = SweetLabApp.core
                    val chefId = SweetLabApp.currentUserId
                    if (token == null || core == null || chefId == null) {
                        errorMessage = "الجلسة غير متوفرة"
                        showProductionDialog = null
                        return@launch
                    }
                    try {
                        isSubmitting = true
                        core.executeProduction(token, recipeId, quantity, chefId)
                        successMessage = "تم تنفيذ الإنتاج بنجاح" // "Production executed successfully"
                        errorMessage = null
                        showProductionDialog = null
                        // Refresh availability and history after production
                        loadData()
                    } catch (e: AppException.InsufficientStock) {
                        errorMessage = "مواد غير كافية: ${e.message}" // "Insufficient materials"
                        successMessage = null
                        showProductionDialog = null
                    } catch (e: Exception) {
                        errorMessage = "فشل تنفيذ الإنتاج: ${e.message}" // "Production execution failed"
                        successMessage = null
                        showProductionDialog = null
                    } finally {
                        isSubmitting = false
                    }
                }
            }
        )
    }
}

/**
 * Displays the list of recipes with their availability status.
 * Each card shows max producible quantity and ingredient details.
 * Recipes with insufficient materials are visually flagged.
 */
@Composable
private fun RecipeAvailabilityList(
    recipes: List<RecipeAvailability>,
    onExecute: (RecipeAvailability) -> Unit
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
            items(recipes, key = { it.recipe.id }) { recipe ->
                RecipeAvailabilityCard(recipe = recipe, onExecute = { onExecute(recipe) })
            }
        }
    }
}

@Composable
private fun RecipeAvailabilityCard(
    recipe: RecipeAvailability,
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
                    Text(text = recipe.recipe.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "المنتج: ${recipe.recipe.finishedGoodName}", // "Product:"
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

            // Insufficient materials warning
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
            recipe.recipe.ingredients.forEach { ingredient ->
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
 * Production execution dialog — select quantity and confirm.
 * Shows calculated raw material requirements based on entered quantity.
 */
@Composable
private fun ProductionExecutionDialog(
    recipe: RecipeAvailability,
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
                    text = "الوصفة: ${recipe.recipe.name}", // "Recipe:"
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "المنتج: ${recipe.recipe.finishedGoodName}", // "Product:"
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

                // Calculated material requirements
                if (quantity > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "المواد الخام المطلوبة:", // "Required raw materials:"
                        style = MaterialTheme.typography.labelMedium
                    )
                    recipe.recipe.ingredients.forEach { ingredient ->
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
                onClick = { onExecute(recipe.recipe.id, quantity) },
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
 * Production history log.
 * Shows all past production runs with chef, recipe, quantity, timestamp, and materials consumed.
 */
@Composable
private fun ProductionHistoryList(
    history: List<ProductionLog>,
    rawMaterialNames: Map<String, String>
) {
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
                ProductionLogCard(log = log, rawMaterialNames = rawMaterialNames)
            }
        }
    }
}

@Composable
private fun ProductionLogCard(
    log: ProductionLog,
    rawMaterialNames: Map<String, String>
) {
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

            // Materials consumed — convert Uuid keys to display names
            if (log.materialsConsumed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "المواد المستهلكة:", // "Materials consumed:"
                    style = MaterialTheme.typography.labelMedium
                )
                log.materialsConsumed.forEach { (materialId, quantity) ->
                    val displayName = rawMaterialNames[materialId] ?: materialId
                    Text(
                        text = "• $displayName: ${"%.2f".format(quantity)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
