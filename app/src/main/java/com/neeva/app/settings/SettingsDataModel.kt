// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.cookiecutter.CookieCutterModel
import com.neeva.app.settings.clearbrowsing.TimeClearingOption
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import java.util.EnumSet

/**
 * A data model for getting any Settings-related state ([SettingsToggle] or [TimeClearingOption]).
 * Used to get toggle states in SettingsController.
 * FEATURE FLAGGING: used in any @Composable or anywhere else to get if a Feature Flag is enabled.
 *
 * This includes:
 *    - Holding all toggle MutableStates (which are based on their SharedPref values)
 *    - Being a wrapper class for Settings-SharedPreferences
 *    - Holding DEBUG-mode-only flags as MutableStates
 */
class SettingsDataModel(val sharedPreferencesModel: SharedPreferencesModel) {
    private val toggleMap = mutableMapOf<String, MutableState<Boolean>>()
    private val cookieCutterMode = mutableStateOf(
        CookieCutterModel.BlockingStrength.valueOf(
            getSharedPrefValue(
                CookieCutterModel.BLOCKING_STRENGTH_SHARED_PREF_KEY,
                CookieCutterModel.BlockingStrength.TRACKER_COOKIE.name
            )
        )
    )
    private val selectedTimeClearingOptionIndex = mutableStateOf(
        getSharedPrefValue(TimeClearingOption.SHARED_PREF_KEY, 0)
    )

    val cookieNoticePreferences = mutableStateOf(loadCookieNoticePreferences())

    init {
        SettingsToggle.values().forEach {
            toggleMap[it.key] = mutableStateOf(getSharedPrefValue(it.key, it.defaultValue))
        }
    }

    private fun <T : Any> getSharedPrefValue(key: String, defaultValue: T): T {
        return sharedPreferencesModel.getValue(SharedPrefFolder.Settings, key, defaultValue)
    }

    private fun setSharedPrefValue(key: String, newValue: Any) {
        sharedPreferencesModel.setValue(SharedPrefFolder.Settings, key, newValue)
    }

    /** When invoked, flips the value of the Boolean preference. */
    fun getTogglePreferenceToggler(settingsToggle: SettingsToggle): () -> Unit {
        return {
            val currentValue = getSettingsToggleValue(settingsToggle)
            setToggleState(settingsToggle, !currentValue)
        }
    }

    fun getTogglePreferenceSetter(settingsToggle: SettingsToggle): (Boolean) -> Unit {
        return { newToggleValue -> setToggleState(settingsToggle, newToggleValue) }
    }

    fun getSettingsToggleValue(settingsToggle: SettingsToggle): Boolean {
        return getToggleState(settingsToggle).value
    }

    fun getToggleState(settingsToggle: SettingsToggle): MutableState<Boolean> {
        check(toggleMap[settingsToggle.key] != null)
        return toggleMap[settingsToggle.key] ?: mutableStateOf(false)
    }

    fun setToggleState(settingsToggle: SettingsToggle, newToggleValue: Boolean) {
        getToggleState(settingsToggle).value = newToggleValue
        setSharedPrefValue(settingsToggle.key, newToggleValue)
    }

    fun getCookieCutterStrength(): CookieCutterModel.BlockingStrength {
        return cookieCutterMode.value
    }

    fun setCookieCutterStrength(strength: CookieCutterModel.BlockingStrength) {
        setSharedPrefValue(CookieCutterModel.BLOCKING_STRENGTH_SHARED_PREF_KEY, strength.name)
        cookieCutterMode.value = strength
    }

    fun getCookieNoticePreferences(): Set<CookieCutterModel.CookieNoticeCookies> {
        return cookieNoticePreferences.value
    }

    private fun loadCookieNoticePreferences(): Set<CookieCutterModel.CookieNoticeCookies> {
        // first get the serialized value
        val serialized = getSharedPrefValue(
            CookieCutterModel.COOKIE_NOTICE_PREFERENCES_SHARED_PREF_KEY,
            ""
        )

        // then deserialize and convert it to an enumset
        val list = serialized.split(";")
            .filter { it.isNotEmpty() }
            .map {
                CookieCutterModel.CookieNoticeCookies.values()[it.toInt()]
            }

        // copyOf throws if called with an empty list
        return if (list.isEmpty()) {
            EnumSet.noneOf(CookieCutterModel.CookieNoticeCookies::class.java)
        } else {
            EnumSet.copyOf(list)
        }
    }

    fun setCookieNoticePreferences(selection: Set<CookieCutterModel.CookieNoticeCookies>) {
        cookieNoticePreferences.value = selection

        // serialize the selected cookies into a single value
        val serialized = selection.joinToString(";") { it.ordinal.toString() }

        setSharedPrefValue(CookieCutterModel.COOKIE_NOTICE_PREFERENCES_SHARED_PREF_KEY, serialized)
    }

    fun getTimeClearingOptionIndex(): MutableState<Int> {
        return selectedTimeClearingOptionIndex
    }

    fun saveSelectedTimeClearingOption(index: Int) {
        setSharedPrefValue(TimeClearingOption.SHARED_PREF_KEY, index)
    }

    fun toggleIsAdvancedSettingsAllowed() {
        val newValue = !getSettingsToggleValue(SettingsToggle.IS_ADVANCED_SETTINGS_ALLOWED)
        getTogglePreferenceSetter(SettingsToggle.IS_ADVANCED_SETTINGS_ALLOWED).invoke(newValue)
    }
}
