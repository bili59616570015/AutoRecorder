package com.example.autorecorder

import android.app.Application
import android.content.Context

class AutoRecorderApp: Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        @JvmStatic
        lateinit var appContext: Context
            private set
    }
}

const val INVALID_ID = -1L