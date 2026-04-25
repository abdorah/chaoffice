package com.inventory.app

import android.app.Application
import com.inventory.ffi.mobileInit
import com.inventory.ffi.mobileBootstrap

class InventoryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("mobile_ffi")

        // Ensure the files directory exists before passing the path to Rust/redb
        val dbDir = filesDir
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }

        try {
            mobileInit(dbDir.absolutePath + "/inventory_data.db")
            mobileBootstrap()
        } catch (e: Exception) {
            android.util.Log.e("InventoryApp", "Failed to initialize database", e)
        }
    }
}

