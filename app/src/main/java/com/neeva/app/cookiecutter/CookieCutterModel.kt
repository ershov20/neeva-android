package com.neeva.app.cookiecutter

import androidx.annotation.StringRes
import com.neeva.app.Dispatchers
import com.neeva.app.R
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.entities.HostInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.ContentFilterManager
import org.chromium.weblayer.ContentFilterMode

class CookieCutterModel(
    private val hostInfoDao: HostInfoDao?,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val settingsDataModel: SettingsDataModel,
) {
    lateinit var contentFilterManager: ContentFilterManager

    val trackingDataFlow = MutableStateFlow<TrackingData?>(null)
    val enableTrackingProtection = settingsDataModel
        .getToggleState(SettingsToggle.TRACKING_PROTECTION)

    enum class BlockingStrength(
        @StringRes val description: Int,
        @StringRes val title: Int,
        val blockingList: String,
        val mode: Int
    ) {
        TRACKER_COOKIE(
            R.string.blocking_strength_standard_description,
            R.string.blocking_strength_standard_title,
            "assets/easyprivacy.proto",
            ContentFilterMode.BLOCK_COOKIES
        ),
        TRACKER_REQUEST(
            R.string.blocking_strength_strict_description,
            R.string.blocking_strength_strict_title,
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

    companion object {
        const val BLOCKING_STRENGTH_SHARED_PREF_KEY = "BLOCKING_STRENGTH"
    }

    fun setUpTrackingProtection(manager: ContentFilterManager) {
        contentFilterManager = manager
        updateTrackingProtectionConfiguration()
    }

    fun updateTrackingProtectionConfiguration() {
        if (enableTrackingProtection.value) {
            val blockingStrength: BlockingStrength = settingsDataModel.getCookieCutterStrength()
            contentFilterManager.setRulesFile(blockingStrength.blockingList)
            contentFilterManager.setMode(blockingStrength.mode)

            coroutineScope.launch(dispatchers.io) {
                hostInfoDao?.getAllTrackingAllowedHosts()?.forEach { hostInfo ->
                    contentFilterManager.addHostExclusion(
                        hostInfo.host
                    )
                }
            }
            contentFilterManager.startFiltering()
        } else {
            contentFilterManager.stopFiltering()
        }
    }

    /** Allow trackers on the given [host] */
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
