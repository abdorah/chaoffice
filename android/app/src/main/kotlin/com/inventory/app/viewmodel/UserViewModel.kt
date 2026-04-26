package com.inventory.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.util.ErrorHandler
import com.inventory.ffi.FfiCreateUserDto
import com.inventory.ffi.FfiException
import com.inventory.ffi.FfiUserDto
import com.inventory.ffi.mobileCreateUser
import com.inventory.ffi.mobileDeactivateUser
import com.inventory.ffi.mobileListUsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<FfiUserDto>>(emptyList())
    val users: StateFlow<List<FfiUserDto>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileListUsers()
                }
                _users.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
                android.util.Log.e("UserViewModel", "loadUsers failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createUser(dto: FfiCreateUserDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileCreateUser(dto)
                }
                loadUsers()
                onSuccess()
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
                android.util.Log.e("UserViewModel", "createUser failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deactivateUser(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileDeactivateUser(userId)
                }
                loadUsers()
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
                android.util.Log.e("UserViewModel", "deactivateUser failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
