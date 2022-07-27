package com.neeva.app.browsing

import android.content.Context
import android.net.Uri
import android.util.Log
import com.apollographql.apollo3.api.Optional
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.StartIncognitoMutation
import com.neeva.app.apollo.ApolloWrapper
import com.neeva.app.apollo.UnauthenticatedApolloWrapper
import com.neeva.app.cookiecutter.IncognitoTrackersAllowList
import com.neeva.app.cookiecutter.ScriptInjectionManager
import com.neeva.app.neevascope.NeevascopeModel
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.storage.Directories
import com.neeva.app.storage.IncognitoTabScreenshotManager
import com.neeva.app.storage.favicons.IncognitoFaviconCache
import com.neeva.app.type.StartIncognitoInput
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.NeevaUser
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import org.chromium.weblayer.Profile
import org.chromium.weblayer.Tab
import org.chromium.weblayer.WebLayer

/** Maintains the logic for an Incognito browser profile. */
class IncognitoBrowserWrapper private constructor(
    appContext: Context,
    activityCallbackProvider: ActivityCallbackProvider,
    private val authenticatedApolloWrapper: ApolloWrapper,
    private val unauthenticatedApolloWrapper: UnauthenticatedApolloWrapper,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    domainProvider: DomainProvider,
    private val incognitoFaviconCache: IncognitoFaviconCache,
    neevaConstants: NeevaConstants,
    private val onRemovedFromHierarchy: (IncognitoBrowserWrapper) -> Unit,
    settingsDataModel: SettingsDataModel,
    tabScreenshotManager: IncognitoTabScreenshotManager,
    scriptInjectionManager: ScriptInjectionManager,
    popupModel: PopupModel,
    neevaUser: NeevaUser
) : BaseBrowserWrapper(
    isIncognito = true,
    appContext = appContext,
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    activityCallbackProvider = activityCallbackProvider,
    suggestionsModel = null,
    neevascopeModel = NeevascopeModel(
        apolloWrapper = unauthenticatedApolloWrapper,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers
    ),
    popupModel = popupModel,
    neevaUser = neevaUser,
    faviconCache = incognitoFaviconCache,
    spaceStore = null,
    historyManager = null,
    tabScreenshotManager = tabScreenshotManager,
    domainProvider = domainProvider,
    neevaConstants = neevaConstants,
    settingsDataModel = settingsDataModel,
    trackerAllowList = IncognitoTrackersAllowList(),
    scriptInjectionManager = scriptInjectionManager
) {
    constructor(
        appContext: Context,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        activityCallbackProvider: ActivityCallbackProvider,
        authenticatedApolloWrapper: ApolloWrapper,
        unauthenticatedApolloWrapper: UnauthenticatedApolloWrapper,
        domainProvider: DomainProvider,
        onRemovedFromHierarchy: (IncognitoBrowserWrapper) -> Unit,
        neevaConstants: NeevaConstants,
        scriptInjectionManager: ScriptInjectionManager,
        settingsDataModel: SettingsDataModel,
        directories: Directories,
        tempDirectory: Deferred<File> = directories.cacheSubdirectoryAsync(FOLDER_NAME),
        popupModel: PopupModel,
        neevaUser: NeevaUser
    ) : this(
        appContext = appContext,
        activityCallbackProvider = activityCallbackProvider,
        authenticatedApolloWrapper = authenticatedApolloWrapper,
        unauthenticatedApolloWrapper = unauthenticatedApolloWrapper,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        domainProvider = domainProvider,
        incognitoFaviconCache = IncognitoFaviconCache(
            appContext,
            tempDirectory,
            domainProvider,
            dispatchers
        ),
        neevaConstants = neevaConstants,
        onRemovedFromHierarchy = onRemovedFromHierarchy,
        scriptInjectionManager = scriptInjectionManager,
        settingsDataModel = settingsDataModel,
        tabScreenshotManager = IncognitoTabScreenshotManager(
            appContext = appContext,
            filesDir = tempDirectory,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers
        ),
        popupModel = popupModel,
        neevaUser = neevaUser
    )

    companion object {
        val TAG = IncognitoBrowserWrapper::class.simpleName

        internal const val INCOGNITO_PROFILE_NAME = "incognito"
        private const val INCOGNITO_PERSISTENCE_ID = "incognito_persistence_id"

        const val FOLDER_NAME = INCOGNITO_PROFILE_NAME

        /** Gets the incognito user profile from WebLayer. The Browser does not need to be alive. */
        fun getProfile(webLayer: WebLayer) = webLayer.getIncognitoProfile(INCOGNITO_PROFILE_NAME)

        suspend fun cleanUpIncognito(
            dispatchers: Dispatchers,
            incognitoProfile: Profile?,
            cacheCleaner: CacheCleaner
        ) {
            // Tell WebLayer that it should destroy the incognito profile when it can.  This deletes
            // temporary files or cookies that were created while the user was in that session.
            withContext(dispatchers.main) {
                incognitoProfile?.apply {
                    Log.d(TAG, "Marking incognito profile for deletion")
                    destroyAndDeleteDataFromDiskSoon {
                        Log.d(TAG, "Destroyed incognito profile")
                    }
                }
            }

            // Delete temporary files we created to store Incognito favicons and tab screenshots.
            withContext(dispatchers.io) {
                cacheCleaner.run()
            }
        }
    }

    /** Whether or not the mutation required to start an Incognito session has succeeded. */
    private var isIncognitoMutationPerformed: Boolean = false

    override fun createBrowserFragment() = WebLayer.createBrowserFragmentWithIncognitoProfile(
        INCOGNITO_PROFILE_NAME,
        INCOGNITO_PERSISTENCE_ID
    )

    override fun unregisterBrowserAndTabCallbacks() {
        super.unregisterBrowserAndTabCallbacks()
        onRemovedFromHierarchy(this)
    }

    override fun shouldInterceptLoad(uri: Uri): Boolean {
        return !isIncognitoMutationPerformed && uri.isNeevaUri(neevaConstants)
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

            val response = authenticatedApolloWrapper.performMutation(
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
