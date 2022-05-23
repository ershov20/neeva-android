package com.neeva.app.sharedprefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Provides access to SharedPreferences for all features.
 *
 * Useful because:
 *     1) The Context being held is an application context, so we can avoid any resource leaks.
 *     2) Other classes don't need to keep their own references to Contexts, which could be storing
 *        the wrong kind.
 *     3) SharedPref functions don't need to be rewritten.
 */
class SharedPreferencesModel(private val appContext: Context) {
    private fun getSharedPreferences(folder: SharedPrefFolder): SharedPreferences {
        return appContext.getSharedPreferences(folder.folderName, Context.MODE_PRIVATE)
    }

    fun <T> getValue(
        folder: SharedPrefFolder,
        sharedPrefKey: SharedPrefKey<T>,
        defaultValue: T
    ) = getValue(folder, sharedPrefKey.preferenceKey, defaultValue)

    fun <T> getValue(folder: SharedPrefFolder, key: String, defaultValue: T): T {
        val sharedPrefs = getSharedPreferences(folder)
        val returnValue: Any? = when (defaultValue) {
            is Boolean -> sharedPrefs.getBoolean(key, defaultValue)
            is String -> sharedPrefs.getString(key, defaultValue)
            is Int -> sharedPrefs.getInt(key, defaultValue)
            else -> defaultValue
        }
        return (returnValue as? T) ?: defaultValue
    }

    fun <T : Any> setValue(folder: SharedPrefFolder, sharedPrefKey: SharedPrefKey<T>, value: T) {
        setValue(folder, sharedPrefKey.preferenceKey, value)
    }

    fun setValue(folder: SharedPrefFolder, key: String, value: Any) {
        val editor = getSharedPreferences(folder).edit()
        if (editor != null) {
            val putLambda = when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                else -> throw IllegalArgumentException("Unsupported value type given")
            }
            putLambda.commit()
        }
    }

    fun <T> removeValue(folder: SharedPrefFolder, sharedPrefKey: SharedPrefKey<T>) {
        removeValue(folder, sharedPrefKey.preferenceKey)
    }

    fun removeValue(folder: SharedPrefFolder, key: String) {
        getSharedPreferences(folder)
            .edit()
            .remove(key)
            .commit()
    }
}

/**
 * Groups the SharedPreferences of related features.
 *
 * NEVER change the [folderName] or we will lose access to the previously saved values.
 */
sealed class SharedPrefFolder(internal val folderName: String) {
    object App : SharedPrefFolder("APP") {
        val CheckForImportedDatabaseKey = SharedPrefKey<Boolean>("CHECK_FOR_IMPORTED_DATABASE_KEY")
        val SessionIdV2Key = SharedPrefKey<String>("SESSION_ID_V2")
    }

    object CookieCutter : SharedPrefFolder("COOKIE_CUTTER")

    object FirstRun : SharedPrefFolder("FIRST_RUN") {
        val FirstRunDone = SharedPrefKey<Boolean>("HAS_FINISHED_FIRST_RUN")
        val ShouldLogFirstLogin = SharedPrefKey<Boolean>("SHOULD_LOG_FIRST_LOGIN")
    }

    object Settings : SharedPrefFolder("SETTINGS")

    object Spaces : SharedPrefFolder("SPACES") {
        val ShowDescriptionsPreferenceKey = SharedPrefKey<Boolean>("SHOW_DESCRIPTIONS")
    }

    object User : SharedPrefFolder("USER") {
        val Token = SharedPrefKey<String>("TOKEN")
    }

    object WebLayer : SharedPrefFolder("WEBLAYER") {
        /**
         * Tracks whether the user is using the Regular or Incognito profile.  Meant to be read only
         * during WebLayerModel initialization.
         */
        val IsCurrentlyIncognito = SharedPrefKey<Boolean>("IsCurrentlyIncognito")
    }

    object ZeroQuery : SharedPrefFolder("ZERO_QUERY")
}

/**
 * Key used to find a SharedPreference with a particular type in a given [SharedPrefFolder].
 *
 * NEVER change the [preferenceKey] or we will lose access to the previously saved values.
 */
data class SharedPrefKey<T>(internal val preferenceKey: String)
