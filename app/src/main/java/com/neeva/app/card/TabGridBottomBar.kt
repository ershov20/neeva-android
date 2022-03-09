package com.neeva.app.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.R
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.getClickableAlpha

@Composable
fun TabGridBottomBar(
    currentBrowser: BrowserWrapper,
    cardGridModel: CardGridModel
) {
    val hasNoTabs = currentBrowser.hasNoTabsFlow().collectAsState(false)

    TabGridBottomBar(
        isDeleteEnabled = !hasNoTabs.value,
        isCloseButtonEnabled = !hasNoTabs.value,
        onCloseAllTabs = cardGridModel::closeAllTabs,
        onOpenLazyTab = cardGridModel::openLazyTab,
        onDone = cardGridModel::showBrowser
    )
}

@Composable
fun TabGridBottomBar(
    isDeleteEnabled: Boolean,
    isCloseButtonEnabled: Boolean,
    onCloseAllTabs: () -> Unit,
    onOpenLazyTab: () -> Unit,
    onDone: () -> Unit
) {
    BottomAppBar(
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(modifier = Modifier.weight(1f)) {
            IconButton(
                enabled = isDeleteEnabled,
                onClick = onCloseAllTabs,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                val deleteButtonAlpha = getClickableAlpha(isDeleteEnabled)
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.close_all_content_description),
                    tint = LocalContentColor.current.copy(
                        alpha = deleteButtonAlpha
                    )
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            IconButton(
                onClick = onOpenLazyTab,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_tab_content_description),
                    tint = LocalContentColor.current
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            val closeButtonAlpha = getClickableAlpha(isCloseButtonEnabled)
            TextButton(
                onClick = onDone,
                enabled = isCloseButtonEnabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LocalContentColor.current.copy(
                        alpha = closeButtonAlpha
                    )
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    modifier = Modifier.padding(Dimensions.PADDING_SMALL),
                    text = stringResource(id = R.string.done),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

class TabGridBottomBarPreviews :
    BooleanPreviewParameterProvider<TabGridBottomBarPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val isDeleteEnabled: Boolean,
        val isCloseEnabled: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isDeleteEnabled = booleanArray[1],
        isCloseEnabled = booleanArray[2]
    )

    @Preview("1x", locale = "en")
    @Preview("2x", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x", locale = "he")
    @Preview("RTL, 2x", locale = "he", fontScale = 2.0f)
    @Composable
    fun DefaultPreview(@PreviewParameter(TabGridBottomBarPreviews::class) params: Params) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            TabGridBottomBar(
                isDeleteEnabled = params.isDeleteEnabled,
                isCloseButtonEnabled = params.isCloseEnabled,
                onCloseAllTabs = {},
                onOpenLazyTab = {},
                onDone = {}
            )
        }
    }
}
