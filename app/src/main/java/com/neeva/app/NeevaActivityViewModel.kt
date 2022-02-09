package com.neeva.app

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException
import kotlinx.coroutines.flow.MutableStateFlow

class NeevaActivityViewModel(
    /** Intent that must be processed once WebLayer has finished initializing. */
    private var pendingLaunchIntent: Intent?
) : ViewModel() {
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

    class Factory(private val pendingLaunchIntent: Intent?) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NeevaActivityViewModel::class.java)) {
                return NeevaActivityViewModel(pendingLaunchIntent) as T
            }
            throw IllegalArgumentException("Unexpected ViewModel class: $modelClass")
        }
    }
}
