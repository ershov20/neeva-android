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
import org.chromium.weblayer.WebLayer

/** Maintains the logic for an Incognito browser profile. */
class IncognitoBrowserWrapper(
    appContext: Application,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : BrowserWrapper(
    isIncognito = true,
    appContext = appContext,
    coroutineScope = coroutineScope
) {
    companion object {
        val TAG = IncognitoBrowserWrapper::class.simpleName
        const val FOLDER_NAME = "incognito"
    }

    override val tabScreenshotter: TabScreenshotter = TabScreenshotter(
        Files.createTempDirectory(FOLDER_NAME).toFile().also {
            Log.d(TAG, "Created temporary folder: ${it.absolutePath}")
            it.deleteOnExit()
        }
    )

    override val suggestionsModel: SuggestionsModel? = null
    override val historyManager: HistoryManager? = null
    override val faviconCache: FaviconCache? = null

    override fun createBrowserFragment(): Fragment {
        return WebLayer.createBrowserFragmentWithIncognitoProfile(null, null)
    }

    override fun unregisterBrowserAndTabCallbacks() {
        Log.d(TAG, "Marking incognito profile for deletion")
        browser.profile.destroyAndDeleteDataFromDiskSoon {
            Log.d(TAG, "Destroyed incognito profile")
        }

        // TODO(dan.alcantara): Make sure to delete any screenshots or favicons we saved.

        super.unregisterBrowserAndTabCallbacks()
    }
}
