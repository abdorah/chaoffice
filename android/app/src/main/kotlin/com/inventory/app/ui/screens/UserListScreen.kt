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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.inventory.app.viewmodel.UserViewModel
import com.inventory.ffi.FfiCreateUserDto
import com.inventory.ffi.FfiUserRole

@Composable
fun UserListScreen(userViewModel: UserViewModel) {
    val users by userViewModel.users.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val error by userViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { userViewModel.loadUsers() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            userViewModel.clearError()
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { dto ->
                userViewModel.createUser(dto) { showCreateDialog = false }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create user")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                isLoading && users.isEmpty() -> LoadingIndicator()
                users.isEmpty() -> Text(
                    text = "No users yet",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(users, key = { it.id.toLong() }) { user ->
                        val status = if (user.isActive) "Active" else "Inactive"
                        EntityListItem(
                            title = user.displayName,
                            subtitle = "${user.username} · ${user.role.name} · $status",
                            trailingContent = {
                                if (user.isActive) {
                                    IconButton(onClick = {
                                        userViewModel.deactivateUser(user.id.toLong())
                                    }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Deactivate user",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (FfiCreateUserDto) -> Unit
) {
    val roles = listOf(
        FfiUserRole.ADMIN, FfiUserRole.MANAGER,
        FfiUserRole.OPERATOR, FfiUserRole.VIEWER
    )
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(FfiUserRole.OPERATOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create User") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                AppTextField(value = username, onValueChange = { username = it }, label = "Username")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = password, onValueChange = { password = it }, label = "Password")
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(value = displayName, onValueChange = { displayName = it }, label = "Display Name")
                Spacer(modifier = Modifier.height(8.dp))
                AppDropdown(
                    items = roles,
                    selectedItem = selectedRole,
                    onItemSelected = { selectedRole = it },
                    label = "Role",
                    itemLabel = { it.name }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    FfiCreateUserDto(
                        username = username,
                        password = password,
                        displayName = displayName,
                        role = selectedRole,
                        personId = null
                    )
                )
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
