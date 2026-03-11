package org.sweetlab.ui.admin

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

// Placeholder data classes until UniFFI bindings are generated
private data class IngredientEntry(
    val rawMaterialId: String,
    val rawMaterialName: String,
    val requiredQuantity: Double
)

private data class RecipeItem(
    val id: String,
    val name: String,
    val finishedGoodId: String,
    val finishedGoodName: String,
    val ingredients: List<IngredientEntry>
)

private data class MaterialOption(val id: String, val name: String, val unit: String)

private const val MAX_INGREDIENTS = 10
private const val MIN_INGREDIENTS = 1

/**
 * Recipe Management screen — list, create, edit, and delete recipes.
 *
 * Requirement 4.1: Recipe with name, finished good, 1-10 ingredients.
 * Requirement 4.2: Validate raw materials exist before saving.
 * Requirement 4.3: Each recipe produces exactly one finished good.
 * Requirement 4.4: Reject recipe if material doesn't exist.
 * Requirement 4.5: Prevent deletion if recipe has production logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeManagementScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val recipes = remember { mutableStateListOf<RecipeItem>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingRecipe by remember { mutableStateOf<RecipeItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<RecipeItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Available materials for ingredient picker
    val availableMaterials = remember { mutableStateListOf<MaterialOption>() }

    LaunchedEffect(Unit) {
        // TODO: Replace with SweetLabCore calls
        // val result = SweetLabApp.core?.getRecipes() ?: emptyList()
        // recipes.addAll(result.map { ... })
        // val rms = SweetLabApp.core?.getRawMaterials() ?: emptyList()
        // availableMaterials.addAll(rms.map { MaterialOption(it.id, it.name, it.unit) })
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
            FloatingActionButton(onClick = { showCreateDialog = true }) {
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

            if (recipes.isEmpty()) {
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
                    try {
                        if (editingRecipe != null) {
                            // TODO: SweetLabApp.core?.updateRecipe(...)
                        } else {
                            // TODO: SweetLabApp.core?.createRecipe(name, finishedGoodId, ingredients)
                        }
                        showCreateDialog = false
                        editingRecipe = null
                        errorMessage = null
                    } catch (e: Exception) {
                        errorMessage = "فشل حفظ الوصفة: ${e.message}" // "Failed to save recipe"
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
                        scope.launch {
                            try {
                                // TODO: SweetLabApp.core?.deleteRecipe(showDeleteConfirm!!.id)
                                recipes.removeAll { it.id == showDeleteConfirm!!.id }
                                showDeleteConfirm = null
                                errorMessage = null
                            } catch (e: Exception) {
                                errorMessage = "فشل حذف الوصفة: ${e.message}"
                                showDeleteConfirm = null
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
    recipe: RecipeItem,
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
    recipe: RecipeItem?,
    availableMaterials: List<MaterialOption>,
    onDismiss: () -> Unit,
    onSave: (name: String, finishedGoodId: String, ingredients: List<IngredientEntry>) -> Unit
) {
    var name by remember { mutableStateOf(recipe?.name ?: "") }
    var finishedGoodId by remember { mutableStateOf(recipe?.finishedGoodId ?: "") }
    val ingredients = remember {
        mutableStateListOf<IngredientEntry>().apply {
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
                                        IngredientEntry(selectedMaterialId, selectedMaterialName, qty)
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
