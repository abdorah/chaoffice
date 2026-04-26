package com.inventory.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.util.ErrorHandler
import com.inventory.ffi.FfiException
import com.inventory.ffi.FfiMovementType
import com.inventory.ffi.FfiRecordStockMovementDto
import com.inventory.ffi.FfiStockHistoryDto
import com.inventory.ffi.FfiStockSummaryDto
import com.inventory.ffi.mobileGetStockHistory
import com.inventory.ffi.mobileGetStockSummary
import com.inventory.ffi.mobileRecordStockMovement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockViewModel : ViewModel() {

    private val _summary = MutableStateFlow<FfiStockSummaryDto?>(null)
    val summary: StateFlow<FfiStockSummaryDto?> = _summary.asStateFlow()

    private val _history = MutableStateFlow<FfiStockHistoryDto?>(null)
    val history: StateFlow<FfiStockHistoryDto?> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSummary(sessionToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileGetStockSummary(sessionToken)
                }
                _summary.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = ErrorHandler.handleAnyError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHistory(sessionToken: String, productId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileGetStockHistory(sessionToken, productId)
                }
                _history.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = ErrorHandler.handleAnyError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun recordMovement(sessionToken: String, dto: FfiRecordStockMovementDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileRecordStockMovement(sessionToken, dto)
                }
                loadSummary(sessionToken)
                onSuccess()
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = ErrorHandler.handleAnyError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
