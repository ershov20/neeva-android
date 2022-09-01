// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.sharedprefs

import kotlinx.coroutines.flow.StateFlow

/**
 * Key used to find a SharedPreference with a particular type in a given [SharedPrefFolder].
 *
 * NEVER change the [preferenceKey] or we will lose access to the previously saved values.
 */
data class SharedPrefKey<T : Any>(
    internal val folder: SharedPrefFolder,
    internal val preferenceKey: String,
    internal val defaultValue: T,

    /** If T represents an enum, you must pass in a valid function. */
    internal val enumFromString: ((String) -> T) = { throw IllegalStateException() }
) {
    fun get(sharedPreferencesModel: SharedPreferencesModel): T {
        return sharedPreferencesModel.getValue(
            folder = folder,
            key = preferenceKey,
            defaultValue = defaultValue,
            enumFromString = enumFromString
        )
    }

    fun getFlow(sharedPreferencesModel: SharedPreferencesModel): StateFlow<T> {
        return sharedPreferencesModel.getFlow(
            folder = folder,
            sharedPrefKey = this,
            defaultValue = defaultValue
        )
    }

    fun set(
        sharedPreferencesModel: SharedPreferencesModel,
        value: T,
        mustCommitImmediately: Boolean = false
    ) {
        sharedPreferencesModel.setValue(
            folder = folder,
            key = preferenceKey,
            value = value,
            mustCommitImmediately = mustCommitImmediately
        )
    }

    fun remove(sharedPreferencesModel: SharedPreferencesModel) {
        sharedPreferencesModel.removeValue(
            folder = folder,
            sharedPrefKey = this
        )
    }
}
