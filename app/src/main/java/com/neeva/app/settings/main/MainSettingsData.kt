package com.neeva.app.settings.main

import android.net.Uri
import androidx.annotation.StringRes
import com.neeva.app.NeevaConstants
import com.neeva.app.NeevaConstants.appConnectionsURL
import com.neeva.app.NeevaConstants.appHelpCenterURL
import com.neeva.app.NeevaConstants.appPrivacyURL
import com.neeva.app.NeevaConstants.appReferralURL
import com.neeva.app.NeevaConstants.appSettingsURL
import com.neeva.app.NeevaConstants.appTermsURL
import com.neeva.app.NeevaConstants.appWelcomeToursURL
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType
import com.neeva.app.settings.SettingsToggle

object MainSettingsData : SettingsPaneDataInterface {
    @StringRes
    override val topAppBarTitleResId: Int = R.string.settings
    override val shouldShowUserName: Boolean = false
    override val data = listOf(
        SettingsGroupData(
            R.string.company_name,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.PROFILE,
                    titleId = R.string.settings_sign_in_to_join_neeva
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    titleId = R.string.settings_account_settings,
                    url = Uri.parse(appSettingsURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    titleId = R.string.settings_connected_apps,
                    url = Uri.parse(appConnectionsURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    titleId = R.string.settings_invite_friends,
                    url = Uri.parse(appReferralURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_general,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    titleId = R.string.settings_default_browser
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_show_search_search_suggestions,
                    togglePreferenceKey = SettingsToggle.SHOW_SEARCH_SUGGESTIONS.key,
                    enabled = false
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_block_pop_up_windows,
                    togglePreferenceKey = SettingsToggle.BLOCK_POP_UP_WINDOWS.key,
                    enabled = false
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_open_copied_links,
                    togglePreferenceKey = SettingsToggle.OFFER_TO_OPEN_COPIED_LINKS.key,
                    enabled = false
                )
            )
        ),
        SettingsGroupData(
            R.string.settings_privacy,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    titleId = R.string.settings_clear_browsing_data
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_close_incognito_tabs,
                    togglePreferenceKey = SettingsToggle.CLOSE_INCOGNITO_TABS.key,
                    enabled = false
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    titleId = R.string.settings_tracking_protection,
                    togglePreferenceKey = SettingsToggle.TRACKING_PROTECTION.key,
                    enabled = false
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    titleId = R.string.settings_privacy_policy,
                    url = Uri.parse(appPrivacyURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_support,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    titleId = R.string.settings_welcome_tours,
                    url = Uri.parse(appWelcomeToursURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    titleId = R.string.settings_help_center,
                    url = Uri.parse(appHelpCenterURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_about,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    titleId = R.string.settings_neeva_browser_version,
                    url = NeevaConstants.playStoreUri,
                    openUrlViaIntent = true
                ),
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    titleId = R.string.settings_licenses,
                    enabled = false
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    titleId = R.string.settings_terms,
                    url = Uri.parse(appTermsURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_debug_local,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    titleId = R.string.settings_debug_local_feature_flags
                )
            ),
            isForDebugOnly = true
        )
    )
}
