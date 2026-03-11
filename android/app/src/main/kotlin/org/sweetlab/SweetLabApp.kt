package org.sweetlab

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
// UniFFI-generated bindings — available after Rust library is compiled
// import org.sweetlab.bindings.SweetLabCore

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
        // var core: SweetLabCore? = null
        //     private set

        @Volatile
        var isInitialized: Boolean = false
            private set

        @Volatile
        var initError: String? = null
            private set
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
                val policiesDir = filesDir.resolve("policies").absolutePath

                // Copy bundled policy files to internal storage on first run
                copyPoliciesIfNeeded(policiesDir)

                // Initialize the Rust core via UniFFI
                // core = SweetLabCore.new(dbPath, policiesDir)

                isInitialized = true
                Log.i(TAG, "Sweet Lab core engine initialized successfully")
            } catch (e: Exception) {
                initError = e.message ?: "Unknown initialization error"
                Log.e(TAG, "Failed to initialize core engine", e)
            }
        }
    }

    private fun copyPoliciesIfNeeded(policiesDir: String) {
        val dir = java.io.File(policiesDir)
        if (!dir.exists()) {
            dir.mkdirs()
            // Copy model.conf and policy.csv from assets to internal storage
            listOf("model.conf", "policy.csv").forEach { filename ->
                try {
                    assets.open("policies/$filename").use { input ->
                        java.io.File(dir, filename).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Policy file $filename not found in assets, will use defaults")
                }
            }
        }
    }
}
