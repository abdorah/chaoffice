package com.inventory.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.app.util.ErrorHandler
import com.inventory.ffi.FfiException
import com.inventory.ffi.FfiSyncConfig
import com.inventory.ffi.FfiSyncResult
import com.inventory.ffi.FfiSyncStrategy
import com.inventory.ffi.mobileGetSyncConfig
import com.inventory.ffi.mobileSetSyncConfig
import com.inventory.ffi.mobileSyncPull
import com.inventory.ffi.mobileSyncPush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncViewModel : ViewModel() {

    private val _config = MutableStateFlow<FfiSyncConfig?>(null)
    val config: StateFlow<FfiSyncConfig?> = _config.asStateFlow()

    private val _syncResult = MutableStateFlow<FfiSyncResult?>(null)
    val syncResult: StateFlow<FfiSyncResult?> = _syncResult.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileGetSyncConfig()
                }
                _config.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveConfig(config: FfiSyncConfig, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    mobileSetSyncConfig(config)
                }
                _config.value = config
                onSuccess()
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun push(sessionToken: String) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null
            _syncResult.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileSyncPush(sessionToken)
                }
                _syncResult.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun pull(sessionToken: String) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null
            _syncResult.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    mobileSyncPull(sessionToken)
                }
                _syncResult.value = result
            } catch (e: FfiException) {
                _error.value = ErrorHandler.handleFfiError(e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
