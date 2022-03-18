package com.neeva.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import com.neeva.app.R

@Composable
fun FullScreenDialogTopBar(
    title: String,
    onBackPressed: () -> Unit,
    buttonTitle: String? = null,
    buttonPressed: (() -> Unit)? = null
) {
    SmallTopAppBar(
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(MaterialTheme.colorScheme.surface),
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

@Preview("FullScreenDialogTopBar, 1x font scale", locale = "en")
@Preview("FullScreenDialogTopBar, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("FullScreenDialogTopBar, RTL, 1x font scale", locale = "he")
@Preview("FullScreenDialogTopBar, RTL, 2x font scale", locale = "he", fontScale = 2.0f)
@Composable
private fun FullScreenDialogTopBarPreview() {
    OneBooleanPreviewContainer { addButton ->
        FullScreenDialogTopBar(
            title = stringResource(R.string.debug_long_string_primary),
            onBackPressed = {},
            buttonTitle = stringResource(R.string.debug_short_action).takeIf { addButton },
            buttonPressed = {}.takeIf { addButton }
        )
    }
}
