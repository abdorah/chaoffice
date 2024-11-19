package org.chaos.office.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import chaoffice.composeapp.generated.resources.Res
import chaoffice.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun WelcomePage(
    appVersion: String,
    onFinish: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = 3

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentPage) {
                0 -> WelcomePage(
                    title = "Welcome to Our App",
                    description = "We're excited to have you here!",
                    imageRes = Res.drawable.compose_multiplatform
                )
                1 -> WelcomePage(
                    title = "Discover Amazing Features",
                    description = "Explore all the powerful tools we offer.",
                    imageRes = Res.drawable.compose_multiplatform
                )
                2 -> WelcomePage(
                    title = "Get Started",
                    description = "You're all set to begin your journey!",
                    imageRes = Res.drawable.compose_multiplatform
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentPage > 0) {
                    TextButton(onClick = { currentPage-- }) {
                        Text("Previous")
                    }
                } else {
                    Spacer(Modifier.width(64.dp))
                }

                if (currentPage < totalPages - 1) {
                    Button(onClick = { currentPage++ }) {
                        Text("Next")
                    }
                } else {
                    Button(onClick = onFinish) {
                        Text("Get Started")
                    }
                }
            }

            Text(
                text = "Version $appVersion",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun WelcomePage(title: String, description: String, imageRes: DrawableResource) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 16.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}