package org.sweetlab

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.sweetlab.core.AppException
import org.sweetlab.core.FfiConverterString
import org.sweetlab.core.FfiConverterOptionalString
import org.sweetlab.core.Session
import org.sweetlab.core.SweetLabCore
import org.sweetlab.core.UniffiLib
import org.sweetlab.core.uniffiRustCallAsync

/**
 * Application class for Sweet Lab ERP.
 * Initializes the Rust core engine via UniFFI bindings on startup.
 */
class SweetLabApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SweetLabApp"

        /**
         * Singleton reference to the Rust core engine.
         * Initialized asynchronously during Application.onCreate().
         * UI components should observe [isInitialized] before calling core methods.
         */
        var core: SweetLabCore? = null
            private set

        @Volatile
        var isInitialized: Boolean = false
            private set

        @Volatile
        var initError: String? = null
            private set

        /**
         * Shared session state — set after successful login, cleared on logout.
         */
        @Volatile
        var currentSession: Session? = null

        @Volatile
        var currentUserId: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        initializeCore()
    }

    private fun initializeCore() {
        applicationScope.launch {
            try {
                Log.i(TAG, "Initializing Sweet Lab core engine...")

                val dbPath = getDatabasePath("sweetlab.db").absolutePath

                // Initialize the Rust core via UniFFI async constructor.
                // The UniFFI-generated bindings do not expose a Kotlin-level
                // suspend factory for async constructors, so we call the FFI
                // function directly and drive the Rust future to completion.
                core = createSweetLabCore(dbPath, "Sweet Lab", null, null)

                isInitialized = true
                Log.i(TAG, "Sweet Lab core engine initialized successfully")
            } catch (e: Exception) {
                initError = e.message ?: "Unknown initialization error"
                Log.e(TAG, "Failed to initialize core engine", e)
            }
        }
    }
}

/**
 * Suspend helper that wraps the async SweetLabCore FFI constructor.
 *
 * UniFFI marks the Rust constructor as `[Async]`, which means the generated
 * Kotlin class has no high-level `new()` factory.  We bridge the gap by
 * calling the low-level FFI function and polling the Rust future ourselves,
 * exactly the same way every generated async *method* does it.
 */
@Throws(AppException::class)
internal suspend fun createSweetLabCore(
    dbPath: String,
    businessName: String,
    syncBaseUrl: String?,
    fontDir: String?,
): SweetLabCore {
    return uniffiRustCallAsync(
        UniffiLib.INSTANCE.uniffi_sweet_lab_core_fn_constructor_sweetlabcore_new(
            FfiConverterString.lower(dbPath),
            FfiConverterString.lower(businessName),
            FfiConverterOptionalString.lower(syncBaseUrl),
            FfiConverterOptionalString.lower(fontDir),
        ),
        { future, callback, continuation ->
            UniffiLib.INSTANCE.ffi_sweet_lab_core_rust_future_poll_pointer(future, callback, continuation)
        },
        { future, continuation ->
            UniffiLib.INSTANCE.ffi_sweet_lab_core_rust_future_complete_pointer(future, continuation)
        },
        { future ->
            UniffiLib.INSTANCE.ffi_sweet_lab_core_rust_future_free_pointer(future)
        },
        // lift: convert the raw Pointer into a SweetLabCore instance
        { ptr -> SweetLabCore(ptr) },
        // error handler
        AppException.ErrorHandler,
    )
}
