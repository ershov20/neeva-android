// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing.toolbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserToolbarModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.findinpage.FindInPageModel
import com.neeva.app.browsing.findinpage.FindInPageToolbar
import com.neeva.app.browsing.findinpage.PreviewFindInPageModel
import com.neeva.app.browsing.urlbar.URLBar
import com.neeva.app.browsing.urlbar.URLBarModelState
import com.neeva.app.contentfilter.ui.icon.TrackingProtectionButton
import com.neeva.app.contentfilter.ui.popover.ContentFilterOnboardingContent
import com.neeva.app.contentfilter.ui.popover.ContentFilterPopover
import com.neeva.app.contentfilter.ui.popover.ContentFilterPopoverContent
import com.neeva.app.contentfilter.ui.popover.ContentFilterPopoverModel
import com.neeva.app.contentfilter.ui.popover.PreviewContentFilterPopoverModel
import com.neeva.app.contentfilter.ui.popover.rememberContentFilterPopoverModel
import com.neeva.app.neevascope.NeevaScopeTooltip
import com.neeva.app.overflowmenu.OverflowMenu
import com.neeva.app.overflowmenu.OverflowMenuItemId
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

@Composable
fun BrowserToolbarContainer(topOffset: Float) {
    val browserToolbarModel = LocalBrowserToolbarModel.current
    val findInPageModel = LocalBrowserWrapper.current.findInPageModel
    val urlFlow = LocalBrowserWrapper.current.activeTabModel.urlFlow

    val topOffsetDp = with(LocalDensity.current) { topOffset.toDp() }

    val appNavModel = LocalAppNavModel.current

    val contentFilterPopoverModel = rememberContentFilterPopoverModel(
        appNavModel = appNavModel,
        reloadTab = browserToolbarModel::reloadAfterContentFilterAllowListUpdate,
        contentFilterModel = browserToolbarModel.contentFilterModel,
        urlFlow = urlFlow
    )

    BrowserToolbar(
        findInPageModel = findInPageModel,
        contentFilterPopoverModel = contentFilterPopoverModel,
        modifier = Modifier
            .offset(y = topOffsetDp)
            .background(MaterialTheme.colorScheme.background)
    )

    val isEditing = browserToolbarModel.urlBarModel.stateFlow.collectAsState()
        .value.isEditing
    BackHandler(enabled = isEditing) {
        // This is the main mechanism for setting isEditing to false:
        browserToolbarModel.urlBarModel.clearFocus()
    }
}

@Composable
fun BrowserToolbarContentFilterPopover(
    contentFilterPopoverModel: ContentFilterPopoverModel
) {
    val sharedPreferencesModel = LocalSharedPreferencesModel.current

    val didShowAdBlockOnboarding = SharedPrefFolder.FirstRun.DidShowAdBlockOnboarding
        .getFlow(sharedPreferencesModel).collectAsState()

    if ((
        contentFilterPopoverModel.cookieNoticeBlocked.collectAsState().value ||
            contentFilterPopoverModel.easyListRuleBlocked.collectAsState().value
        ) &&
        !didShowAdBlockOnboarding.value
    ) {
        contentFilterPopoverModel.openOnboardingPopover()
    }

    if (contentFilterPopoverModel.shouldShowOnboardingPopover.value) {
        ContentFilterPopover(
            onDismissRequest = contentFilterPopoverModel::dismissOnboardingPopover,
            content = {
                ContentFilterOnboardingContent(
                    contentFilterPopoverModel = contentFilterPopoverModel
                )
            }
        )
    } else if (contentFilterPopoverModel.popoverVisible.value) {
        // The popover will be aligned to the top left of the enclosing Box.
        ContentFilterPopover(
            onDismissRequest = contentFilterPopoverModel::dismissPopover,
            content = {
                ContentFilterPopoverContent(
                    contentFilterPopoverModel = contentFilterPopoverModel
                )
            }
        )
    }
}

