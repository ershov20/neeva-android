package com.neeva.app.cookiecutter

import com.neeva.app.Dispatchers
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.entities.HostInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.ContentFilterManager
import org.chromium.weblayer.ContentFilterMode

class CookieCutterModel(
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val hostInfoDao: HostInfoDao?,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers
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

            coroutineScope.launch(dispatchers.io) {
                hostInfoDao?.getAllTrackingAllowedHosts()?.forEach(
                    { hostInfo -> contentFilterManager.addHostExclusion(hostInfo.host) }
                )
            }
            contentFilterManager.startFiltering()
        } else {
            contentFilterManager.stopFiltering()
        }
    }

    // host exclusion
    fun addToAllowList(host: String) {
        coroutineScope.launch {
            withContext(dispatchers.io) {
                hostInfoDao?.upsert(HostInfo(host = host, isTrackingAllowed = true))
            }
            withContext(dispatchers.main) {
                contentFilterManager.addHostExclusion(host)
            }
        }
    }

    fun removeFromAllowList(host: String) {
        coroutineScope.launch {
            withContext(dispatchers.io) {
                hostInfoDao?.deleteFromHostInfo(host)
            }
            withContext(dispatchers.main) {
                contentFilterManager.removeHostExclusion(host)
            }
        }
    }

    fun removeAllHostFromAllowList() {
        coroutineScope.launch {
            withContext(dispatchers.io) {
                hostInfoDao?.deleteTrackingAllowedHosts()
            }
            withContext(dispatchers.main) {
                contentFilterManager.clearAllHostExclusions()
            }
        }
    }
}
