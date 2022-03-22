package com.neeva.app.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import coil.annotation.ExperimentalCoilApi
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions

/**
 * Base skeleton for everything that can be displayed as a suggestion in UI.  Callers must provide
 * a Composable [mainContent] that applies the provided modifier in order for it to properly take
 * the fully available width in the row.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun BaseSuggestionRow(
    iconParams: SuggestionRowIconParams,
    onTapRow: () -> Unit,
    onTapRowContentDescription: String? = null,
    actionParams: SuggestionRowActionParams? = null,
    mainContent: @Composable () -> Unit
) {
    BaseRowLayout(
        onTapRow = onTapRow,
        onTapRowContentDescription = onTapRowContentDescription,
        startComposable = {
            Box(modifier = Modifier.padding(start = Dimensions.PADDING_SMALL)) {
                SuggestionRowIcon(iconParams)
            }
        },
        endComposable = actionParams?.let { { SuggestionRowAction(it) } },
        mainContent = mainContent
    )
}

@Preview("Globe favicon, LTR, 1x", locale = "en")
@Preview("Globe favicon, LTR, 2x", locale = "en", fontScale = 2.0f)
@Preview("Globe favicon, RTL, 1x", locale = "he")
@Composable
fun BaseSuggestionRow_Preview() {
    OneBooleanPreviewContainer { showAction ->
        BaseSuggestionRow(
            onTapRow = {},
            actionParams = SuggestionRowActionParams(
                onTapAction = {},
                actionType = SuggestionRowActionParams.ActionType.REFINE
            ).takeIf { showAction },
            iconParams = SuggestionRowIconParams()
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Magenta)
                    .fillMaxWidth()
                    .height(Dimensions.SIZE_TOUCH_TARGET)
            )
        }
    }
}
