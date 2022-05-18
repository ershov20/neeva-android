package com.neeva.app.settings.profile

import android.net.Uri
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.settings.SettingsGroupData
import com.neeva.app.settings.SettingsPaneDataInterface
import com.neeva.app.settings.SettingsRowData
import com.neeva.app.settings.SettingsRowType

class ProfileSettingsPaneData(neevaConstants: NeevaConstants) : SettingsPaneDataInterface {
    // For Profile Settings, the TopAppBar Title is the user's display name.
    override val topAppBarTitleResId: Int = -1
    override val shouldShowUserName: Boolean = true
    override val data = listOf(
        SettingsGroupData(
            R.string.settings_signed_into_neeva_with,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.PROFILE,
                    primaryLabelId = R.string.settings_sign_in_to_join_neeva,
                    showSSOProviderAsPrimaryLabel = true
                )
            )
        ),
        SettingsGroupData(
            R.string.settings_membership_status,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.SUBSCRIPTION,
                    primaryLabelId = R.string.settings_membership_status,
                    url = Uri.parse(neevaConstants.appMembershipURL)
                )
            )
        ),
        SettingsGroupData(
            R.string.settings_sign_out,
            listOf(
                SettingsRowData(
                    type = SettingsRowType.BUTTON,
                    primaryLabelId = R.string.settings_sign_out
                )
            )
        )
    )
}
