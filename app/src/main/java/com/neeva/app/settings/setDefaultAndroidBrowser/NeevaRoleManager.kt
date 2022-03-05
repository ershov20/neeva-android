package com.neeva.app.settings.setDefaultAndroidBrowser

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

@RequiresApi(Build.VERSION_CODES.Q)
class NeevaRoleManager(
    activity: AppCompatActivity
) : SetDefaultAndroidBrowserManager() {
    private val androidDefaultBrowserRequester =
        activity.registerForActivityResult(DefaultAndroidBrowserRequester(this)) {}

    private val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager

    override val isDefaultBrowser: MutableState<Boolean> =
        mutableStateOf(isNeevaTheDefaultBrowser())

    override fun isNeevaTheDefaultBrowser(): Boolean {
        return roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
    }

    override fun isRoleManagerAvailable(): Boolean {
        return true
    }

    override fun requestToBeDefaultBrowser() {
        if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
            androidDefaultBrowserRequester.launch(null)
        }
    }

    internal fun makeRequestRoleIntent(): Intent {
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
internal class DefaultAndroidBrowserRequester(
    private val neevaRoleManager: NeevaRoleManager
) : ActivityResultContract<Void?, Boolean?>() {

    override fun createIntent(context: Context, input: Void?): Intent {
        return neevaRoleManager.makeRequestRoleIntent()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean? {
        neevaRoleManager.updateIsDefaultBrowser()
        return null
    }
}
