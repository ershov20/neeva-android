package com.neeva.app.cookiecutter

import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.Dispatchers
import com.neeva.app.R
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.storage.daos.HostInfoDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.chromium.weblayer.ContentFilterManager
import org.chromium.weblayer.ContentFilterMode

interface CookieCutterModel {
    var trackersAllowListFlow: MutableStateFlow<TrackersAllowList?>
    val trackingDataFlow: MutableStateFlow<TrackingData?>
    val enableTrackingProtection: MutableState<Boolean>

    fun setUpTrackingProtection(manager: ContentFilterManager)
    fun updateTrackingProtectionConfiguration()

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
}

class CookieCutterModelImpl(
    private val hostInfoDao: HostInfoDao?,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val settingsDataModel: SettingsDataModel
) : CookieCutterModel {
    private lateinit var contentFilterManager: ContentFilterManager
    override var trackersAllowListFlow = MutableStateFlow<TrackersAllowList?>(null)

    override val trackingDataFlow = MutableStateFlow<TrackingData?>(null)
    override val enableTrackingProtection = settingsDataModel
        .getToggleState(SettingsToggle.TRACKING_PROTECTION)

    override fun setUpTrackingProtection(manager: ContentFilterManager) {
        contentFilterManager = manager
        updateTrackingProtectionConfiguration()
        trackersAllowListFlow.value = TrackersAllowListImpl(
            hostInfoDao = hostInfoDao,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            contentFilterManager = contentFilterManager
        )
    }

    override fun updateTrackingProtectionConfiguration() {
        if (enableTrackingProtection.value) {
            val blockingStrength = settingsDataModel.getCookieCutterStrength()
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
}

class PreviewCookieCutterModel : CookieCutterModel {
    override var trackersAllowListFlow: MutableStateFlow<TrackersAllowList?> =
        MutableStateFlow(PreviewTrackersAllowList())
    override val trackingDataFlow: MutableStateFlow<TrackingData?> = MutableStateFlow(null)
    override val enableTrackingProtection: MutableState<Boolean> = mutableStateOf(true)
    override fun setUpTrackingProtection(manager: ContentFilterManager) { }
    override fun updateTrackingProtectionConfiguration() { }
}
