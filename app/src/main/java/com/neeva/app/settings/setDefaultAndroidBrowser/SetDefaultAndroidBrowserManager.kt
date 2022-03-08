package com.neeva.app.settings.setDefaultAndroidBrowser

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Used to check/set the Default Browser.
 * The subclass used here is dependent on the device's Android version.
 */
abstract class SetDefaultAndroidBrowserManager {
    abstract val isDefaultBrowser: MutableState<Boolean>

    fun updateIsDefaultBrowser() {
        isDefaultBrowser.value = isNeevaTheDefaultBrowser()
    }

    abstract fun isNeevaTheDefaultBrowser(): Boolean

    abstract fun isRoleManagerAvailable(): Boolean

    abstract fun requestToBeDefaultBrowser()

    companion object {
        fun create(activity: AppCompatActivity): SetDefaultAndroidBrowserManager {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NeevaRoleManager(activity)
            } else {
                PreQDefaultBrowserSetter(activity)
            }
        }
    }
}

/** Used for Preview Testing */
class FakeSetDefaultAndroidBrowserManager : SetDefaultAndroidBrowserManager() {
    override val isDefaultBrowser: MutableState<Boolean> = mutableStateOf(true)

    override fun isNeevaTheDefaultBrowser(): Boolean {
        return true
    }

    override fun isRoleManagerAvailable(): Boolean {
        return true
    }

    override fun requestToBeDefaultBrowser() {}
}
