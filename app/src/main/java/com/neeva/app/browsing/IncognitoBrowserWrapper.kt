package com.neeva.app.browsing

import android.content.Context
import android.util.Log
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.IncognitoTabScreenshotManager
import com.neeva.app.storage.favicons.IncognitoFaviconCache
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chromium.weblayer.WebLayer

/** Maintains the logic for an Incognito browser profile. */
class IncognitoBrowserWrapper private constructor(
    appContext: Context,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    activityCallbackProvider: () -> ActivityCallbacks?,
    private val onDestroyed: () -> Unit,
    private val tempDirectory: File,
    private val incognitoFaviconCache: IncognitoFaviconCache
) : BrowserWrapper(
    isIncognito = true,
    appContext = appContext,
    coroutineScope = coroutineScope,
    activityCallbackProvider = activityCallbackProvider,
    suggestionsModel = null,
    faviconCache = incognitoFaviconCache
) {
    constructor(
        appContext: Context,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        activityCallbackProvider: () -> ActivityCallbacks?,
        domainProvider: DomainProvider,
        onDestroyed: () -> Unit,
        tempDirectory: File = Files.createTempDirectory(FOLDER_PREFIX).toFile()
    ) : this(
        appContext = appContext,
        coroutineScope = coroutineScope,
        activityCallbackProvider = activityCallbackProvider,
        onDestroyed = onDestroyed,
        tempDirectory = tempDirectory,
        incognitoFaviconCache = IncognitoFaviconCache(appContext, tempDirectory, domainProvider)
    )

    companion object {
        val TAG = IncognitoBrowserWrapper::class.simpleName
        const val FOLDER_PREFIX = "incognito"
    }

    override val historyManager: HistoryManager? = null

    override fun createBrowserFragment() =
        WebLayer.createBrowserFragmentWithIncognitoProfile(null, null)

    override fun createTabScreenshotManager() =
        IncognitoTabScreenshotManager(appContext, tempDirectory)

    override fun unregisterBrowserAndTabCallbacks() {
        // Tell WebLayer that it should destroy the incognito profile when it can.  This deletes any
        // temporary files or cookies that were created while the user was in the incognito session.
        Log.d(TAG, "Marking incognito profile for deletion")
        browser?.profile?.destroyAndDeleteDataFromDiskSoon {
            Log.d(TAG, "Destroyed incognito profile")
        }

        // Delete the local state that we keep track of separately from WebLayer.
        coroutineScope.launch(Dispatchers.IO) {
            incognitoFaviconCache.clearMapping()
            CacheCleaner(appContext.cacheDir).run()
        }

        super.unregisterBrowserAndTabCallbacks()

        onDestroyed()
    }
}
