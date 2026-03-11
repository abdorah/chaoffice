package org.sweetlab.ui.representative

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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

// Placeholder data class until UniFFI bindings are generated
private data class CustomerData(
    val id: String,
    val name: String,
    val city: String,
    val mobile: String,
    val reliabilityRating: Int, // 0-5, 0 = initial
    val totalDebt: Double,
    val overdueDays: Int
)

/**
 * Customer Management screen — Representative role.
 *
 * Provides customer list with search, customer profile with debt summary
 * and reliability rating, and create/edit customer form.
 *
 * Requirement 7.1: Create customer with name, city, mobile, initial rating = 0.
 * Requirement 7.2: Update reliability rating between 1-5 stars.
 * Requirement 7.3: Enforce unique mobile number.
 * Requirement 7.4: Search by name, city, or mobile.
 * Requirement 7.5: Display customer profile with name, city, mobile, rating, total debt, overdue days.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagementScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val customers = remember { mutableStateListOf<CustomerData>() }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf<CustomerData?>(null) }
    var showRatingDialog by remember { mutableStateOf<CustomerData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // TODO: Replace with SweetLabCore call
        // val result = SweetLabApp.core?.getCustomers() ?: emptyList()
        // customers.addAll(result.map {
        //     CustomerData(it.id, it.name, it.city, it.mobile, it.reliabilityRating, it.totalDebt, it.overdueDays)
        // })
    }

    // Filter customers based on search query (Req 7.4)
    val filteredCustomers = if (searchQuery.isBlank()) {
        customers.toList()
    } else {
        customers.filter { customer ->
            customer.name.contains(searchQuery, ignoreCase = true) ||
                    customer.city.contains(searchQuery, ignoreCase = true) ||
                    customer.mobile.contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة العملاء") }, // "Customer Management"
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
                Icon(Icons.Default.Add, contentDescription = "إضافة عميل") // "Add Customer"
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Error / success messages
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ── Search Bar (Req 7.4) ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newQuery ->
                    searchQuery = newQuery
                    // TODO: Replace with SweetLabCore call for server-side search
                    // scope.launch {
                    //     val results = SweetLabApp.core?.searchCustomers(newQuery) ?: emptyList()
                    //     customers.clear()
                    //     customers.addAll(results.map {
                    //         CustomerData(it.id, it.name, it.city, it.mobile, it.reliabilityRating, it.totalDebt, it.overdueDays)
                    //     })
                    // }
                },
                label = { Text("بحث بالاسم أو المدينة أو الجوال") }, // "Search by name, city, or mobile"
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "بحث")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Customer List ──
            if (filteredCustomers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "لا يوجد عملاء بعد" // "No customers yet"
                        else "لا توجد نتائج للبحث", // "No search results"
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            onViewProfile = { showProfileDialog = customer },
                            onUpdateRating = { showRatingDialog = customer }
                        )
                    }
                }
            }
        }
    }

    // ── Create Customer Dialog (Req 7.1, 7.3) ──
    if (showCreateDialog) {
        CreateCustomerDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, city, mobile ->
                scope.launch {
                    try {
                        // TODO: Replace with SweetLabCore call
                        // val newCustomer = SweetLabApp.core?.createCustomer(name, city, mobile)
                        // customers.add(CustomerData(
                        //     newCustomer.id, newCustomer.name, newCustomer.city,
                        //     newCustomer.mobile, newCustomer.reliabilityRating,
                        //     newCustomer.totalDebt, newCustomer.overdueDays
                        // ))
                        showCreateDialog = false
                        successMessage = "تم إنشاء العميل بنجاح" // "Customer created successfully"
                        errorMessage = null
                    } catch (e: Exception) {
                        errorMessage = "فشل إنشاء العميل: ${e.message}" // "Failed to create customer"
                        successMessage = null
                    }
                }
            }
        )
    }

    // ── Customer Profile Dialog (Req 7.5) ──
    if (showProfileDialog != null) {
        CustomerProfileDialog(
            customer = showProfileDialog!!,
            onDismiss = { showProfileDialog = null },
            onUpdateRating = { customer ->
                showProfileDialog = null
                showRatingDialog = customer
            }
        )
    }

    // ── Rating Update Dialog (Req 7.2) ──
    if (showRatingDialog != null) {
        RatingUpdateDialog(
            customer = showRatingDialog!!,
            onDismiss = { showRatingDialog = null },
            onUpdateRating = { customerId, newRating ->
                scope.launch {
                    try {
                        // TODO: Replace with SweetLabCore call
                        // SweetLabApp.core?.updateReliabilityRating(customerId, newRating)
                        val idx = customers.indexOfFirst { it.id == customerId }
                        if (idx >= 0) {
                            customers[idx] = customers[idx].copy(reliabilityRating = newRating)
                        }
                        showRatingDialog = null
                        successMessage = "تم تحديث التقييم بنجاح" // "Rating updated successfully"
                        errorMessage = null
                    } catch (e: Exception) {
                        errorMessage = "فشل تحديث التقييم: ${e.message}" // "Failed to update rating"
                        successMessage = null
                    }
                }
            }
        )
    }
}


// ── Customer Card ────────────────────────────────────────────────────

/**
 * Card displaying customer summary: name, city, mobile, rating stars, debt info.
 * Tapping the card opens the profile dialog (Req 7.5).
 */
