package org.sweetlab.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.sweetlab.core.AppException
import org.sweetlab.core.RawMaterial
import org.sweetlab.core.Recipe
import org.sweetlab.core.RecipeIngredient

private const val MAX_INGREDIENTS = 10
private const val MIN_INGREDIENTS = 1

/**
 * Recipe Management screen — list, create, edit, and delete recipes.
 *
 * Requirement 24.6: Admin navigates to recipes screen, fetches recipes from
 * the Core_Engine, and supports create, edit, and delete operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeManagementScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val recipes = remember { mutableStateListOf<Recipe>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Recipe?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Available materials for ingredient picker
    val availableMaterials = remember { mutableStateListOf<RawMaterial>() }

    /** Re-fetch recipes from the core engine. */
    fun refreshRecipes() {
        scope.launch {
            val token = SweetLabApp.currentSession?.sessionId ?: return@launch
            val core = SweetLabApp.core ?: return@launch
            try {
                val result = core.getRecipes(token, null)
                recipes.clear()
                recipes.addAll(result)
                errorMessage = null
            } catch (_: Exception) { /* keep stale list visible */ }
        }
    }

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
            val result = core.getRecipes(token, null)
            recipes.clear()
            recipes.addAll(result)
            val rms = core.getRawMaterials(token, null)
            availableMaterials.clear()
            availableMaterials.addAll(rms)
        } catch (e: Exception) {
            errorMessage = "فشل تحميل الوصفات" // "Failed to load recipes"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة الوصفات") }, // "Recipe Management"
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
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = if (isSubmitting) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة وصفة") // "Add recipe"
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
            } else if (recipes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "لا توجد وصفات بعد", // "No recipes yet"
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onEdit = { editingRecipe = recipe },
                            onDelete = { showDeleteConfirm = recipe }
                        )
                    }
                }
            }
        }
    }

    // ── Create / Edit Recipe Dialog ──
    if (showCreateDialog || editingRecipe != null) {
        RecipeFormDialog(
            recipe = editingRecipe,
            availableMaterials = availableMaterials,
            onDismiss = {
                showCreateDialog = false
                editingRecipe = null
            },
            onSave = { name, finishedGoodId, ingredients ->
                scope.launch {
                    val token = SweetLabApp.currentSession?.sessionId
                    val core = SweetLabApp.core
                    if (token == null || core == null) {
                        errorMessage = "الجلسة غير متوفرة"
                        return@launch
                    }
                    try {
                        isSubmitting = true
                        if (editingRecipe != null) {
                            val updated = editingRecipe!!.copy(
                                name = name,
                                finishedGoodId = finishedGoodId,
                                ingredients = ingredients
                            )
                            core.updateRecipe(token, updated)
                        } else {
                            core.createRecipe(token, name, finishedGoodId, ingredients)
                        }
                        showCreateDialog = false
                        editingRecipe = null
                        errorMessage = null
                        refreshRecipes()
                    } catch (e: Exception) {
                        errorMessage = "فشل حفظ الوصفة: ${e.message}" // "Failed to save recipe"
                    } finally {
                        isSubmitting = false
                    }
                }
            }
        )
    }

    // ── Delete Confirmation Dialog ──
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("حذف الوصفة") }, // "Delete Recipe"
            text = {
                Column {
                    Text("هل أنت متأكد من حذف الوصفة \"${showDeleteConfirm!!.name}\"؟")
                    // "Are you sure you want to delete recipe ...?"
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ لا يمكن حذف الوصفة إذا كانت مستخدمة في سجلات الإنتاج",
                        // "Recipe cannot be deleted if used in production logs"
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val recipeToDelete = showDeleteConfirm!!
                        showDeleteConfirm = null
                        scope.launch {
                            val token = SweetLabApp.currentSession?.sessionId
                            val core = SweetLabApp.core
                            if (token == null || core == null) {
                                errorMessage = "الجلسة غير متوفرة"
                                return@launch
                            }
                            try {
                                isSubmitting = true
                                core.deleteRecipe(token, recipeToDelete.id)
                                errorMessage = null
                                refreshRecipes()
                            } catch (e: AppException.DeletionBlocked) {
                                errorMessage = "لا يمكن حذف الوصفة لأنها مستخدمة في سجلات الإنتاج"
                                // "Cannot delete recipe because it is used in production logs"
                            } catch (e: Exception) {
                                errorMessage = "فشل حذف الوصفة: ${e.message}"
                                // "Failed to delete recipe"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error) // "Delete"
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("إلغاء") // "Cancel"
                }
            }
        )
    }
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = recipe.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "المنتج: ${recipe.finishedGoodName}", // "Product:"
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "المكونات (${recipe.ingredients.size}):", // "Ingredients (n):"
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeFormDialog(
    recipe: Recipe?,
    availableMaterials: List<RawMaterial>,
    onDismiss: () -> Unit,
    onSave: (name: String, finishedGoodId: String, ingredients: List<RecipeIngredient>) -> Unit
) {
    var name by remember { mutableStateOf(recipe?.name ?: "") }
    var finishedGoodId by remember { mutableStateOf(recipe?.finishedGoodId ?: "") }
    val ingredients = remember {
        mutableStateListOf<RecipeIngredient>().apply {
            if (recipe != null) addAll(recipe.ingredients)
        }
    }

    // Ingredient being added
    var selectedMaterialId by remember { mutableStateOf("") }
    var selectedMaterialName by remember { mutableStateOf("") }
    var ingredientQuantity by remember { mutableStateOf("") }
    var materialExpanded by remember { mutableStateOf(false) }

    val isEdit = recipe != null
    val title = if (isEdit) "تعديل الوصفة" else "إنشاء وصفة جديدة"
    // "Edit Recipe" / "Create New Recipe"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الوصفة") }, // "Recipe Name"
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = finishedGoodId,
                    onValueChange = { finishedGoodId = it },
                    label = { Text("معرّف المنتج النهائي") }, // "Finished Good ID"
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "المكونات (${ingredients.size}/$MAX_INGREDIENTS):",
                    // "Ingredients (n/10):"
                    style = MaterialTheme.typography.labelMedium
                )

                // Existing ingredients
                ingredients.forEachIndexed { index, ingredient ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${ingredient.rawMaterialName}: ${ingredient.requiredQuantity}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { ingredients.removeAt(index) }) {
                            Icon(
                                Icons.Default.RemoveCircleOutline,
                                contentDescription = "إزالة", // "Remove"
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Add ingredient row
                if (ingredients.size < MAX_INGREDIENTS) {
                    ExposedDropdownMenuBox(
                        expanded = materialExpanded,
                        onExpandedChange = { materialExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedMaterialName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المادة الخام") }, // "Raw Material"
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = materialExpanded,
                            onDismissRequest = { materialExpanded = false }
                        ) {
                            availableMaterials.forEach { material ->
                                DropdownMenuItem(
                                    text = { Text("${material.name} (${material.unit})") },
                                    onClick = {
                                        selectedMaterialId = material.id
                                        selectedMaterialName = material.name
                                        materialExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ingredientQuantity,
                            onValueChange = { ingredientQuantity = it },
                            label = { Text("الكمية") }, // "Quantity"
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                val qty = ingredientQuantity.toDoubleOrNull()
                                if (selectedMaterialId.isNotBlank() && qty != null && qty > 0) {
                                    ingredients.add(
                                        RecipeIngredient(
                                            rawMaterialId = selectedMaterialId,
                                            rawMaterialName = selectedMaterialName,
                                            requiredQuantity = qty
                                        )
                                    )
                                    selectedMaterialId = ""
                                    selectedMaterialName = ""
                                    ingredientQuantity = ""
                                }
                            },
                            enabled = selectedMaterialId.isNotBlank() && ingredientQuantity.isNotBlank()
                        ) {
                            Text("إضافة") // "Add"
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, finishedGoodId, ingredients.toList()) },
                enabled = name.isNotBlank() && finishedGoodId.isNotBlank() &&
                        ingredients.size in MIN_INGREDIENTS..MAX_INGREDIENTS
            ) {
                Text(if (isEdit) "تحديث" else "إنشاء") // "Update" / "Create"
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء") // "Cancel"
            }
        }
    )
}
