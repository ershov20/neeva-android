package com.neeva.app.browsing

import android.app.Application
import android.util.Log
import androidx.fragment.app.Fragment
import com.neeva.app.history.HistoryManager
import com.neeva.app.storage.FaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chromium.weblayer.WebLayer

/**
 * Maintains the logic for an Incognito browser profile.
 *
 * TODO(dan.alcantara): Encrypt the screenshots that are taken using a per-session ID that
 *                      guarantees that we can't decrypt the screenshots once the app is restarted.
 */
class IncognitoBrowserWrapper(
    appContext: Application,
    activityCallbackProvider: () -> ActivityCallbacks?,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : BrowserWrapper(
    isIncognito = true,
    appContext = appContext,
    activityCallbackProvider = activityCallbackProvider,
    coroutineScope = coroutineScope
) {
    companion object {
        val TAG = IncognitoBrowserWrapper::class.simpleName
        const val FOLDER_PREFIX = "incognito"
    }

    override val suggestionsModel: SuggestionsModel? = null
    override val historyManager: HistoryManager? = null
    override val faviconCache: FaviconCache? = null

    override fun createBrowserFragment(): Fragment {
        return WebLayer.createBrowserFragmentWithIncognitoProfile(null, null)
    }

    override fun createTabScreenshotManager() = IncognitoTabScreenshotManager(
        appContext,
        Files.createTempDirectory(FOLDER_PREFIX).toFile()
    )

    override fun unregisterBrowserAndTabCallbacks() {
        // Tell WebLayer that it should destroy the incognito profile when it can.  This deletes any
        // temporary files or cookies that were created while the user was in the incognito session.
        Log.d(TAG, "Marking incognito profile for deletion")
        browser?.profile?.destroyAndDeleteDataFromDiskSoon {
            Log.d(TAG, "Destroyed incognito profile")
        }

        // Delete the local state that we keep track of separately from WebLayer.
        coroutineScope.launch(Dispatchers.IO) {
            CacheCleaner(appContext.cacheDir).run()
        }

        super.unregisterBrowserAndTabCallbacks()
    }
}
