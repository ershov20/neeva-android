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

    @Suppress("UNCHECKED_CAST")
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

    fun <T : Any> setValue(
        folder: SharedPrefFolder,
        sharedPrefKey: SharedPrefKey<T>,
        value: T,
        mustCommitImmediately: Boolean = false
    ) {
        setValue(folder, sharedPrefKey.preferenceKey, value, mustCommitImmediately)
    }

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
                else -> throw IllegalArgumentException("Unsupported value type given")
            }

            if (mustCommitImmediately) {
                putLambda.commit()
            } else {
                putLambda.apply()
            }
        }
    }

    fun <T> removeValue(folder: SharedPrefFolder, sharedPrefKey: SharedPrefKey<T>) {
        removeValue(folder, sharedPrefKey.preferenceKey)
    }

    private fun removeValue(folder: SharedPrefFolder, key: String) {
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

        /**
         * Tracks whether the user is using the Regular or Incognito profile.  Meant to be read only
         * during WebLayerModel initialization.
         */
        val IsCurrentlyIncognito = SharedPrefKey<Boolean>("IS_CURRENTLY_INCOGNITO")

        val SpacesShowDescriptionsPreferenceKey =
            SharedPrefKey<Boolean>("SPACES_SHOW_DESCRIPTIONS")

        val ZeroQuerySuggestedSitesState =
            SharedPrefKey<String>("ZERO_QUERY_SUGGESTED_SITES_STATE")
        val ZeroQuerySuggestedQueriesState =
            SharedPrefKey<String>("ZERO_QUERY_SUGGESTED_QUERIES_STATE")
        val ZeroQueryCommunitySpacesState =
            SharedPrefKey<String>("ZERO_QUERY_COMMUNITY_SPACES_STATE")
        val ZeroQuerySpacesState =
            SharedPrefKey<String>("ZERO_QUERY_SPACES_STATE")
    }

    object FirstRun : SharedPrefFolder("FIRST_RUN") {
        val FirstRunDone = SharedPrefKey<Boolean>("HAS_FINISHED_FIRST_RUN")
        val ShouldLogFirstLogin = SharedPrefKey<Boolean>("SHOULD_LOG_FIRST_LOGIN")
    }

    object Settings : SharedPrefFolder("SETTINGS")

    object User : SharedPrefFolder("USER") {
        val Token = SharedPrefKey<String>("TOKEN")
    }
}

/**
 * Key used to find a SharedPreference with a particular type in a given [SharedPrefFolder].
 *
 * NEVER change the [preferenceKey] or we will lose access to the previously saved values.
 */
data class SharedPrefKey<T>(internal val preferenceKey: String)
