package com.neeva.app.browsing.toolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import com.neeva.app.LocalBrowserToolbarModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.browsing.findinpage.FindInPageToolbar
import com.neeva.app.browsing.urlbar.URLBar
import com.neeva.app.overflowmenu.OverflowMenu
import com.neeva.app.settings.LocalDebugFlags
import com.neeva.app.ui.theme.Dimensions

@Composable
fun BrowserToolbarContainer(
    useSingleBrowserToolbar: Boolean,
    isUpdateAvailable: Boolean,
    topOffset: Float
) {
    val topOffsetDp = with(LocalDensity.current) { topOffset.toDp() }
    BrowserToolbar(
        useSingleBrowserToolbar = useSingleBrowserToolbar,
        isUpdateAvailable = isUpdateAvailable,
        modifier = Modifier
            .offset(y = topOffsetDp)
            .background(MaterialTheme.colorScheme.background)
    )
}

@Composable
fun BrowserToolbar(
    useSingleBrowserToolbar: Boolean,
    isUpdateAvailable: Boolean,
    modifier: Modifier
) {
    val browserToolbarModel = LocalBrowserToolbarModel.current
    val browserWrapper = LocalBrowserWrapper.current

    val urlBarModel = browserWrapper.urlBarModel
    val isEditing by urlBarModel.isEditing.collectAsState(false)

    val findInPageModel = browserWrapper.findInPageModel
    val findInPageInfo by findInPageModel.findInPageInfo.collectAsState()

    val activeTabModel = browserWrapper.activeTabModel
    val progress: Int by activeTabModel.progressFlow.collectAsState()
    val navigationInfoFlow = activeTabModel.navigationInfoFlow.collectAsState()

    // TODO(kobec): use a ViewModel interface pattern to encapsulate the params and wiring + see previews closer to the design.
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
            val enableShowDesktopSite = LocalEnvironment.current.settingsDataModel
                .getDebugFlagValue(LocalDebugFlags.DEBUG_ENABLE_SHOW_DESKTOP_SITE)

            val overflowMenuData = remember(
                useSingleBrowserToolbar,
                isForwardEnabled,
                isUpdateAvailable,
                isDesktopUserAgentEnabled,
                enableShowDesktopSite
            ) {
                createBrowserOverflowMenuData(
                    isIconRowVisible = !useSingleBrowserToolbar,
                    isForwardEnabled = isForwardEnabled,
                    isUpdateAvailableVisible = isUpdateAvailable,
                    isDesktopUserAgentEnabled = isDesktopUserAgentEnabled,
                    enableShowDesktopSite = enableShowDesktopSite
                )
            }

            Box {
                if (useSingleBrowserToolbar) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = !isEditing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))

                                BackButton(contentColorFor(MaterialTheme.colorScheme.background))

                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))

                                ForwardButton(contentColorFor(MaterialTheme.colorScheme.background))

                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))

                                RefreshButton()
                            }
                        }

                        URLBar(modifier = Modifier.weight(1.0f)) { modifier ->
                            ShareButton(modifier = modifier)
                        }

                        AnimatedVisibility(visible = !isEditing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AddToSpaceButton()

                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))

                                TabSwitcherButton()

                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))

                                OverflowMenu(
                                    overflowMenuData = overflowMenuData,
                                    onMenuItem = browserToolbarModel::onMenuItem
                                )

                                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                            }
                        }
                    }
                } else {
                    URLBar { modifier ->
                        OverflowMenu(
                            overflowMenuData = overflowMenuData,
                            onMenuItem = browserToolbarModel::onMenuItem,
                            modifier = modifier
                        )
                    }
                }

                LoadingBar(
                    progress = progress,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
