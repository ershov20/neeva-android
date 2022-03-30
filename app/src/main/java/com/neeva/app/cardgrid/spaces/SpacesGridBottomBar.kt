package com.neeva.app.cardgrid.spaces

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.cardgrid.CardGridBottomBar
import com.neeva.app.ui.OneBooleanPreviewContainer

@Composable
fun SpacesGridBottomBar(
    isDoneEnabled: Boolean,
    onNavigateToSpacesWebsite: () -> Unit,
    onDone: () -> Unit
) {
    CardGridBottomBar(
        startComposable = {
            IconButton(onClick = onNavigateToSpacesWebsite) {
                Icon(
                    painter = painterResource(R.drawable.ic_public_black_24),
                    contentDescription = stringResource(R.string.spaces)
                )
            }
        },
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
            onNavigateToSpacesWebsite = {},
            onDone = {}
        )
    }
}
