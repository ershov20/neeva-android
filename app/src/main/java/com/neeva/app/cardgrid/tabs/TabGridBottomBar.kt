package com.neeva.app.cardgrid.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.cardgrid.CardGridBottomBar
import com.neeva.app.ui.ConfirmationAlertDialog
import com.neeva.app.ui.TwoBooleanPreviewContainer

@Composable
fun TabGridBottomBar(
    isIncognito: Boolean,
    hasNoTabs: Boolean,
    requireConfirmationWhenCloseAllTabs: Boolean = true,
    onCloseAllTabs: () -> Unit,
    onOpenLazyTab: () -> Unit,
    onDone: () -> Unit
) {
    val showCloseAllTabsDialog = remember { mutableStateOf(false) }

    CardGridBottomBar(
        startComposable = {
            IconButton(
                enabled = !hasNoTabs,
                onClick = {
                    if (requireConfirmationWhenCloseAllTabs) {
                        showCloseAllTabsDialog.value = true
                    } else {
                        onCloseAllTabs()
                    }
                },
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.close_all_content_description)
                )
            }
        },
        centerComposable = {
            IconButton(onClick = onOpenLazyTab) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_tab_content_description)
                )
            }
        },
        isDoneEnabled = !hasNoTabs,
        onDone = onDone
    )

    if (showCloseAllTabsDialog.value) {
        val dismissLambda = { showCloseAllTabsDialog.value = false }
        val titleId = if (isIncognito) {
            R.string.close_all_incognito_tabs
        } else {
            R.string.close_all_regular_tabs
        }
        ConfirmationAlertDialog(
            title = stringResource(titleId),
            onDismiss = dismissLambda,
            onConfirm = {
                onCloseAllTabs()
                dismissLambda()
            }
        )
    }
}

@Preview("LTR, 1x", locale = "en")
@Preview("LTR, 2x", locale = "en", fontScale = 2.0f)
@Preview("RTL, 1x", locale = "he")
@Composable
fun DefaultPreview() {
    TwoBooleanPreviewContainer { isIncognito, hasNoTabs ->
        TabGridBottomBar(
            isIncognito = isIncognito,
            hasNoTabs = hasNoTabs,
            onCloseAllTabs = {},
            onOpenLazyTab = {},
            onDone = {}
        )
    }
}
