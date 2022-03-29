package com.neeva.app.ui.widgets.collapsingsection

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionState.Companion.toCollapsingSectionState

interface CollapsingSectionStateKey {
    val name: String
    val allowCompactState: Boolean
    val defaultValue: CollapsingSectionState
}

/**
 * Manages the [State] and SharedPreferences that keeps track of the state of a particular
 * collapsing section.
 */
open class CollapsingSectionStateModel<T : CollapsingSectionStateKey>(
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val sharedPrefFolder: SharedPrefFolder
) {
    private val stateMap = mutableMapOf<T, MutableState<CollapsingSectionState>>()

    /**
     * Returns the current State associated with the given key.  If the key does not yet exist in
     * SharedPreferences, the key is initialized with [CollapsingSectionStateKey.defaultValue].
     */
    fun getState(preferenceKey: T): State<CollapsingSectionState> {
        return stateMap.getOrPut(preferenceKey) {
            mutableStateOf(
                sharedPreferencesModel
                    .getValue(sharedPrefFolder, preferenceKey.name, preferenceKey.defaultValue.name)
                    .toCollapsingSectionState()
            )
        }
    }

    /** Advances the state to the next valid value and saves it to the SharedPreferences. */
    fun advanceState(prefKey: T) {
        val nextState = getState(prefKey).value.next(prefKey.allowCompactState)
        sharedPreferencesModel.setValue(sharedPrefFolder, prefKey.name, nextState.name)
        stateMap[prefKey]!!.value = nextState
    }
}
