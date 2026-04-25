package com.inventory.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.inventory.app.navigation.Screen
import com.inventory.ffi.mobileGetAllCategories
import com.inventory.ffi.mobileGetAllDeals
import com.inventory.ffi.mobileGetAllLocations
import com.inventory.ffi.mobileGetAllPersons
import com.inventory.ffi.mobileGetAllProducts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

private data class DashboardTile(
    val label: String,
    val count: Int?,
    val route: String
)

@Composable
fun DashboardScreen(navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    var productCount by remember { mutableStateOf<Int?>(null) }
    var categoryCount by remember { mutableStateOf<Int?>(null) }
    var personCount by remember { mutableStateOf<Int?>(null) }
    var dealCount by remember { mutableStateOf<Int?>(null) }
    var locationCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val products = async { runCatching { mobileGetAllProducts().size }.getOrNull() }
            val categories = async { runCatching { mobileGetAllCategories().size }.getOrNull() }
            val persons = async { runCatching { mobileGetAllPersons().size }.getOrNull() }
            val deals = async { runCatching { mobileGetAllDeals().size }.getOrNull() }
            val locations = async { runCatching { mobileGetAllLocations().size }.getOrNull() }

            productCount = products.await()
            categoryCount = categories.await()
            personCount = persons.await()
            dealCount = deals.await()
            locationCount = locations.await()
        }
        isLoading = false
    }

    val tiles = listOf(
        DashboardTile("Products", productCount, Screen.Products.route),
        DashboardTile("Categories", categoryCount, Screen.Categories.route),
        DashboardTile("People", personCount, Screen.Persons.route),
        DashboardTile("Deals", dealCount, Screen.Deals.route),
        DashboardTile("Locations", locationCount, Screen.Locations.route),
        DashboardTile("Stock Tracking", null, Screen.Stock.route),
        DashboardTile("Budget", null, Screen.Budget.route),
        DashboardTile("Reports", null, Screen.Reports.route),
        DashboardTile("Users", null, Screen.Users.route),
        DashboardTile("Sync Settings", null, Screen.Sync.route),
    )

    if (isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tiles) { tile ->
                DashboardCard(
                    label = tile.label,
                    count = tile.count,
                    onClick = { navController.navigate(tile.route) }
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(
    label: String,
    count: Int?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
