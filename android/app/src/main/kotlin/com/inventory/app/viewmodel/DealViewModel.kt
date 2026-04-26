package com.inventory.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.util.ErrorHandler
import com.inventory.ffi.FfiCreateDealDto
import com.inventory.ffi.FfiDealDto
import com.inventory.ffi.FfiException
import com.inventory.ffi.FfiUpdateDealDto
import com.inventory.ffi.mobileCreateDeal
import com.inventory.ffi.mobileGetAllDeals
import com.inventory.ffi.mobileRemoveDeal
import com.inventory.ffi.mobileUpdateDeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DealViewModel : ViewModel() {

    private val _deals = MutableStateFlow<List<FfiDealDto>>(emptyList())
    val deals: StateFlow<List<FfiDealDto>> = _deals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadDeals() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileGetAllDeals()
                }
                _deals.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = ErrorHandler.handleAnyError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createDeal(dto: FfiCreateDealDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileCreateDeal(dto)
                }
                loadDeals()
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

    fun updateDeal(dto: FfiUpdateDealDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileUpdateDeal(dto)
                }
                loadDeals()
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

    fun removeDeal(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileRemoveDeal(id)
                }
                loadDeals()
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
