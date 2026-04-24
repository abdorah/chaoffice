package com.inventory.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.util.ErrorHandler
import com.inventory.ffi.FfiException
import com.inventory.ffi.FfiReportFormat
import com.inventory.ffi.FfiReportResult
import com.inventory.ffi.mobileGenerateBudgetReport
import com.inventory.ffi.mobileGenerateInventoryReport
import com.inventory.ffi.mobileGeneratePurchasingReport
import com.inventory.ffi.mobileGenerateStockMovementReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportViewModel : ViewModel() {

    private val _result = MutableStateFlow<FfiReportResult?>(null)
    val result: StateFlow<FfiReportResult?> = _result.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun generateInventoryReport(format: FfiReportFormat, outputDir: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _result.value = null
            try {
                val res = withContext(Dispatchers.IO) {
                    mobileGenerateInventoryReport(format, outputDir)
                }
                _result.value = res
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateStockMovementReport(format: FfiReportFormat, outputDir: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _result.value = null
            try {
                val res = withContext(Dispatchers.IO) {
                    mobileGenerateStockMovementReport(
                        format, outputDir,
                        "2000-01-01T00:00:00Z", "2099-12-31T23:59:59Z"
                    )
                }
                _result.value = res
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateBudgetReport(format: FfiReportFormat, outputDir: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _result.value = null
            try {
                val res = withContext(Dispatchers.IO) {
                    mobileGenerateBudgetReport(
                        format, outputDir,
                        "2000-01-01T00:00:00Z", "2099-12-31T23:59:59Z",
                        false
                    )
                }
                _result.value = res
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generatePurchasingReport(format: FfiReportFormat, outputDir: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _result.value = null
            try {
                val res = withContext(Dispatchers.IO) {
                    mobileGeneratePurchasingReport(format, outputDir)
                }
                _result.value = res
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearResult() {
        _result.value = null
    }
}
