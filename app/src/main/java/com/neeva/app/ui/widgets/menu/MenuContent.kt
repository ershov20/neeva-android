// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.overflowmenu.OverflowMenuData
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams

@Composable
fun MenuContent(
    menuRows: Collection<MenuRowItem>,
    menuIconItems: Collection<MenuIconItemData> = emptyList(),
    onMenuItem: (id: Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            menuIconItems.forEach { data ->
                RowActionIconButton(
                    RowActionIconParams(
                        onTapAction = { onMenuItem(data.id) },
                        actionType = data.action,
                        contentDescription = stringResource(id = data.labelId),
                        enabled = data.enabled
                    )
                )
            }
        }

        menuRows.forEach {
            it.Composed(onClick = onMenuItem)
        }
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Preview(locale = "en", fontScale = 2.0f)
@Preview(locale = "he", fontScale = 2.0f)
@Composable
fun MenuContent_Light() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        Surface {
            MenuContent(
                menuRows = listOf(
                    MenuHeader(
                        stringResource(R.string.debug_long_string_primary),
                        stringResource(R.string.debug_long_url)
                    ),
                    MenuSeparator,
                    MenuAction(R.string.menu_open_in_new_tab),
                    MenuAction(R.string.menu_open_in_new_incognito_tab),
                    MenuAction(R.string.menu_copy_link_address),
                    MenuAction(R.string.menu_copy_link_text),
                    MenuAction(R.string.menu_download_link)
                )
            ) {}
        }
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Preview(locale = "en", fontScale = 2.0f)
@Preview(locale = "he", fontScale = 2.0f)
@Composable
fun MenuContent_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        Surface {
            MenuContent(
                menuRows = listOf(
                    MenuHeader(
                        stringResource(R.string.debug_long_string_primary),
                        stringResource(R.string.debug_long_url)
                    ),
                    MenuSeparator,
                    MenuAction(R.string.menu_open_in_new_tab),
                    MenuAction(R.string.menu_open_in_new_incognito_tab),
                    MenuAction(R.string.menu_copy_link_address),
                    MenuAction(R.string.menu_copy_link_text),
                    MenuAction(R.string.menu_download_link)
                )
            ) {}
        }
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Preview(locale = "en", fontScale = 2.0f)
@Preview(locale = "he", fontScale = 2.0f)
@Composable
fun MenuContent_OnlyTitle() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        Surface {
            MenuContent(
                menuRows = listOf(
                    MenuHeader(stringResource(R.string.debug_long_string_primary)),
                    MenuSeparator,
                    MenuAction(R.string.menu_open_in_new_tab),
                    MenuAction(R.string.menu_open_in_new_incognito_tab),
                    MenuAction(R.string.menu_copy_link_address),
                    MenuAction(R.string.menu_copy_link_text),
                    MenuAction(R.string.menu_download_link)
                )
            ) {}
        }
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Preview(locale = "en", fontScale = 2.0f)
@Preview(locale = "he", fontScale = 2.0f)
@Composable
fun MenuContent_OnlyUrl() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        Surface {
            MenuContent(
                menuRows = listOf(
                    MenuHeader(null, stringResource(R.string.debug_long_url)),
                    MenuSeparator,
                    MenuAction(R.string.menu_open_in_new_tab),
                    MenuAction(R.string.menu_open_in_new_incognito_tab),
                    MenuAction(R.string.menu_copy_link_address),
                    MenuAction(R.string.menu_copy_link_text),
                    MenuAction(R.string.menu_download_link)
                )
            ) {}
        }
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Preview(locale = "en", fontScale = 2.0f)
@Preview(locale = "he", fontScale = 2.0f)
@Composable
fun MenuContent_OverflowMenu_Light() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        Surface {
            MenuContent(
                menuRows = OverflowMenuData().rowItems
            ) {}
        }
    }
}

@Preview(locale = "en", fontScale = 1.0f)
@Preview(locale = "en", fontScale = 2.0f)
@Preview(locale = "he", fontScale = 2.0f)
@Composable
fun MenuContent_OverflowMenu_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        Surface {
            MenuContent(
                menuRows = OverflowMenuData().rowItems
            ) {}
        }
    }
}