@Composable
fun BrowserToolbar(
    findInPageModel: FindInPageModel,
    contentFilterPopoverModel: ContentFilterPopoverModel,
    modifier: Modifier = Modifier,
) {
    val browserToolbarModel = LocalBrowserToolbarModel.current
    val isEditing = browserToolbarModel.urlBarModel.stateFlow.collectAsState()
        .value.isEditing
    val findInPageInfo = findInPageModel.findInPageInfoFlow.collectAsState().value
    val navigationInfoFlow = browserToolbarModel.navigationInfoFlow.collectAsState()

    val showRedditDot = remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.top_toolbar_height))
    ) {
        if (findInPageInfo.text != null) {
            FindInPageToolbar(
                findInPageInfo = findInPageInfo,
                onUpdateQuery = { findInPageModel.updateFindInPageQuery(it) },
                onScrollToResult = { forward -> findInPageModel.scrollToFindInPageResult(forward) }
            )
        } else {
            val isDesktopUserAgentEnabled = navigationInfoFlow.value.desktopUserAgentEnabled
            val isForwardEnabled = navigationInfoFlow.value.canGoForward

            val overflowMenuData = remember(
                browserToolbarModel.useSingleBrowserToolbar,
                isForwardEnabled,
                browserToolbarModel.isUpdateAvailable,
                isDesktopUserAgentEnabled
            ) {
                createBrowserOverflowMenuData(
                    isForwardEnabled = isForwardEnabled,
                    isUpdateAvailableVisible = browserToolbarModel.isUpdateAvailable,
                    isDesktopUserAgentEnabled = isDesktopUserAgentEnabled
                )
            }

            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = !isEditing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (browserToolbarModel.useSingleBrowserToolbar) {
                                BackButton()
                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                                ShareButton()
                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                            }

                            TrackingProtectionButton(
                                showIncognitoBadge = browserToolbarModel.isIncognito,
                                trackingDataFlow = contentFilterPopoverModel.trackingDataFlow,
                                onClick = contentFilterPopoverModel::openPopover
                            )

                            if (browserToolbarModel.useSingleBrowserToolbar) {
                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                            }
                        }
                    }

                    // We need to apply padding if all of the controls are hidden to prevent the
                    // URLBar from hitting the edge.  Normally, that padding is provided by the
                    // buttons beside it.
                    AnimatedVisibility(visible = isEditing) {
                        Spacer(Modifier.size(Dimensions.PADDING_LARGE))
                    }

                    URLBar(modifier = Modifier.weight(1.0f))

                    AnimatedVisibility(visible = isEditing) {
                        Spacer(Modifier.size(Dimensions.PADDING_LARGE))
                    }

                    AnimatedVisibility(visible = !isEditing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (browserToolbarModel.useSingleBrowserToolbar) {
                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                                NeevaScopeButton(
                                    isLandscape = true,
                                    showRedditDot = showRedditDot,
                                    isIncognito = browserToolbarModel.isIncognito
                                )
                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                                AddToSpaceButton()
                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                                TabSwitcherButton()
                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                            }

                            OverflowMenu(
                                overflowMenuData = overflowMenuData,
                                onMenuItem = {
                                    browserToolbarModel.onMenuItem(OverflowMenuItemId.values()[it])
                                }
                            )
                        }
                    }
                }

                LoadingBar(
                    progressFlow = browserToolbarModel.tabProgressFlow,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                BrowserToolbarContentFilterPopover(
                    contentFilterPopoverModel = contentFilterPopoverModel
                )

                if (
                    browserToolbarModel.useSingleBrowserToolbar &&
                    browserToolbarModel.shouldShowNeevaScopeTooltip()
                ) {
                    browserToolbarModel.getNeevaScopeModel()?.let {
                        NeevaScopeTooltip(
                            neevaScopeModel = it,
                            showRedditDot = showRedditDot,
                            isLandscape = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ToolbarPreview_Blank(useSingleBrowserToolbar: Boolean) {
    OneBooleanPreviewContainer { isIncognito ->
        CompositionLocalProvider(
            LocalBrowserToolbarModel provides PreviewBrowserToolbarModel(
                useSingleBrowserToolbar = useSingleBrowserToolbar,
                isIncognito = isIncognito,
                displayedInfo = ActiveTabModel.DisplayedInfo(
                    ActiveTabModel.DisplayMode.PLACEHOLDER,
                    "Search or enter address"
                )
            )
        ) {
            BrowserToolbar(
                findInPageModel = PreviewFindInPageModel(),
                contentFilterPopoverModel = PreviewContentFilterPopoverModel()
            )
        }
    }
}

@Preview("Blank, LTR", locale = "en")
@Preview("Blank, RTL", locale = "he")
@Composable
fun ToolbarPreview_Blank_Portrait() {
    ToolbarPreview_Blank(false)
}

@Composable
internal fun ToolbarPreview_Focus(useSingleBrowserToolbar: Boolean) {
    OneBooleanPreviewContainer { isIncognito ->
        CompositionLocalProvider(
            LocalBrowserToolbarModel provides PreviewBrowserToolbarModel(
                useSingleBrowserToolbar = useSingleBrowserToolbar,
                isIncognito = isIncognito,
                displayedInfo = ActiveTabModel.DisplayedInfo(ActiveTabModel.DisplayMode.QUERY),
                urlBarModelStateValue = URLBarModelState(
                    isEditing = true,
                    // TODO(kobec): Previews don't show the text cursor for some reason
                )
            )
        ) {
            BrowserToolbar(
                findInPageModel = PreviewFindInPageModel(),
                contentFilterPopoverModel = PreviewContentFilterPopoverModel()
            )
        }
    }
}

@Preview("Focus, LTR", locale = "en")
@Preview("Focus, RTL", locale = "he")
@Composable
fun ToolbarPreview_Focus_Portrait() {
    ToolbarPreview_Focus(false)
}

@Composable
internal fun ToolbarPreview_Typing(useSingleBrowserToolbar: Boolean) {
    OneBooleanPreviewContainer { isIncognito ->
        CompositionLocalProvider(
            LocalBrowserToolbarModel provides PreviewBrowserToolbarModel(
                useSingleBrowserToolbar = useSingleBrowserToolbar,
                isIncognito = isIncognito,
                displayedInfo = ActiveTabModel.DisplayedInfo(ActiveTabModel.DisplayMode.QUERY),
                urlBarModelStateValue = URLBarModelState(
                    isEditing = true,
                    // TODO(kobec): Previews don't show the text cursor for some reason
                    textFieldValue = TextFieldValue(text = "typing", selection = TextRange(5))
                )
            )
        ) {
            BrowserToolbar(
                findInPageModel = PreviewFindInPageModel(),
                contentFilterPopoverModel = PreviewContentFilterPopoverModel()
            )
        }
    }
}

@Preview("Typing, LTR", locale = "en")
@Preview("Typing, RTL", locale = "he")
@Composable
fun ToolbarPreview_Typing_Portrait() {
    ToolbarPreview_Typing(false)
}

@Composable
internal fun ToolbarPreview_Search(useSingleBrowserToolbar: Boolean) {
    OneBooleanPreviewContainer { isIncognito ->
        CompositionLocalProvider(
            LocalBrowserToolbarModel provides PreviewBrowserToolbarModel(
                useSingleBrowserToolbar = useSingleBrowserToolbar,
                isIncognito = isIncognito,
                displayedInfo = ActiveTabModel.DisplayedInfo(
                    ActiveTabModel.DisplayMode.QUERY,
                    displayedText = "search query"
                ),
                trackers = 9
            )
        ) {
            BrowserToolbar(
                findInPageModel = PreviewFindInPageModel(),
                contentFilterPopoverModel = PreviewContentFilterPopoverModel()
            )
        }
    }
}

@Preview("Search, LTR", locale = "en")
@Preview("Search, RTL", locale = "he")
@Composable
fun ToolbarPreview_Search_Portrait() {
    ToolbarPreview_Search(false)
}

@Composable
internal fun ToolbarPreview_Loading(useSingleBrowserToolbar: Boolean) {
    // TODO(kobec/dan): not completely accurate because of weblayer url bar i think...
    OneBooleanPreviewContainer { isIncognito ->
        CompositionLocalProvider(
            LocalBrowserToolbarModel provides PreviewBrowserToolbarModel(
                useSingleBrowserToolbar = useSingleBrowserToolbar,
                isIncognito = isIncognito,
                displayedInfo = ActiveTabModel.DisplayedInfo(
                    ActiveTabModel.DisplayMode.URL,
                    displayedText = "website.url"
                ),
                trackers = 999,
                tabProgressValue = 75
            )
        ) {
            BrowserToolbar(
                findInPageModel = PreviewFindInPageModel(),
                contentFilterPopoverModel = PreviewContentFilterPopoverModel()
            )
        }
    }
}

@Preview("Loading, LTR", locale = "en")
@Preview("Loading, RTL", locale = "he")
@Composable
fun ToolbarPreview_Loading_Portrait() {
    ToolbarPreview_Loading(false)
}
