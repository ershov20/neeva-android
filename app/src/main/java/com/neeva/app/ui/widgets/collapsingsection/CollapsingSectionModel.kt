package com.neeva.app.ui.widgets.collapsingsection

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPrefKey
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionState.Companion.toCollapsingSectionState

interface CollapsingSectionStateSharedPref {
    val sharedPrefKey: SharedPrefKey<String>
    val allowCompactState: Boolean
    val defaultValue: CollapsingSectionState
}

/**
 * Manages the [State] and SharedPreferences that keeps track of the state of a particular
 * collapsing section.
 */
open class CollapsingSectionStateModel(
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val sharedPrefFolder: SharedPrefFolder
) {
    private val stateMap =
        mutableMapOf<CollapsingSectionStateSharedPref, MutableState<CollapsingSectionState>>()

    /**
     * Returns the current State associated with the given key.  If the key does not yet exist in
     * SharedPreferences, the key is initialized with
     * [CollapsingSectionStateSharedPref.defaultValue].
     */
    fun getState(
        preferenceKey: CollapsingSectionStateSharedPref
    ): State<CollapsingSectionState> {
        return stateMap.getOrPut(preferenceKey) {
            mutableStateOf(
                sharedPreferencesModel
                    .getValue(
                        sharedPrefFolder,
                        preferenceKey.sharedPrefKey,
                        preferenceKey.defaultValue.name
                    )
                    .toCollapsingSectionState()
            )
        }
    }

    /** Advances the state to the next valid value and saves it to the SharedPreferences. */
    fun advanceState(preferenceKey: CollapsingSectionStateSharedPref) {
        val nextState = getState(preferenceKey).value.next(preferenceKey.allowCompactState)
        sharedPreferencesModel.setValue(
            sharedPrefFolder,
            preferenceKey.sharedPrefKey,
            nextState.name
        )
        stateMap[preferenceKey]!!.value = nextState
    }
}
