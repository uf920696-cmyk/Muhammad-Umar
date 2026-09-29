package com.jarvis.app

import android.app.Application
import android.util.Log

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("JarvisApplication", "JARVIS native application initialized")
    }
}
