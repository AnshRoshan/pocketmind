package com.pocketmind

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PocketmindApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
