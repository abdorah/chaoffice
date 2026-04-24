package com.inventory.app.util

import com.inventory.ffi.FfiException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shared error handler that ViewModels use to signal authentication errors.
 * The navigation layer observes [authErrors] and navigates to Login when emitted.
 */
object ErrorHandler {

    private val _authErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authErrors: SharedFlow<String> = _authErrors.asSharedFlow()

    /**
     * Handles an FFI exception. Returns the error message string.
     * If it's an authentication error, emits on [authErrors] so the navigation layer can react.
     */
    fun handleFfiError(error: FfiException): String {
        val message = error.message ?: "Unknown error"
        if (error is FfiException.AuthenticationException) {
            _authErrors.tryEmit(message)
        }
        return message
    }
}
