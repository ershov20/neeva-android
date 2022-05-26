package com.neeva.app.settings.main

import android.net.Uri
import androidx.annotation.StringRes
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsToggle

class MainSettingsData(neevaConstants: NeevaConstants) : SettingsPaneDataInterface {
    @StringRes
    override val topAppBarTitleResId: Int = R.string.settings
    override val shouldShowUserName: Boolean = false
    override val data = listOf(
        SettingsGroupData(
            R.string.company_name,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.PROFILE,
                    primaryLabelId = R.string.settings_sign_in_to_join_neeva
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.settings_account_settings,
                    url = Uri.parse(neevaConstants.appSettingsURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.settings_connected_apps,
                    url = Uri.parse(neevaConstants.appConnectionsURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.settings_invite_friends,
                    url = Uri.parse(neevaConstants.appReferralURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_general,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    primaryLabelId = R.string.settings_default_browser
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.SHOW_SEARCH_SUGGESTIONS
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.BLOCK_POP_UP_WINDOWS,
                    enabled = false
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.REQUIRE_CONFIRMATION_ON_TAB_CLOSE,
                )
            )
        ),
        SettingsGroupData(
            R.string.settings_privacy,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    primaryLabelId = R.string.settings_clear_browsing_data
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    settingsToggle = SettingsToggle.CLOSE_INCOGNITO_TABS
                ),
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    primaryLabelId = R.string.settings_cookie_cutter
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.settings_privacy_policy,
                    url = Uri.parse(neevaConstants.appPrivacyURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_support,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.settings_welcome_tours,
                    url = Uri.parse(neevaConstants.appWelcomeToursURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.settings_help_center,
                    url = Uri.parse(neevaConstants.appHelpCenterURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_about,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.BUTTON,
                    primaryLabelId = R.string.settings_neeva_browser_version,
                    openUrlViaIntent = true
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.settings_play_store_page,
                    url = neevaConstants.playStoreUri,
                    openUrlViaIntent = true
                ),
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    primaryLabelId = R.string.settings_licenses
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    primaryLabelId = R.string.settings_terms,
                    url = Uri.parse(neevaConstants.appTermsURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_debug_local,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    primaryLabelId = R.string.settings_debug_local_feature_flags
                )
            ),
            isForDebugOnly = true
        )
    )
}
