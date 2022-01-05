package com.neeva.app

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeevaBrowser: Application() {
    companion object {
        // TODO(dan.alcantara): Context shouldn't be stored like this.
        lateinit var context: Context

        var versionString: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        versionString = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }
}
