package com.hexoskin.astroskinhexoskindemo

import android.app.Application

/**
 * For the HxSDK to work, it is required to load the library file here at launch, you can do it in a companion object like below.
 */
class DemoApplication : Application() {
    lateinit var deviceManager: DeviceManager

    override fun onCreate() {
        super.onCreate()
        deviceManager = DeviceManager(this)
    }

    companion object {
        init {
            System.loadLibrary("hxsdk-native")
        }
    }
}