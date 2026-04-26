package com.inventory.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.EntityListItem
import com.inventory.app.ui.components.LoadingIndicator
import com.inventory.app.viewmodel.ProductViewModel

@Composable
fun ProductListScreen(
    productViewModel: ProductViewModel,
    onNavigateToForm: (Long) -> Unit
) {
    val products by productViewModel.products.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val error by productViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        productViewModel.loadProducts()
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            productViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToForm(0L) }) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                isLoading && products.isEmpty() -> {
                    LoadingIndicator()
                }
                products.isEmpty() -> {
                    Text(
                        text = "No products yet",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(products, key = { it.id }) { product ->
                            EntityListItem(
                                title = product.name,
                                subtitle = "${product.reference} · Qty: ${product.quantity}",
                                onClick = { onNavigateToForm(product.id) },
                                trailingContent = {
                                    IconButton(onClick = {
                                        productViewModel.removeProduct(product.id)
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete product",
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
}
