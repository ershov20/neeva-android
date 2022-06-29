package com.neeva.app

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
open class NeevaBrowser : Application() {
    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        super.onCreate()
    }

    companion object {
        /** Returns true if the app is being run as part of an instrumentation test. */
        fun isBeingInstrumented(): Boolean {
            return try {
                // Look for a class that's compiled in only for the `androidTestImplementation`
                // target.  This check will fail
                Class.forName("androidx.test.ext.junit.runners.AndroidJUnit4")
                true
            } catch (e: LinkageError) {
                false
            } catch (e: ExceptionInInitializerError) {
                false
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }
}
