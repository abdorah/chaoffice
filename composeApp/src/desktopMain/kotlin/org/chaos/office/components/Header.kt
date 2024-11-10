package org.chaos.office.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chaoffice.composeapp.generated.resources.Res
import chaoffice.composeapp.generated.resources.compose_multiplatform
import org.chaos.office.style.primaryLight
import org.jetbrains.compose.resources.painterResource


@Composable
fun Header(title: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = primaryLight
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(Res.drawable.compose_multiplatform), null)
            Text(
                text = title,
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.onPrimary
            )
        }
    }
}