package com.inventory.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.util.ErrorHandler
import com.inventory.ffi.FfiCategoryDto
import com.inventory.ffi.FfiCreateCategoryDto
import com.inventory.ffi.FfiException
import com.inventory.ffi.FfiUpdateCategoryDto
import com.inventory.ffi.mobileCreateCategory
import com.inventory.ffi.mobileGetAllCategories
import com.inventory.ffi.mobileRemoveCategory
import com.inventory.ffi.mobileUpdateCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryViewModel : ViewModel() {

    private val _categories = MutableStateFlow<List<FfiCategoryDto>>(emptyList())
    val categories: StateFlow<List<FfiCategoryDto>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileGetAllCategories()
                }
                _categories.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = ErrorHandler.handleAnyError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCategory(dto: FfiCreateCategoryDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileCreateCategory(dto)
                }
                loadCategories()
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

    fun updateCategory(dto: FfiUpdateCategoryDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileUpdateCategory(dto)
                }
                loadCategories()
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

    fun removeCategory(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileRemoveCategory(id.toULong())
                }
                loadCategories()
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
