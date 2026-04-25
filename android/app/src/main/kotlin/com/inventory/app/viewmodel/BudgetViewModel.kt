package com.inventory.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.util.ErrorHandler
import com.inventory.ffi.FfiBudgetProjectionDto
import com.inventory.ffi.FfiBudgetSummaryDto
import com.inventory.ffi.FfiException
import com.inventory.ffi.FfiGetBudgetProjectionDto
import com.inventory.ffi.FfiGetBudgetSummaryDto
import com.inventory.ffi.FfiRecordBudgetEntryDto
import com.inventory.ffi.mobileGetBudgetProjection
import com.inventory.ffi.mobileGetBudgetSummary
import com.inventory.ffi.mobileRecordBudgetEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetViewModel : ViewModel() {

    private val _summary = MutableStateFlow<FfiBudgetSummaryDto?>(null)
    val summary: StateFlow<FfiBudgetSummaryDto?> = _summary.asStateFlow()

    private val _projection = MutableStateFlow<FfiBudgetProjectionDto?>(null)
    val projection: StateFlow<FfiBudgetProjectionDto?> = _projection.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSummary(sessionToken: String, fromDate: String, toDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileGetBudgetSummary(
                        sessionToken,
                        FfiGetBudgetSummaryDto(fromDate = fromDate, toDate = toDate)
                    )
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

    fun loadProjection(sessionToken: String, monthsAhead: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileGetBudgetProjection(
                        sessionToken,
                        FfiGetBudgetProjectionDto(monthsAhead = monthsAhead)
                    )
                }
                _projection.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = ErrorHandler.handleAnyError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun recordEntry(sessionToken: String, dto: FfiRecordBudgetEntryDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileRecordBudgetEntry(sessionToken, dto)
                }
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
