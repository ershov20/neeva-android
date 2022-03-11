package com.neeva.app.browsing

import android.content.Context
import android.net.Uri
import android.util.Log
import com.apollographql.apollo3.api.Optional
import com.neeva.app.ApolloWrapper
import com.neeva.app.Dispatchers
import com.neeva.app.StartIncognitoMutation
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.IncognitoTabScreenshotManager
import com.neeva.app.storage.favicons.IncognitoFaviconCache
import com.neeva.app.type.StartIncognitoInput
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.Tab
import org.chromium.weblayer.WebLayer

/** Maintains the logic for an Incognito browser profile. */
class IncognitoBrowserWrapper private constructor(
    appContext: Context,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    activityCallbackProvider: () -> ActivityCallbacks?,
    private val apolloWrapper: ApolloWrapper,
    private val onDestroyed: (IncognitoBrowserWrapper) -> Unit,
    private val tempDirectory: File,
    private val incognitoFaviconCache: IncognitoFaviconCache
) : BrowserWrapper(
    isIncognito = true,
    appContext = appContext,
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    activityCallbackProvider = activityCallbackProvider,
    suggestionsModel = null,
    faviconCache = incognitoFaviconCache,
    spaceStore = null
) {
    constructor(
        appContext: Context,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        activityCallbackProvider: () -> ActivityCallbacks?,
        apolloWrapper: ApolloWrapper,
        domainProvider: DomainProvider,
        onDestroyed: (IncognitoBrowserWrapper) -> Unit,
        tempDirectory: File = Files.createTempDirectory(FOLDER_PREFIX).toFile()
    ) : this(
        appContext = appContext,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        activityCallbackProvider = activityCallbackProvider,
        apolloWrapper = apolloWrapper,
        onDestroyed = onDestroyed,
        tempDirectory = tempDirectory,
        incognitoFaviconCache = IncognitoFaviconCache(
            appContext,
            tempDirectory,
            domainProvider,
            dispatchers
        )
    )

    companion object {
        val TAG = IncognitoBrowserWrapper::class.simpleName
        const val FOLDER_PREFIX = "incognito"
    }

    /** Whether or not the mutation required to start an Incognito session has succeeded. */
    private var isIncognitoMutationPerformed: Boolean = false

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
        coroutineScope.launch(dispatchers.io) {
            incognitoFaviconCache.clearMapping()
            CacheCleaner(appContext.cacheDir).run()
        }

        super.unregisterBrowserAndTabCallbacks()

        onDestroyed(this)
    }

    override fun shouldInterceptLoad(uri: Uri): Boolean {
        return !isIncognitoMutationPerformed && uri.isNeevaUri()
    }

    /** Perform the mutation necessary to get the Incognito URL. */
    override suspend fun getReplacementUrl(uri: Uri): Uri {
        val redirectUri: String? = withContext(dispatchers.io) {
            // The `StartIncognito` endpoint requires sending a relative URI, which we manually
            // construct by creating a new URI using the relevant parts.
            val toApi = Uri.Builder()
                .path(uri.path)
                .encodedQuery(uri.encodedQuery)
                .fragment(uri.fragment)
                .build()

            val response = apolloWrapper.performMutation(
                mutation = StartIncognitoMutation(
                    StartIncognitoInput(
                        redirect = Optional.presentIfNotNull(toApi.toString())
                    )
                ),
                userMustBeLoggedIn = true
            )
            response?.data?.result
        }

        return withContext(dispatchers.main) {
            if (redirectUri != null) {
                Log.i(TAG, "Incognito URI acquired; redirecting")
                isIncognitoMutationPerformed = true
                Uri.parse(redirectUri)
            } else {
                Log.w(TAG, "Failed to get redirect URI.  Falling back to original URI.")
                uri
            }
        }
    }

    override fun onBlankTabCreated(tab: Tab) {
        // We don't want to leave the user in a state where an `about:blank` tab exists.
        tab.dispatchBeforeUnloadAndClose()
    }
}
