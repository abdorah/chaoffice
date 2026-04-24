package com.inventory.app

import android.app.Application
import com.inventory.ffi.mobileInit
import com.inventory.ffi.mobileBootstrap

class InventoryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("mobile_ffi")
        mobileInit(filesDir.absolutePath + "/inventory_data.db")
        mobileBootstrap()
    }
}