@Composable
private fun CustomerCard(
    customer: CustomerData,
    onViewProfile: () -> Unit,
    onUpdateRating: () -> Unit
) {
    Card(
        onClick = onViewProfile,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = customer.name, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = customer.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = customer.mobile,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRatingDisplay(rating = customer.reliabilityRating)
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onUpdateRating) {
                        Text("تعديل", style = MaterialTheme.typography.labelSmall) // "Edit"
                    }
                }

                // Debt info
                if (customer.totalDebt > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "دين: ${"%.2f".format(customer.totalDebt)}", // "Debt:"
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (customer.overdueDays > 0) {
                            Text(
                                text = "متأخر: ${customer.overdueDays} يوم", // "Overdue: X days"
                                style = MaterialTheme.typography.labelSmall,
                                color = if (customer.overdueDays > 30)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Star Rating Display ──────────────────────────────────────────────

/**
 * Displays a row of 5 stars, filled up to [rating].
 */
@Composable
private fun StarRatingDisplay(rating: Int) {
    Row {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$i نجمة",
                tint = if (i <= rating) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(18.dp)
            )
        }
    }
}

// ── Star Rating Selector ─────────────────────────────────────────────

/**
 * Interactive star selector allowing the user to pick a rating from 1 to 5.
 */
@Composable
private fun StarRatingSelector(
    selectedRating: Int,
    onRatingSelected: (Int) -> Unit
) {
    Row {
        for (i in 1..5) {
            IconButton(onClick = { onRatingSelected(i) }) {
                Icon(
                    imageVector = if (i <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "$i نجمة",
                    tint = if (i <= selectedRating) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


// ── Create Customer Dialog ───────────────────────────────────────────

/**
 * Dialog for creating a new customer with name, city, and mobile fields.
 *
 * Requirement 7.1: Store customer name, city, mobile, initial rating = 0.
 * Requirement 7.3: Unique mobile number (enforced server-side).
 */
@Composable
private fun CreateCustomerDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, city: String, mobile: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء عميل جديد") }, // "Create New Customer"
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل") }, // "Customer Name"
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("المدينة") }, // "City"
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("رقم الجوال") }, // "Mobile Number"
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "سيتم تعيين تقييم الموثوقية الأولي إلى 0",
                    // "Initial reliability rating will be set to 0"
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, city, mobile) },
                enabled = name.isNotBlank() && city.isNotBlank() && mobile.isNotBlank()
            ) {
                Text("إنشاء") // "Create"
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء") // "Cancel"
            }
        }
    )
}

// ── Customer Profile Dialog ──────────────────────────────────────────

/**
 * Dialog displaying the full customer profile with debt summary and reliability rating.
 *
 * Requirement 7.5: Display name, city, mobile, reliability rating, total debt, overdue days.
 */
@Composable
private fun CustomerProfileDialog(
    customer: CustomerData,
    onDismiss: () -> Unit,
    onUpdateRating: (CustomerData) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ملف العميل") }, // "Customer Profile"
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "الاسم", // "Name"
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = customer.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // City
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationCity,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "المدينة", // "City"
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = customer.city, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // Mobile
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "رقم الجوال", // "Mobile"
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = customer.mobile, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // Reliability Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "تقييم الموثوقية", // "Reliability Rating"
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarRatingDisplay(rating = customer.reliabilityRating)
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { onUpdateRating(customer) }) {
                                Text("تعديل", style = MaterialTheme.typography.labelSmall) // "Edit"
                            }
                        }
                    }
                }

                // Debt Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (customer.totalDebt > 0)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ملخص الديون", // "Debt Summary"
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "إجمالي الدين:", // "Total Debt:"
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "%.2f".format(customer.totalDebt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (customer.totalDebt > 0)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "أيام التأخير:", // "Overdue Days:"
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (customer.overdueDays > 0) "${customer.overdueDays} يوم" else "لا يوجد", // "X days" or "None"
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (customer.overdueDays > 30)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (customer.overdueDays > 30) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠ دين حرج — متأخر أكثر من 30 يوم", // "Critical debt — overdue more than 30 days"
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق") // "Close"
            }
        }
    )
}

// ── Rating Update Dialog ─────────────────────────────────────────────

/**
 * Dialog for updating a customer's reliability rating (1-5 stars).
 *
 * Requirement 7.2: Accept a value between 1 and 5 stars inclusive.
 */
@Composable
private fun RatingUpdateDialog(
    customer: CustomerData,
    onDismiss: () -> Unit,
    onUpdateRating: (customerId: String, newRating: Int) -> Unit
) {
    var selectedRating by remember { mutableStateOf(customer.reliabilityRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحديث تقييم الموثوقية") }, // "Update Reliability Rating"
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "اختر التقييم الجديد (1-5 نجوم):", // "Select new rating (1-5 stars):"
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StarRatingSelector(
                    selectedRating = selectedRating,
                    onRatingSelected = { selectedRating = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onUpdateRating(customer.id, selectedRating) },
                enabled = selectedRating in 1..5
            ) {
                Text("تحديث") // "Update"
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء") // "Cancel"
            }
        }
    )
}
