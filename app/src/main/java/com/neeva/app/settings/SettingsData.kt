package com.neeva.app.settings

import android.net.Uri
import com.neeva.app.NeevaBrowser
import com.neeva.app.NeevaConstants.appConnectionsURL
import com.neeva.app.NeevaConstants.appHelpCenterURL
import com.neeva.app.NeevaConstants.appPrivacyURL
import com.neeva.app.NeevaConstants.appReferralURL
import com.neeva.app.NeevaConstants.appSettingsURL
import com.neeva.app.NeevaConstants.appTermsURL
import com.neeva.app.NeevaConstants.appWelcomeToursURL

object SettingsMainData {
    // TODO(dan.alcantara): These strings should be in strings.xml, not hard-coded here.
    val groups = listOf(
        SettingsGroupData(
            "Neeva",
            listOf(
                SettingsRowData(
                    "Account Settings",
                    SettingsRowType.LINK,
                    Uri.parse(appSettingsURL)
                ),
                SettingsRowData(
                    "Connected Apps",
                    SettingsRowType.LINK,
                    Uri.parse(appConnectionsURL)
                ),
                SettingsRowData(
                    "Invite your friends!",
                    SettingsRowType.LINK,
                    Uri.parse(appReferralURL)
                ),
            )
        ),
        SettingsGroupData(
            "Privacy",
            listOf(
                SettingsRowData(
                    "Privacy Policy",
                    SettingsRowType.LINK,
                    Uri.parse(appPrivacyURL)
                ),
            )
        ),
        SettingsGroupData(
            "Support",
            listOf(
                SettingsRowData(
                    "Welcome Tours",
                    SettingsRowType.LINK,
                    Uri.parse(appWelcomeToursURL)
                ),
                SettingsRowData("Help Center", SettingsRowType.LINK, Uri.parse(appHelpCenterURL)),
            )
        ),
        SettingsGroupData(
            "About",
            listOf(
                SettingsRowData(
                    "Neeva Browser ${NeevaBrowser.versionString}",
                    SettingsRowType.LABEL
                ),
                SettingsRowData("Terms", SettingsRowType.LINK, Uri.parse(appTermsURL)),
            )
        )
    )
}

data class SettingsGroupData(
    val label: String,
    val rows: List<SettingsRowData>
)

data class SettingsRowData(
    val title: String,
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
