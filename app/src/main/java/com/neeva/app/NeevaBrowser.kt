package com.neeva.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeevaBrowser : Application() {
    companion object {
        var versionString: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        versionString = packageManager.getPackageInfo(packageName, 0).versionName
    }
}
