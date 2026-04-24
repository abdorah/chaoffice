package com.inventory.app.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.inventory.app.ui.components.AppDropdown
import com.inventory.app.ui.components.LoadingIndicator
import com.inventory.app.viewmodel.ReportViewModel
import com.inventory.ffi.FfiReportFormat
import java.io.File

@Composable
fun ReportScreen(reportViewModel: ReportViewModel) {
    val result by reportViewModel.result.collectAsState()
    val isLoading by reportViewModel.isLoading.collectAsState()
    val error by reportViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val formats = listOf(FfiReportFormat.PDF, FfiReportFormat.EXCEL, FfiReportFormat.CSV)
    var selectedFormat by remember { mutableStateOf(FfiReportFormat.PDF) }

    val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
        ?: context.filesDir.absolutePath

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            reportViewModel.clearError()
        }
    }

    LaunchedEffect(result) {
        result?.let {
            snackbarHostState.showSnackbar("Report saved: ${it.filePath}")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                LoadingIndicator()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Generate Reports", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    AppDropdown(
                        items = formats,
                        selectedItem = selectedFormat,
                        onItemSelected = { selectedFormat = it },
                        label = "Format",
                        itemLabel = { it.name }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { reportViewModel.generateInventoryReport(selectedFormat, outputDir) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Inventory Report") }
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { reportViewModel.generateStockMovementReport(selectedFormat, outputDir) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Stock Movement Report") }
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { reportViewModel.generateBudgetReport(selectedFormat, outputDir) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Budget Report") }
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { reportViewModel.generatePurchasingReport(selectedFormat, outputDir) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Purchasing Report") }

                    // Result display with share action
                    result?.let { res ->
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Report Generated", style = MaterialTheme.typography.titleSmall)
                                Text("Path: ${res.filePath}")
                                Text("Size: ${res.sizeBytes} bytes")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = { shareFile(context, res.filePath) }) {
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareFile(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    } catch (e: Exception) {
        // Fallback: just try a basic share
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Report file: $filePath")
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}
