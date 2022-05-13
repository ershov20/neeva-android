package com.neeva.app.cookiecutter

import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.chromium.weblayer.ContentFilterManager
import org.chromium.weblayer.ContentFilterMode

// TODO: set up the profile
class CookieCutterModel(
    private val sharedPreferencesModel: SharedPreferencesModel
) {
    lateinit var contentFilterManager: ContentFilterManager

    val trackingDataFlow = MutableStateFlow<TrackingData?>(null)

    companion object {
        enum class BlockingStrength(
            val description: String,
            val blockingList: String,
            val mode: Int
        ) {
            TRACKER_COOKIE(
                "tracker cookies only",
                "assets/easyprivacy.proto",
                ContentFilterMode.BLOCK_COOKIES
            ),
            TRACKER_REQUEST(
                "tracker request",
                "assets/easyprivacy.proto",
                ContentFilterMode.BLOCK_REQUESTS
            ),
            // TODO: enable this when CSS blocking is supported
            // AD_BLOCK(
            //    "ad blocker",
            //    "assets/easylist.proto",
            //    ContentFilterMode.BLOCK_REQUESTS
            // )
        }

        const val BLOCKING_STRENGTH_KEY = "BLOCKING_STRENGTH"
        const val ENABLE_TRACKING_PROTECTION_KEY = "ENABLE_TRACKING_PROTECTION"
    }

    // TODO convert this to database and persist to disk
    val unblockedDomains: ArrayList<String> = ArrayList<String>()

    // TODO configure tracking protection when this flag is changed
    var enableTrackingProtection: Boolean
        get() =
            sharedPreferencesModel.getValue(
                SharedPrefFolder.COOKIE_CUTTER,
                ENABLE_TRACKING_PROTECTION_KEY,
                true
            )
        set(value) {
            sharedPreferencesModel.setValue(
                SharedPrefFolder.COOKIE_CUTTER,
                ENABLE_TRACKING_PROTECTION_KEY,
                value
            )
        }

    var blockingStrength: BlockingStrength
        get() =
            sharedPreferencesModel.getValue(
                SharedPrefFolder.COOKIE_CUTTER,
                BLOCKING_STRENGTH_KEY,
                BlockingStrength.TRACKER_COOKIE
            )
        set(value) {
            sharedPreferencesModel.setValue(
                SharedPrefFolder.COOKIE_CUTTER,
                BLOCKING_STRENGTH_KEY,
                value
            )
        }

    fun setUpTrackingProtection(manager: ContentFilterManager) {
        contentFilterManager = manager
        updateTrackingProtectionConfiguration()
    }

    fun updateTrackingProtectionConfiguration() {
        if (enableTrackingProtection) {
            contentFilterManager.setRulesFile(blockingStrength.blockingList)
            contentFilterManager.setMode(blockingStrength.mode)
            unblockedDomains.forEach(
                { domain -> contentFilterManager.addHostExclusion(domain) }
            )
            contentFilterManager.startFiltering()
        } else {
            contentFilterManager.stopFiltering()
        }
    }

    fun addToAllowList(domain: String) {
        unblockedDomains.add(domain)
        contentFilterManager.addHostExclusion(domain)
    }

    fun removeFromAllowList(domain: String) {
        unblockedDomains.remove(domain)
        contentFilterManager.removeHostExclusion(domain)
    }

    fun removeAllHostExclusion() {
        contentFilterManager.clearAllHostExclusions()
    }
}
