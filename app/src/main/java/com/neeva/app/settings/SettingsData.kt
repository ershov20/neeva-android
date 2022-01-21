package com.neeva.app.settings

import android.net.Uri
import com.neeva.app.NeevaConstants.appConnectionsURL
import com.neeva.app.NeevaConstants.appHelpCenterURL
import com.neeva.app.NeevaConstants.appPrivacyURL
import com.neeva.app.NeevaConstants.appReferralURL
import com.neeva.app.NeevaConstants.appSettingsURL
import com.neeva.app.NeevaConstants.appTermsURL
import com.neeva.app.NeevaConstants.appWelcomeToursURL
import com.neeva.app.R

object SettingsMainData {
    val groups = listOf(
        SettingsGroupData(
            R.string.company_name,
            listOf(
                SettingsRowData(type = SettingsRowType.PROFILE),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    title_id = R.string.settings_account_settings,
                    url = Uri.parse(appSettingsURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    title_id = R.string.settings_connected_apps,
                    url = Uri.parse(appConnectionsURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    title_id = R.string.settings_invite_friends,
                    url = Uri.parse(appReferralURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_general,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    title_id = R.string.settings_default_browser,
                    url = Uri.parse(appPrivacyURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    title_id = R.string.settings_show_search_search_suggestions,
                    togglePreferenceKey = SettingsToggle.SHOW_SEARCH_SUGGESTIONS.key
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    title_id = R.string.settings_block_pop_up_windows,
                    togglePreferenceKey = SettingsToggle.BLOCK_POP_UP_WINDOWS.key
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    title_id = R.string.settings_open_copied_links,
                    togglePreferenceKey = SettingsToggle.OFFER_TO_OPEN_COPIED_LINKS.key
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    title_id = R.string.settings_show_link_previews,
                    togglePreferenceKey = SettingsToggle.SHOW_LINK_PREVIEWS.key
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_privacy,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    title_id = R.string.settings_clear_browsing_data,
                    url = Uri.parse(appPrivacyURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    title_id = R.string.settings_close_incognito_tabs,
                    togglePreferenceKey = SettingsToggle.CLOSE_INCOGNITO_TABS.key
                ),
                SettingsRowData(
                    type = SettingsRowType.TOGGLE,
                    title_id = R.string.settings_tracking_protection,
                    togglePreferenceKey = SettingsToggle.TRACKING_PROTECTION.key
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    title_id = R.string.settings_privacy_policy,
                    url = Uri.parse(appPrivacyURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_support,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    title_id = R.string.settings_welcome_tours,
                    url = Uri.parse(appWelcomeToursURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    title_id = R.string.settings_help_center,
                    url = Uri.parse(appHelpCenterURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_about,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.LABEL,
                    title_id = R.string.settings_neeva_browser_version
                ),
                SettingsRowData(
                    type = SettingsRowType.NAVIGATION,
                    title_id = R.string.settings_licenses,
                    url = Uri.parse(appTermsURL)
                ),
                SettingsRowData(
                    type = SettingsRowType.LINK,
                    title_id = R.string.settings_terms,
                    url = Uri.parse(appTermsURL)
                ),
            )
        )
    )
}

data class SettingsGroupData(
    val title_id: Int,
    val rows: List<SettingsRowData>
)

data class SettingsRowData(
    val type: SettingsRowType,
    val title_id: Int? = null,
    val url: Uri? = null,
    val togglePreferenceKey: String? = null,
)

enum class SettingsRowType {
    LABEL,
    LINK,
    TOGGLE,
    NAVIGATION,
    PROFILE
}

enum class SettingsToggle(
    val key: String,
    val defaultValue: Boolean
) {
    SHOW_SEARCH_SUGGESTIONS("SHOW_SEARCH_SUGGESTIONS", true),
    BLOCK_POP_UP_WINDOWS("BLOCK_POP_UP_WINDOWS", true),
    OFFER_TO_OPEN_COPIED_LINKS("OFFER_TO_OPEN_COPIED_LINKS", false),
    SHOW_LINK_PREVIEWS("SHOW_LINK_PREVIEWS", false),
    CLOSE_INCOGNITO_TABS("CLOSE_INCOGNITO_TABS", false),
    TRACKING_PROTECTION("TRACKING_PROTECTION", true)
}
