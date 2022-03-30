package com.neeva.app

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NeevaActivityViewModel(
    /** Intent that must be processed once WebLayer has finished initializing. */
    private var pendingLaunchIntent: Intent?,
    private val neevaUser: NeevaUser,
    private val spaceStore: SpaceStore,
    private val webLayerModel: WebLayerModel,
    private val dispatchers: Dispatchers
) : ViewModel() {
    private val _isUpdateAvailableFlow = MutableStateFlow(false)
    val isUpdateAvailableFlow: StateFlow<Boolean> = _isUpdateAvailableFlow

    /**
     * WebLayer provides information about when the bottom and top toolbars need to be scrolled off.
     * We provide a placeholder instead of the real view because WebLayer has a bug that prevents it
     * from rendering Composables properly.
     * TODO(dan.alcantara): Revisit this once we move past WebLayer/Chromium v98.
     */
    internal val topControlOffset = MutableStateFlow(0.0f)
    internal val bottomControlOffset = MutableStateFlow(0.0f)

    /**
     * Returns an Intent that needs to be processed when everything has been initialized.
     * Subsequent calls will return no Intent.
     *
     * This mechanism relies on the ViewModel being created and kept alive across the Activity's
     * lifecycle.  When the Activity creates the ViewModel for the first time, it is constructed
     * using the Intent used to start the Activity.  If the Activity is alive when a new Intent
     * comes in, or if the Activity is recreated due to a configuration change, the ViewModel stays
     * alive so we know not to process the Intent again.
     */
    fun getPendingLaunchIntent(): Intent? {
        val intentToReturn = pendingLaunchIntent
        pendingLaunchIntent = null
        return intentToReturn
    }

    fun onBottomBarOffsetChanged(offset: Int) { bottomControlOffset.value = offset.toFloat() }
    fun onTopBarOffsetChanged(offset: Int) { topControlOffset.value = offset.toFloat() }

    class Factory(
        private val pendingLaunchIntent: Intent?,
        private val neevaUser: NeevaUser,
        private val spaceStore: SpaceStore,
        private val webLayerModel: WebLayerModel,
        private val dispatchers: Dispatchers
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return NeevaActivityViewModel(
                pendingLaunchIntent,
                neevaUser,
                spaceStore,
                webLayerModel,
                dispatchers
            ) as? T ?: throw IllegalArgumentException("Unexpected ViewModel class: $modelClass")
        }
    }

    internal fun checkForUpdates(context: Context) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Skipping update check for debug build")
            return
        }

        val appUpdateManager = AppUpdateManagerFactory.create(context)
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                _isUpdateAvailableFlow.value = info.updateAvailability() == UPDATE_AVAILABLE
                Log.i(TAG, "Update check result: ${_isUpdateAvailableFlow.value}")
            }
            .addOnFailureListener {
                Log.w(TAG, "Failed to check for update", it)
            }
    }

    fun signOut() {
        viewModelScope.launch(dispatchers.io) { spaceStore.deleteAllData() }
        neevaUser.clearUser()
        webLayerModel.clearNeevaCookies()
    }

    companion object {
        private val TAG = NeevaActivityViewModel::class.simpleName
    }
}
