package com.inventory.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.AppDropdown
import com.inventory.app.ui.components.AppTextField
import com.inventory.app.ui.components.EntityListItem
import com.inventory.app.ui.components.LoadingIndicator
import com.inventory.app.viewmodel.CategoryViewModel
import com.inventory.ffi.FfiCreateCategoryDto
import com.inventory.ffi.FfiUpdateCategoryDto

@Composable
fun CategoryListScreen(categoryViewModel: CategoryViewModel) {
    val categories by categoryViewModel.categories.collectAsState()
    val isLoading by categoryViewModel.isLoading.collectAsState()
    val error by categoryViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var parentCategoryId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { categoryViewModel.loadCategories() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            categoryViewModel.clearError()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingId != null) "Edit Category" else "New Category") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AppTextField(value = name, onValueChange = { name = it }, label = "Name")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = description, onValueChange = { description = it }, label = "Description")
                    Spacer(modifier = Modifier.height(8.dp))
                    val parentOptions = listOf<Long?>(null) + categories.map { it.id }
                    AppDropdown(
                        items = parentOptions,
                        selectedItem = parentCategoryId,
                        onItemSelected = { parentCategoryId = it },
                        label = "Parent Category",
                        itemLabel = { id ->
                            if (id == null) "None"
                            else categories.find { it.id == id }?.name ?: "Unknown"
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingId != null) {
                        categoryViewModel.updateCategory(
                            FfiUpdateCategoryDto(
                                id = editingId!!,
                                name = name,
                                description = description
                            )
                        ) { showDialog = false }
                    } else {
                        categoryViewModel.createCategory(
                            FfiCreateCategoryDto(
                                name = name,
                                description = description,
                                parentCategoryId = parentCategoryId
                            )
                        ) { showDialog = false }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingId = null; name = ""; description = ""; parentCategoryId = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                isLoading && categories.isEmpty() -> LoadingIndicator()
                categories.isEmpty() -> Text(
                    text = "No categories yet",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(categories, key = { it.id }) { category ->
                        EntityListItem(
                            title = category.name,
                            subtitle = category.description.ifEmpty { null },
                            onClick = {
                                editingId = category.id
                                name = category.name
                                description = category.description
                                parentCategoryId = category.parentCategoryId
                                showDialog = true
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    categoryViewModel.removeCategory(category.id)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete category",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
