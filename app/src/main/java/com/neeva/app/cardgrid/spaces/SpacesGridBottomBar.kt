package com.neeva.app.cardgrid.spaces

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.cardgrid.CardGridBottomBar
import com.neeva.app.ui.OneBooleanPreviewContainer

@Composable
fun SpacesGridBottomBar(isDoneEnabled: Boolean, onDone: () -> Unit) {
    CardGridBottomBar(
        isDoneEnabled = isDoneEnabled,
        onDone = onDone
    )
}

@Preview("LTR, 1x", locale = "en")
@Preview("LTR, 2x", locale = "en", fontScale = 2.0f)
@Preview("RTL, 1x", locale = "he")
@Composable
fun SpacesGridBottomBarPreview() {
    OneBooleanPreviewContainer { isDoneEnabled ->
        SpacesGridBottomBar(
            isDoneEnabled = isDoneEnabled,
            onDone = {}
        )
    }
}
