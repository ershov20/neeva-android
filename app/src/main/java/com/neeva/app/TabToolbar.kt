package com.neeva.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme

data class TabToolbarModel(
    val onNeevaMenu: () -> Unit = {},
    val onAddToSpace: () -> Unit = {},
    val onTabSwitcher: () -> Unit = {},
    val goBack: () -> Unit = {},
    val goForward: () -> Unit = {}
)

@Composable
fun TabToolbar(
    model: TabToolbarModel,
    activeTabModel: ActiveTabModel,
    modifier: Modifier
) {
    val navigationInfo by activeTabModel.navigationInfoFlow.collectAsState()
    TabToolbar(
        model = model,
        canGoBackward = navigationInfo.canGoBackward,
        canGoForward = navigationInfo.canGoForward,
        modifier = modifier
    )
}

@Composable
fun TabToolbar(
    model: TabToolbarModel,
    canGoBackward: Boolean,
    canGoForward: Boolean,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        BottomAppBar(
            backgroundColor = MaterialTheme.colorScheme.background,
            modifier = modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.bottom_toolbar_height))
        ) {
            val backAlpha = if (canGoBackward) 1.0f else 0.25f
            IconButton(
                enabled = canGoBackward,
                onClick = model.goBack,
                modifier = Modifier.weight(1.0f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = stringResource(id = R.string.toolbar_go_back),
                    tint = LocalContentColor.current.copy(alpha = backAlpha)
                )
            }

            val forwardAlpha = if (canGoForward) 1.0f else 0.25f
            IconButton(
                enabled = canGoForward,
                onClick = model.goForward,
                modifier = Modifier.weight(1.0f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_arrow_forward_24),
                    contentDescription = stringResource(id = R.string.toolbar_go_forward),
                    tint = LocalContentColor.current.copy(alpha = forwardAlpha)
                )
            }

            IconButton(
                onClick = model.onNeevaMenu,
                modifier = Modifier.weight(1.0f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_neeva_logo_vector),
                    contentDescription = stringResource(id = R.string.toolbar_neeva_menu),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                enabled = true,
                onClick = model.onAddToSpace,
                modifier = Modifier.weight(1.0f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_bookmark_border_24),
                    contentDescription = stringResource(R.string.toolbar_save_to_space),
                )
            }

            IconButton(
                enabled = true,
                onClick = model.onTabSwitcher,
                modifier = Modifier.weight(1.0f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_grid_view_24),
                    contentDescription = stringResource(R.string.toolbar_tab_switcher)
                )
            }
        }
    }
}

class TabToolbarPreviews : BooleanPreviewParameterProvider<TabToolbarPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val backEnabled: Boolean,
        val forwardEnabled: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        backEnabled = booleanArray[1],
        forwardEnabled = booleanArray[2]
    )

    @Preview("1x scale", locale = "en")
    @Preview("2x scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x scale", locale = "he")
    @Preview("RTL, 2x scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun Default(@PreviewParameter(TabToolbarPreviews::class) params: Params) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            TabToolbar(
                model = TabToolbarModel(),
                canGoBackward = params.backEnabled,
                canGoForward = params.forwardEnabled
            )
        }
    }
}
