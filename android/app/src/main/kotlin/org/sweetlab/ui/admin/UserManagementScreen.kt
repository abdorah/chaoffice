package org.sweetlab.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.sweetlab.SweetLabApp
import org.sweetlab.core.SafeUser
import org.sweetlab.core.UserRole

private val ROLE_OPTIONS = listOf(UserRole.ADMIN, UserRole.CHEF, UserRole.REPRESENTATIVE)
private val ROLE_LABELS = mapOf(
    UserRole.ADMIN to "مدير",
    UserRole.CHEF to "طاهٍ",
    UserRole.REPRESENTATIVE to "مندوب"
)

/** Display label for a [UserRole]. */
private fun UserRole.label(): String = ROLE_LABELS[this] ?: name

/**
 * User Management screen — list users, create new users, update roles.
 *
 * Requirements 24.4: Admin navigates to user management, fetches user list,
 * supports creating new users and updating roles via the Core_Engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val users = remember { mutableStateListOf<SafeUser>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf<SafeUser?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Fetch users on screen entry
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
            val result = core.listUsers(token)
            users.clear()
            users.addAll(result)
        } catch (e: Exception) {
            errorMessage = "فشل تحميل المستخدمين" // "Failed to load users"
        } finally {
            isLoading = false
        }
    }

    /** Re-fetch the user list from the core engine. */
    fun refreshUsers() {
        scope.launch {
            val token = SweetLabApp.currentSession?.sessionId ?: return@launch
            val core = SweetLabApp.core ?: return@launch
            try {
                val result = core.listUsers(token)
                users.clear()
                users.addAll(result)
                errorMessage = null
            } catch (_: Exception) { /* keep stale list visible */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة المستخدمين") }, // "User Management"
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
                Icon(Icons.Default.Add, contentDescription = "إضافة مستخدم") // "Add user"
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
            } else if (users.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "لا يوجد مستخدمون بعد", // "No users yet"
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users, key = { it.id }) { user ->
                        UserCard(
                            user = user,
                            onChangeRole = { showRoleDialog = user }
                        )
                    }
                }
            }
        }
    }

    // ── Create User Dialog ──
    if (showCreateDialog) {
        CreateUserDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { username, password, fullName, role ->
                scope.launch {
                    val token = SweetLabApp.currentSession?.sessionId
                    val core = SweetLabApp.core
                    if (token == null || core == null) {
                        errorMessage = "الجلسة غير متوفرة"
                        return@launch
                    }
                    try {
                        isSubmitting = true
                        core.createUser(token, username, password, fullName, role)
                        showCreateDialog = false
                        errorMessage = null
                        refreshUsers()
                    } catch (e: Exception) {
                        errorMessage = "فشل إنشاء المستخدم: ${e.message}"
                        // "Failed to create user"
                    } finally {
                        isSubmitting = false
                    }
                }
            }
        )
    }

    // ── Role Update Dialog ──
    if (showRoleDialog != null) {
        RoleUpdateDialog(
            user = showRoleDialog!!,
            onDismiss = { showRoleDialog = null },
            onUpdateRole = { userId, newRole ->
                scope.launch {
                    val token = SweetLabApp.currentSession?.sessionId
                    val core = SweetLabApp.core
                    if (token == null || core == null) {
                        errorMessage = "الجلسة غير متوفرة"
                        return@launch
                    }
                    try {
                        isSubmitting = true
                        core.updateUserRole(token, userId, newRole)
                        showRoleDialog = null
                        errorMessage = null
                        refreshUsers()
                    } catch (e: Exception) {
                        errorMessage = "فشل تحديث الدور: ${e.message}"
                        // "Failed to update role"
                    } finally {
                        isSubmitting = false
                    }
                }
            }
        )
    }
}

@Composable
private fun UserCard(user: SafeUser, onChangeRole: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.fullName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onChangeRole) {
                Text(user.role.label())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (username: String, password: String, fullName: String, role: UserRole) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(ROLE_OPTIONS[0]) }
    var roleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء مستخدم جديد") }, // "Create New User"
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الكامل") }, // "Full Name"
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم") }, // "Username"
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") }, // "Password"
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRole.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الدور") }, // "Role"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        ROLE_OPTIONS.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.label()) },
                                onClick = {
                                    selectedRole = role
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(username, password, fullName, selectedRole) },
                enabled = username.isNotBlank() && password.isNotBlank() && fullName.isNotBlank()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleUpdateDialog(
    user: SafeUser,
    onDismiss: () -> Unit,
    onUpdateRole: (userId: String, newRole: UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.role) }
    var roleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحديث دور المستخدم") }, // "Update User Role"
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${user.fullName} (${user.username})",
                    style = MaterialTheme.typography.bodyMedium
                )
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRole.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الدور الجديد") }, // "New Role"
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        ROLE_OPTIONS.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.label()) },
                                onClick = {
                                    selectedRole = role
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    text = "سيتم تطبيق الدور الجديد عند تسجيل الدخول التالي",
                    // "New role will be applied on next login"
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onUpdateRole(user.id, selectedRole) },
                enabled = selectedRole != user.role
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
