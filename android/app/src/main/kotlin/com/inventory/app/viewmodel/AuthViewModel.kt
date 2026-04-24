package com.inventory.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.util.ErrorHandler
import com.inventory.ffi.FfiException
import com.inventory.ffi.FfiUserRole
import com.inventory.ffi.mobileChangePassword
import com.inventory.ffi.mobileLogin
import com.inventory.ffi.mobileLogout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _sessionToken = MutableStateFlow<String?>(null)
    val sessionToken: StateFlow<String?> = _sessionToken.asStateFlow()

    private val _userRole = MutableStateFlow<FfiUserRole?>(null)
    val userRole: StateFlow<FfiUserRole?> = _userRole.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileLogin(username, password)
                }
                _sessionToken.value = result.sessionToken
                _userRole.value = result.role
                _displayName.value = result.displayName
                _isLoggedIn.value = true
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = _sessionToken.value
                if (token != null) {
                    withContext(Dispatchers.IO) {
                        mobileLogout(token)
                    }
                }
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _sessionToken.value = null
                _userRole.value = null
                _displayName.value = null
                _isLoggedIn.value = false
                _isLoading.value = false
            }
        }
    }

    /**
     * Called when an AuthenticationError is detected from any ViewModel.
     * Clears session state so the navigation layer redirects to Login.
     */
    fun forceLogout() {
        _sessionToken.value = null
        _userRole.value = null
        _displayName.value = null
        _isLoggedIn.value = false
        _error.value = "Session expired. Please log in again."
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = _sessionToken.value
                    ?: throw IllegalStateException("Not logged in")
                val result = withContext(Dispatchers.IO) {
                    mobileChangePassword(token, oldPassword, newPassword)
                }
                if (!result.success) {
                    _error.value = result.errorMessage
                }
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } catch (e: IllegalStateException) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
