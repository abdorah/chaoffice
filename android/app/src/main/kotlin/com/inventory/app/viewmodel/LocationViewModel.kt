package com.inventory.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.util.ErrorHandler
import com.inventory.ffi.FfiCreateLocationDto
import com.inventory.ffi.FfiException
import com.inventory.ffi.FfiLocationDto
import com.inventory.ffi.FfiUpdateLocationDto
import com.inventory.ffi.mobileCreateLocation
import com.inventory.ffi.mobileGetAllLocations
import com.inventory.ffi.mobileRemoveLocation
import com.inventory.ffi.mobileUpdateLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationViewModel : ViewModel() {

    private val _locations = MutableStateFlow<List<FfiLocationDto>>(emptyList())
    val locations: StateFlow<List<FfiLocationDto>> = _locations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadLocations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileGetAllLocations()
                }
                _locations.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = ErrorHandler.handleAnyError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createLocation(dto: FfiCreateLocationDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileCreateLocation(dto)
                }
                loadLocations()
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

    fun updateLocation(dto: FfiUpdateLocationDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileUpdateLocation(dto)
                }
                loadLocations()
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

    fun removeLocation(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileRemoveLocation(id)
                }
                loadLocations()
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
