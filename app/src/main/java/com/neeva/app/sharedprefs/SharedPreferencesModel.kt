// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.sharedprefs

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Provides access to SharedPreferences. */
class SharedPreferencesModel(private val appContext: Context) {
    private val preferenceFlowMap = mutableMapOf<SharedPrefKey<*>, MutableStateFlow<*>>()

    /**
     * Listeners that have been registered to detect changes to the SharedPreferences.
     * The listeners are held weakly by the SharedPreferences, so they have to be stored.
     */
    private val listeners = mutableListOf<OnSharedPreferenceChangeListener>()

    private fun getSharedPreferences(folder: SharedPrefFolder): SharedPreferences {
        return appContext.getSharedPreferences(folder.folderName, Context.MODE_PRIVATE)
    }

    /** Returns a [StateFlow] that can be collected to observe changes to a SharedPreference. */
    fun <T : Any> getFlow(
        folder: SharedPrefFolder,
        sharedPrefKey: SharedPrefKey<T>,
        defaultValue: T
    ): StateFlow<T> {
        @Suppress("UNCHECKED_CAST")
        return preferenceFlowMap.getOrPut(sharedPrefKey) {
            val newFlow = MutableStateFlow(getValue(folder, sharedPrefKey, defaultValue))

            val listener = OnSharedPreferenceChangeListener { _, _ ->
                newFlow.value = getValue(folder, sharedPrefKey, defaultValue)
            }
            listeners.add(listener)
            getSharedPreferences(folder).registerOnSharedPreferenceChangeListener(listener)

            newFlow
        } as StateFlow<T>
    }

    fun <T : Any> getValue(
        folder: SharedPrefFolder,
        sharedPrefKey: SharedPrefKey<T>,
        defaultValue: T
    ) = getValue(folder, sharedPrefKey.preferenceKey, defaultValue, sharedPrefKey.enumFromString)

    fun <T : Any> getValue(
        folder: SharedPrefFolder,
        key: String,
        defaultValue: T,
        enumFromString: ((String) -> T) = { throw IllegalStateException() }
    ): T {
        val sharedPrefs = getSharedPreferences(folder)
        val returnValue: Any? = when (defaultValue) {
            is Boolean -> sharedPrefs.getBoolean(key, defaultValue)
            is String -> sharedPrefs.getString(key, defaultValue)
            is Int -> sharedPrefs.getInt(key, defaultValue)
            is Enum<*> -> enumFromString.invoke(sharedPrefs.getString(key, defaultValue.name)!!)
            else -> defaultValue
        }

        @Suppress("UNCHECKED_CAST")
        return (returnValue as? T) ?: defaultValue
    }

    fun <T : Any> setValue(
        folder: SharedPrefFolder,
        sharedPrefKey: SharedPrefKey<T>,
        value: T,
        mustCommitImmediately: Boolean = false
    ) = setValue(folder, sharedPrefKey.preferenceKey, value, mustCommitImmediately)

    fun setValue(
        folder: SharedPrefFolder,
        key: String,
        value: Any,
        mustCommitImmediately: Boolean = false
    ) {
        val editor = getSharedPreferences(folder).edit()
        if (editor != null) {
            val putLambda = when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Enum<*> -> editor.putString(key, value.name)
                else -> throw IllegalArgumentException("Unsupported value type given")
            }

            if (mustCommitImmediately) {
                putLambda.commit()
            } else {
                putLambda.apply()
            }
        }
    }

    fun <T : Any> removeValue(folder: SharedPrefFolder, sharedPrefKey: SharedPrefKey<T>) {
        removeValue(folder, sharedPrefKey.preferenceKey)
    }

    private fun removeValue(folder: SharedPrefFolder, key: String) {
        getSharedPreferences(folder)
            .edit()
            .remove(key)
            .commit()
    }
}
