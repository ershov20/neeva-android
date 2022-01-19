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
                SettingsRowData(
                    title_id = R.string.settings_account_email_placeholder,
                    type = SettingsRowType.NAVIGATION,
                    url = Uri.parse(appSettingsURL)
                ),
                SettingsRowData(
                    title_id = R.string.settings_account_settings,
                    type = SettingsRowType.LINK,
                    url = Uri.parse(appSettingsURL)
                ),
                SettingsRowData(
                    title_id = R.string.settings_connected_apps,
                    type = SettingsRowType.LINK,
                    url = Uri.parse(appConnectionsURL)
                ),
                SettingsRowData(
                    title_id = R.string.settings_invite_friends,
                    type = SettingsRowType.LINK,
                    url = Uri.parse(appReferralURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_general,
            listOf(
                SettingsRowData(
                    title_id = R.string.settings_default_browser,
                    type = SettingsRowType.NAVIGATION,
                    url = Uri.parse(appPrivacyURL)
                ),
                SettingsRowData(
                    title_id = R.string.settings_show_search_search_suggestions,
                    type = SettingsRowType.TOGGLE,
                    togglePreferenceKey = SettingsToggle.SHOW_SEARCH_SUGGESTIONS.key
                ),
                SettingsRowData(
                    title_id = R.string.settings_block_pop_up_windows,
                    type = SettingsRowType.TOGGLE,
                    togglePreferenceKey = SettingsToggle.BLOCK_POP_UP_WINDOWS.key
                ),
                SettingsRowData(
                    title_id = R.string.settings_open_copied_links,
                    type = SettingsRowType.TOGGLE,
                    togglePreferenceKey = SettingsToggle.OFFER_TO_OPEN_COPIED_LINKS.key
                ),
                SettingsRowData(
                    title_id = R.string.settings_show_link_previews,
                    type = SettingsRowType.TOGGLE,
                    togglePreferenceKey = SettingsToggle.SHOW_LINK_PREVIEWS.key
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_privacy,
            listOf(
                SettingsRowData(
                    title_id = R.string.settings_clear_browsing_data,
                    type = SettingsRowType.NAVIGATION,
                    url = Uri.parse(appPrivacyURL)
                ),
                SettingsRowData(
                    title_id = R.string.settings_close_incognito_tabs,
                    type = SettingsRowType.TOGGLE,
                    togglePreferenceKey = SettingsToggle.CLOSE_INCOGNITO_TABS.key
                ),
                SettingsRowData(
                    title_id = R.string.settings_tracking_protection,
                    type = SettingsRowType.TOGGLE,
                    togglePreferenceKey = SettingsToggle.TRACKING_PROTECTION.key
                ),
                SettingsRowData(
                    title_id = R.string.settings_privacy_policy,
                    type = SettingsRowType.LINK,
                    url = Uri.parse(appPrivacyURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_support,
            listOf(
                SettingsRowData(
                    title_id = R.string.settings_welcome_tours,
                    type = SettingsRowType.LINK,
                    url = Uri.parse(appWelcomeToursURL)
                ),
                SettingsRowData(
                    title_id = R.string.settings_help_center,
                    type = SettingsRowType.LINK,
                    url = Uri.parse(appHelpCenterURL)
                ),
            )
        ),
        SettingsGroupData(
            R.string.settings_about,
            listOf(
                SettingsRowData(
                    title_id = R.string.settings_neeva_browser_version,
                    type = SettingsRowType.LABEL
                ),
                SettingsRowData(
                    title_id = R.string.settings_licenses,
                    type = SettingsRowType.NAVIGATION,
                    url = Uri.parse(appTermsURL)
                ),
                SettingsRowData(
                    title_id = R.string.settings_terms,
                    type = SettingsRowType.LINK,
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
    val title_id: Int,
    val type: SettingsRowType,
    val url: Uri? = null,
    val togglePreferenceKey: String? = null,
)

enum class SettingsRowType {
    LABEL,
    LINK,
    TOGGLE,
    NAVIGATION
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
