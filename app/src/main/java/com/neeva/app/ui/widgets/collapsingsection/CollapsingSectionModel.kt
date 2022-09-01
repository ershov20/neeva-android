// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets.collapsingsection

import androidx.compose.runtime.State
import com.neeva.app.sharedprefs.SharedPrefKey
import com.neeva.app.sharedprefs.SharedPreferencesModel
import kotlinx.coroutines.flow.StateFlow

interface CollapsingSectionStateSharedPref {
    val sharedPrefKey: SharedPrefKey<CollapsingSectionState>
    val allowCompactState: Boolean

    val defaultValue: CollapsingSectionState get() = sharedPrefKey.defaultValue
}

/**
 * Manages the [State] and SharedPreferences that keeps track of the state of a particular
 * collapsing section.
 */
open class CollapsingSectionStateModel(private val sharedPreferencesModel: SharedPreferencesModel) {
    /**
     * Returns the current State associated with the given key.  If the key does not yet exist in
     * SharedPreferences, the key is initialized with
     * [CollapsingSectionStateSharedPref.defaultValue].
     */
    fun getFlow(
        preferenceKey: CollapsingSectionStateSharedPref
    ): StateFlow<CollapsingSectionState> {
        return preferenceKey.sharedPrefKey.getFlow(sharedPreferencesModel)
    }

    /** Advances the state to the next valid value and saves it to the SharedPreferences. */
    fun advanceState(preferenceKey: CollapsingSectionStateSharedPref) = preferenceKey.apply {
        val nextState = sharedPrefKey
            .get(sharedPreferencesModel)
            .next(preferenceKey.allowCompactState)
        sharedPrefKey.set(sharedPreferencesModel, nextState)
    }
}
