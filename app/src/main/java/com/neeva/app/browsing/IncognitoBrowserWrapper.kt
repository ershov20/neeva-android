package com.neeva.app.browsing

import android.app.Application
import android.util.Log
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.IncognitoFaviconCache
import com.neeva.app.storage.IncognitoTabScreenshotManager
import com.neeva.app.suggestions.SuggestionsModel
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chromium.weblayer.WebLayer

/** Maintains the logic for an Incognito browser profile. */
class IncognitoBrowserWrapper(
    appContext: Application,
    activityCallbackProvider: () -> ActivityCallbacks?,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    domainProvider: DomainProvider
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

    private val tempDirectory = Files.createTempDirectory(FOLDER_PREFIX).toFile()
    private val encrypter = FileEncrypter(appContext)

    override val suggestionsModel: SuggestionsModel? = null
    override val historyManager: HistoryManager? = null

    override val faviconCache =
        IncognitoFaviconCache(tempDirectory, domainProvider, { browser?.profile }, encrypter)

    override fun createBrowserFragment() =
        WebLayer.createBrowserFragmentWithIncognitoProfile(null, null)

    override fun createTabScreenshotManager() =
        IncognitoTabScreenshotManager(tempDirectory, encrypter)

    override fun unregisterBrowserAndTabCallbacks() {
        // Tell WebLayer that it should destroy the incognito profile when it can.  This deletes any
        // temporary files or cookies that were created while the user was in the incognito session.
        Log.d(TAG, "Marking incognito profile for deletion")
        browser?.profile?.destroyAndDeleteDataFromDiskSoon {
            Log.d(TAG, "Destroyed incognito profile")
        }

        // Delete the local state that we keep track of separately from WebLayer.
        coroutineScope.launch(Dispatchers.IO) {
            faviconCache.clearMapping()
            CacheCleaner(appContext.cacheDir).run()
        }

        super.unregisterBrowserAndTabCallbacks()
    }
}
