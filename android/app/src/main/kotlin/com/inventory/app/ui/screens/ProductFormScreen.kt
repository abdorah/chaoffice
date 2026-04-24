package com.inventory.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.app.ui.components.AppDropdown
import com.inventory.app.ui.components.AppTextField
import com.inventory.app.viewmodel.ProductViewModel
import com.inventory.ffi.FfiCreateProductDto
import com.inventory.ffi.FfiProductStatus
import com.inventory.ffi.FfiUpdateProductDto

@Composable
fun ProductFormScreen(
    productId: Long,
    productViewModel: ProductViewModel,
    onSaved: () -> Unit
) {
    val isEditMode = productId != 0L
    val selectedProduct by productViewModel.selectedProduct.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val error by productViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var priceUnit by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(FfiProductStatus.AVAILABLE) }
    var formLoaded by remember { mutableStateOf(false) }

    // Load existing product for edit mode
    LaunchedEffect(productId) {
        if (isEditMode) {
            productViewModel.loadProduct(productId)
        }
    }

    // Populate form fields when product is loaded
    LaunchedEffect(selectedProduct) {
        if (isEditMode && selectedProduct != null && !formLoaded) {
            val p = selectedProduct!!
            name = p.name
            reference = p.reference
            description = p.description
            quantity = p.quantity.toString()
            priceUnit = p.priceUnit.toString()
            status = p.status
            formLoaded = true
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            productViewModel.clearError()
        }
    }

    val statusOptions = listOf(
        FfiProductStatus.AVAILABLE,
        FfiProductStatus.OUT_OF_STOCK,
        FfiProductStatus.DISCONTINUED,
        FfiProductStatus.RESERVED
    )

    val onSave = {
        val qty = quantity.toLongOrNull() ?: 0L
        val price = priceUnit.toDoubleOrNull() ?: 0.0

        if (isEditMode) {
            productViewModel.updateProduct(
                FfiUpdateProductDto(
                    id = productId.toULong(),
                    name = name,
                    reference = reference,
                    description = description,
                    quantity = qty,
                    priceUnit = price,
                    status = status
                ),
                onSuccess = onSaved
            )
        } else {
            productViewModel.createProduct(
                FfiCreateProductDto(
                    name = name,
                    reference = reference,
                    description = description,
                    quantity = qty,
                    priceUnit = price,
                    status = status,
                    categoryId = null,
                    supplierId = null,
                    locationId = null
                ),
                onSuccess = onSaved
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = if (isEditMode) "Edit Product" else "New Product",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppTextField(value = name, onValueChange = { name = it }, label = "Name", enabled = !isLoading)
            Spacer(modifier = Modifier.height(8.dp))

            AppTextField(value = reference, onValueChange = { reference = it }, label = "Reference", enabled = !isLoading)
            Spacer(modifier = Modifier.height(8.dp))

            AppTextField(value = description, onValueChange = { description = it }, label = "Description", enabled = !isLoading, singleLine = false)
            Spacer(modifier = Modifier.height(8.dp))

            AppTextField(value = quantity, onValueChange = { quantity = it }, label = "Quantity", enabled = !isLoading)
            Spacer(modifier = Modifier.height(8.dp))

            AppTextField(value = priceUnit, onValueChange = { priceUnit = it }, label = "Price per Unit", enabled = !isLoading)
            Spacer(modifier = Modifier.height(8.dp))

            AppDropdown(
                items = statusOptions,
                selectedItem = status,
                onItemSelected = { status = it },
                label = "Status",
                itemLabel = { it.name.replace("_", " ") },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSave() },
                enabled = !isLoading && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(if (isEditMode) "Update" else "Create")
            }
        }
    }
}
