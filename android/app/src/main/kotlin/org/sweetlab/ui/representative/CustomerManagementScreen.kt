package org.sweetlab.ui.representative

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.sweetlab.core.Customer
import org.sweetlab.ui.util.toMoneyDisplay

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
    val customers = remember { mutableStateListOf<Customer>() }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf<Customer?>(null) }
    var showRatingDialog by remember { mutableStateOf<Customer?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    suspend fun loadCustomers() {
        val token = SweetLabApp.currentSession?.sessionId ?: return
        val core = SweetLabApp.core ?: return
        try {
            isLoading = true
            errorMessage = null
            val result = core.getCustomers(token, null)
            customers.clear()
            customers.addAll(result)
        } catch (e: Exception) {
            errorMessage = "فشل تحميل بيانات العملاء" // "Failed to load customer data"
        } finally {
            isLoading = false
        }
    }

    suspend fun searchCustomers(query: String) {
        val token = SweetLabApp.currentSession?.sessionId ?: return
        val core = SweetLabApp.core ?: return
        try {
            errorMessage = null
            val result = if (query.isBlank()) {
                core.getCustomers(token, null)
            } else {
                core.searchCustomers(token, query, null)
            }
            customers.clear()
            customers.addAll(result)
        } catch (e: Exception) {
            errorMessage = "فشل البحث عن العملاء" // "Failed to search customers"
        }
    }

    LaunchedEffect(Unit) {
        loadCustomers()
    }

    Scaffold(
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
                    scope.launch {
                        searchCustomers(newQuery)
                    }
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (customers.isEmpty()) {
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
                    items(customers.toList(), key = { it.id }) { customer ->
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
                    val token = SweetLabApp.currentSession?.sessionId
                    val core = SweetLabApp.core
                    if (token == null || core == null) {
                        errorMessage = "الجلسة غير متوفرة"
                        showCreateDialog = false
                        return@launch
                    }
                    try {
                        isSubmitting = true
                        core.createCustomer(token, name, city, mobile)
                        showCreateDialog = false
                        successMessage = "تم إنشاء العميل بنجاح" // "Customer created successfully"
                        errorMessage = null
                        // Refresh customer list after creation
                        loadCustomers()
                    } catch (e: AppException.Duplicate) {
                        errorMessage = "رقم الجوال مسجل مسبقاً" // "Mobile number already registered"
                        successMessage = null
                    } catch (e: Exception) {
                        errorMessage = "فشل إنشاء العميل: ${e.message}" // "Failed to create customer"
                        successMessage = null
                    } finally {
                        isSubmitting = false
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
                    val token = SweetLabApp.currentSession?.sessionId
                    val core = SweetLabApp.core
                    if (token == null || core == null) {
                        errorMessage = "الجلسة غير متوفرة"
                        showRatingDialog = null
                        return@launch
                    }
                    try {
                        isSubmitting = true
                        core.updateReliabilityRating(token, customerId, newRating)
                        showRatingDialog = null
                        successMessage = "تم تحديث التقييم بنجاح" // "Rating updated successfully"
                        errorMessage = null
                        // Refresh customer list after rating update
                        loadCustomers()
                    } catch (e: Exception) {
                        errorMessage = "فشل تحديث التقييم: ${e.message}" // "Failed to update rating"
                        successMessage = null
                    } finally {
                        isSubmitting = false
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
 * Money totalDebt is i64 cents — divide by 100 for display.
 */
@Composable
private fun CustomerCard(
    customer: Customer,
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

                // Debt info — totalDebt is Money (i64 cents)
                if (customer.totalDebt > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "دين: ${customer.totalDebt.toMoneyDisplay()}", // "Debt:"
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
 * Requirement 7.3: Unique mobile number (enforced server-side, Duplicate error handled).
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
 * Money totalDebt is i64 cents — divide by 100 for display.
 *
 * Requirement 7.5: Display name, city, mobile, reliability rating, total debt, overdue days.
 */
@Composable
private fun CustomerProfileDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onUpdateRating: (Customer) -> Unit
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

                // Debt Summary — totalDebt is Money (i64 cents)
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
                                text = customer.totalDebt.toMoneyDisplay(),
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
    customer: Customer,
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
