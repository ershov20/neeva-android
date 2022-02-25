package com.neeva.app.settings.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.settings.SettingsViewModel
import com.neeva.app.settings.getFakeSettingsViewModel
import com.neeva.app.settings.sharedComposables.SettingsGroupView
import com.neeva.app.ui.theme.FullScreenDialogTopBar
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserData

@Composable
fun ProfileSettingsPane(
    settingsViewModel: SettingsViewModel,
    signUserOut: () -> Unit,
    neevaUserData: NeevaUserData
) {
    val buttonClickMap = mapOf(
        R.string.settings_sign_out to signUserOut
    )
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .fillMaxSize(),
    ) {
        FullScreenDialogTopBar(
            title = neevaUserData.displayName ?: "",
            onBackPressed = settingsViewModel::onBackPressed
        )

        LazyColumn(
            modifier = Modifier
                .weight(1.0f)
        ) {
            ProfileSettingsPaneData.data.forEach { groupData ->
                item {
                    SettingsGroupView(
                        settingsViewModel,
                        groupData,
                        buttonOnClicks = buttonClickMap
                    )
                }
            }
        }
    }
}

@Preview(name = "Settings Profile, 1x font size", locale = "en")
@Preview(name = "Settings Profile, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Profile, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Profile, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsProfile_Preview() {
    NeevaTheme {
        ProfileSettingsPane(
            settingsViewModel = getFakeSettingsViewModel(),
            signUserOut = {},
            neevaUserData = NeevaUserData(
                displayName = "Jehan Kobe Chang",
                email = "jehanc@uci.edu",
                ssoProvider = NeevaUser.SSOProvider.GOOGLE
            )
        )
    }
}

@Preview(name = "Settings Profile Dark, 1x font size", locale = "en")
@Preview(name = "Settings Profile Dark, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Settings Profile Dark, RTL, 1x font size", locale = "he")
@Preview(name = "Settings Profile Dark, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsProfile_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        ProfileSettingsPane(
            settingsViewModel = getFakeSettingsViewModel(),
            signUserOut = {},
            neevaUserData = NeevaUserData(
                displayName = "Jehan Kobe Chang",
                email = "jehanc@uci.edu",
                ssoProvider = NeevaUser.SSOProvider.GOOGLE
            )
        )
    }
}
