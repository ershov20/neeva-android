package com.neeva.app.ui

import androidx.compose.material.IconButton
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.zIndex
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun FullScreenDialogTopBar(
    title: String,
    onBackPressed: () -> Unit,
    buttonTitle: String? = null,
    buttonPressed: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge
            )
        },
        backgroundColor = MaterialTheme.colorScheme.surface,
        navigationIcon = {
            IconButton(
                onClick = { onBackPressed() }
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.close),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (buttonTitle != null && buttonPressed != null) {
                TextButton(onClick = { buttonPressed() }) {
                    Text(
                        text = buttonTitle,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        modifier = Modifier.zIndex(1.0f)
    )
}

class FullScreenDialogTopBarPreviews :
    BooleanPreviewParameterProvider<FullScreenDialogTopBarPreviews.Params>(2) {
    data class Params(
        val darkTheme: Boolean,
        val addButton: Boolean,
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        addButton = booleanArray[1]
    )

    @Preview("1x font scale", locale = "en")
    @Preview("2x font scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x font scale", locale = "he")
    @Preview("RTL, 2x font scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun FullScreenDialogTopBarPreview(
        @PreviewParameter(FullScreenDialogTopBarPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            if (params.addButton) {
                FullScreenDialogTopBar(
                    title = stringResource(R.string.debug_long_string_primary),
                    onBackPressed = {},
                    buttonTitle = stringResource(R.string.debug_short_action),
                    buttonPressed = {}
                )
            } else {
                FullScreenDialogTopBar(
                    title = stringResource(R.string.debug_long_string_primary),
                    onBackPressed = {}
                )
            }
        }
    }
}
