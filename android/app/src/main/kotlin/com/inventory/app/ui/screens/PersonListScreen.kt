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
import com.inventory.app.viewmodel.PersonViewModel
import com.inventory.ffi.FfiCreatePersonDto
import com.inventory.ffi.FfiPersonRole
import com.inventory.ffi.FfiUpdatePersonDto

@Composable
fun PersonListScreen(personViewModel: PersonViewModel) {
    val persons by personViewModel.persons.collectAsState()
    val isLoading by personViewModel.isLoading.collectAsState()
    val error by personViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(FfiPersonRole.SUPPLIER) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { personViewModel.loadPersons() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            personViewModel.clearError()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingId != null) "Edit Person" else "New Person") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AppTextField(value = name, onValueChange = { name = it }, label = "Name")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppDropdown(
                        items = listOf(FfiPersonRole.SUPPLIER, FfiPersonRole.MANAGER),
                        selectedItem = role,
                        onItemSelected = { role = it },
                        label = "Role",
                        itemLabel = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = phone, onValueChange = { phone = it }, label = "Phone")
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = email, onValueChange = { email = it }, label = "Email")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingId != null) {
                        personViewModel.updatePerson(
                            FfiUpdatePersonDto(
                                id = editingId!!,
                                name = name,
                                role = role
                            )
                        ) { showDialog = false }
                    } else {
                        personViewModel.createPerson(
                            FfiCreatePersonDto(
                                name = name,
                                role = role,
                                contactId = null
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
                editingId = null; name = ""; role = FfiPersonRole.SUPPLIER
                phone = ""; email = ""
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add person")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                isLoading && persons.isEmpty() -> LoadingIndicator()
                persons.isEmpty() -> Text(
                    text = "No people yet",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(persons, key = { it.id }) { person ->
                        EntityListItem(
                            title = person.name,
                            subtitle = person.role.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            onClick = {
                                editingId = person.id
                                name = person.name
                                role = person.role
                                phone = ""; email = ""
                                showDialog = true
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    personViewModel.removePerson(person.id)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete person",
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
