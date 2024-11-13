package org.chaos.office.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.chaos.office.configuration.PageSize
import org.chaos.office.style.AppTheme

@Composable
fun SignInPage(
    screenSize: PageSize,
    handleSignIn: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (screenSize) {
                PageSize.Compact -> CompactSignInLayout(
                    username, password,
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    handleSignIn
                )

                PageSize.Medium -> MediumSignInLayout(
                    username, password,
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    handleSignIn
                )

                PageSize.Expanded -> ExpandedSignInLayout(
                    username, password,
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    handleSignIn
                )
            }
        }
    }
}

@Composable
fun CompactSignInLayout(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    handleSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SignIn",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = handleSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SignIn")
        }
    }
}

@Composable
fun MediumSignInLayout(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    handleSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.width(320.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.width(320.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = handleSignIn,
            modifier = Modifier.width(200.dp)
        ) {
            Text("Sign In")
        }
    }
}

@Composable
fun ExpandedSignInLayout(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    handleSignIn: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(64.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Our App",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Please sign in to continue",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(start = 64.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = handleSignIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SignIn")
                }
            }
        }
    }
}