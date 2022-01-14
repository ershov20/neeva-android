package com.neeva.app.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.md_theme_light_shadow
import com.neeva.app.widgets.Button

@Composable
fun ModeSwitcher(
    selectedScreen: SelectedScreen,
    onSwitchScreen: (SelectedScreen) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .height(48.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val regularBackground: Color
            val regularForeground: Color
            val incognitoBackground: Color
            val incognitoForeground: Color
            if (selectedScreen == SelectedScreen.INCOGNITO_TABS) {
                incognitoBackground = md_theme_light_shadow
                incognitoForeground = MaterialTheme.colorScheme.onPrimary
                regularBackground = MaterialTheme.colorScheme.surfaceVariant
                regularForeground = MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                incognitoBackground = MaterialTheme.colorScheme.surfaceVariant
                incognitoForeground = MaterialTheme.colorScheme.onSurfaceVariant
                regularBackground = MaterialTheme.colorScheme.primary
                regularForeground = MaterialTheme.colorScheme.onPrimary
            }

            Button(
                enabled = true,
                resID = R.drawable.ic_incognito,
                contentDescription = stringResource(R.string.incognito),
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color = incognitoBackground, shape = RoundedCornerShape(24.dp)),
                colorTint = incognitoForeground
            ) {
                onSwitchScreen(SelectedScreen.INCOGNITO_TABS)
            }

            Button(
                enabled = true,
                resID = R.drawable.ic_tabs,
                contentDescription = stringResource(id = R.string.tabs),
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color = regularBackground, shape = RoundedCornerShape(24.dp)),
                colorTint = regularForeground
            ) {
                onSwitchScreen(SelectedScreen.REGULAR_TABS)
            }
        }
    }
}

class ModeSwitcherPreviews : BooleanPreviewParameterProvider<ModeSwitcherPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val selectedScreen: SelectedScreen
    )

    override fun createParams(booleanArray: BooleanArray): Params {
        return Params(
            darkTheme = booleanArray[0],
            selectedScreen = when (booleanArray[1]) {
                false -> SelectedScreen.REGULAR_TABS
                else -> SelectedScreen.INCOGNITO_TABS
            }
        )
    }

    @Preview("1x", locale = "en")
    @Preview("2x", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x", locale = "he")
    @Preview("RTL, 2x", locale = "he", fontScale = 2.0f)
    @Composable
    fun DefaultPreview(
        @PreviewParameter(ModeSwitcherPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                ModeSwitcher(params.selectedScreen) {}
            }
        }
    }
}
