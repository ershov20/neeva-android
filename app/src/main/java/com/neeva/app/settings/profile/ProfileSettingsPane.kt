package com.neeva.app.settings.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.settings.ProfileRow
import com.neeva.app.settings.SettingsTopAppBar
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsButtonRow
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserData

@Composable
fun ProfileSettingsPane(
    onBackPressed: () -> Unit,
    signUserOut: () -> Unit,
    neevaUserData: NeevaUserData
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        SettingsTopAppBar(
            title = neevaUserData.displayName ?: "",
            onBackPressed = onBackPressed
        )

        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .wrapContentHeight(align = Alignment.Bottom),
            ) {
                Text(
                    text = stringResource(R.string.settings_signed_into_neeva_with),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }

            val rowModifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface)

            ProfileRow(
                primaryLabel = neevaUserData.ssoProvider.name,
                secondaryLabel = neevaUserData.email,
                pictureUrl = neevaUserData.pictureURL,
                modifier = rowModifier
            )
            SettingsButtonRow(
                title = stringResource(R.string.settings_sign_out),
                onClick = {
                    signUserOut()
                },
                modifier = rowModifier
            )
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
            onBackPressed = {},
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
            onBackPressed = {},
            signUserOut = {},
            neevaUserData = NeevaUserData(
                displayName = "Jehan Kobe Chang",
                email = "jehanc@uci.edu",
                ssoProvider = NeevaUser.SSOProvider.GOOGLE
            )
        )
    }
}
