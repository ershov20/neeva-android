// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.history

import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.neeva.app.LocalDomainProvider
import com.neeva.app.LocalHistoryManager
import com.neeva.app.LocalPopupModel
import com.neeva.app.R
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.daos.SitePlusVisit
import com.neeva.app.storage.entities.Site
import com.neeva.app.storage.entities.Visit
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.storage.favicons.previewFaviconCache
import com.neeva.app.suggestions.NavSuggestionRow
import com.neeva.app.suggestions.toNavSuggestion
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.toLocalDate
import com.neeva.app.ui.widgets.AutocompleteTextField
import com.neeva.app.ui.widgets.ClickableRow
import com.neeva.app.ui.widgets.DefaultDivider
import com.neeva.app.ui.widgets.HeavyDivider
import com.neeva.app.ui.widgets.PillSurface
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.RowActionStartIconParams
import java.util.Date
import kotlinx.coroutines.flow.flowOf

@Composable
fun HistoryUI(
    onClearHistory: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    faviconCache: FaviconCache
) {
    val domainProvider = LocalDomainProvider.current
    val historyManager = LocalHistoryManager.current
    val snackbarModel = LocalPopupModel.current
    val context = LocalContext.current
    val filterTextFieldValue = remember { mutableStateOf(TextFieldValue()) }

    val allHistory = remember(filterTextFieldValue.value.text) {
        historyManager
            .getPagedHistory(
                startTime = Date(0L),
                filter = filterTextFieldValue.value.text
            )
    }.collectAsLazyPagingItems()

    HistoryUI(
        allHistory = allHistory,
        onClearHistory = onClearHistory,
        onOpenUrl = onOpenUrl,
        onDeleteVisit = { visitUID: Int, siteLabel: String ->
            historyManager.markVisitForDeletion(visitUID, isMarkedForDeletion = true)
            snackbarModel.showSnackbar(
                message = context.getString(R.string.history_removed_visit, siteLabel),
                actionLabel = context.getString(R.string.undo),
                onActionPerformed = {
                    historyManager.markVisitForDeletion(visitUID, isMarkedForDeletion = false)
                }
            )
        },
        faviconCache = faviconCache,
        domainProvider = domainProvider,
        filterTextFieldValue = filterTextFieldValue
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryUI(
    allHistory: LazyPagingItems<SitePlusVisit>,
    onClearHistory: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    onDeleteVisit: (visitUID: Int, siteLabel: String) -> Unit,
    faviconCache: FaviconCache,
    domainProvider: DomainProvider,
    filterTextFieldValue: MutableState<TextFieldValue>
) {
    LazyColumn {
        item {
            Box(
                modifier = Modifier
                    .padding(
                        horizontal = Dimensions.PADDING_LARGE,
                        vertical = Dimensions.PADDING_TINY
                    )
            ) {
                PillSurface {
                    AutocompleteTextField(
                        textFieldValue = filterTextFieldValue.value,
                        imageVector = Icons.Default.Search,
                        onTextEdited = {
                            filterTextFieldValue.component2().invoke(it)
                        },
                        onTextCleared = {
                            filterTextFieldValue.component2().invoke(TextFieldValue())
                        },
                        onSubmitted = {},
                        onAcceptSuggestion = {},
                        placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        placeholderText = stringResource(R.string.history_filter_placeholder),
                        focusImmediately = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height = 40.dp)
                    )
                }
            }
        }

        item {
            ClickableRow(
                primaryLabel = stringResource(R.string.settings_clear_browsing_data),
                actionIconParams = RowActionIconParams(
                    onTapAction = onClearHistory,
                    actionType = RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN,
                    size = Dimensions.SIZE_ICON_SMALL
                )
            )
        }

        itemsIndexed(
            items = allHistory,
            key = { _, site -> site.visit.visitUID }
        ) { index, site ->
            val previousTimestamp = (index - 1)
                .takeIf { it >= 0 }
                ?.let { allHistory[index - 1]?.visit?.timestamp?.toLocalDate() }
            val currentTimestamp = site?.visit?.timestamp?.toLocalDate()

            val showDate = when {
                currentTimestamp == null -> false
                previousTimestamp == null -> true
                else -> currentTimestamp != previousTimestamp
            }

            site?.let {
                val timestamp = it.visit.timestamp

                if (showDate) {
                    val formatted = SimpleDateFormat.getDateInstance().format(timestamp)
                    HistoryHeader(formatted)
                }

                HistoryEntry(site, faviconCache, domainProvider, onOpenUrl, onDeleteVisit)
            }
        }
    }
}

@PortraitPreviews
@Composable
fun HistoryHeaderPreview_Light() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        Surface {
            HistoryHeader(stringResource(id = R.string.debug_long_string_primary))
        }
    }
}

@PortraitPreviewsDark
@Composable
fun HistoryHeaderPreview_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        Surface {
            HistoryHeader(stringResource(id = R.string.debug_long_string_primary))
        }
    }
}

@Composable
fun HistoryHeader(text: String, useHeavyDivider: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (useHeavyDivider) {
            HeavyDivider()
        } else {
            DefaultDivider()
        }

        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Dimensions.PADDING_LARGE)
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
    }
}

@Composable
fun HistoryEntry(
    sitePlusVisit: SitePlusVisit,
    faviconCache: FaviconCache,
    domainProvider: DomainProvider,
    onOpenUrl: (Uri) -> Unit,
    onDeleteVisit: (visitUID: Int, siteLabel: String) -> Unit
) {
    val site = sitePlusVisit.site
    val navSuggestion = site.toNavSuggestion(domainProvider)
    val faviconBitmap: Bitmap? by faviconCache.getFaviconAsync(navSuggestion.url)

    val contentDescription = stringResource(
        R.string.history_remove_visit,
        sitePlusVisit.site.title ?: sitePlusVisit.site.siteURL
    )

    NavSuggestionRow(
        iconParams = RowActionStartIconParams(faviconBitmap = faviconBitmap),
        primaryLabel = navSuggestion.label,
        secondaryLabel = navSuggestion.secondaryLabel,
        onTapRow = { onOpenUrl(navSuggestion.url) },
        actionIconParams = RowActionIconParams(
            onTapAction = {
                onDeleteVisit(sitePlusVisit.visit.visitUID, navSuggestion.label)
            },
            actionType = RowActionIconParams.ActionType.DELETE,
            contentDescription = contentDescription
        )
    )
}

@PortraitPreviews
@Composable
fun HistoryUI_Preview_Light() = HistoryUI_Preview(useDarkTheme = false)

@PortraitPreviews
@Composable
fun HistoryUI_Preview_Dark() = HistoryUI_Preview(useDarkTheme = true)

@Composable
private fun HistoryUI_Preview(useDarkTheme: Boolean) {
    var ids = 0

    fun createSitePlusVisit(timestamp: Date): SitePlusVisit {
        val site = Site(
            siteUID = ids++,
            siteURL = "https://www.site$ids.com/",
            title = null,
            largestFavicon = null
        )

        // The only useful value here is the ID.
        val visit = Visit(
            visitUID = ids++,
            timestamp = timestamp,
            visitedSiteUID = site.siteUID
        )

        return SitePlusVisit(site, visit)
    }

    // Add items across several days.
    val allHistory = mutableListOf<SitePlusVisit>().apply {
        for (i in 0 until 7) {
            val currentDate = Calendar.getInstance().apply {
                set(2022, 4, 7 - i)
            }

            add(createSitePlusVisit(currentDate.time))
            add(createSitePlusVisit(currentDate.time))
        }
    }

    val allHistoryFlow = flowOf(PagingData.from(allHistory))
    val filterTextFieldValue = remember { mutableStateOf(TextFieldValue()) }

    NeevaThemePreviewContainer(useDarkTheme = useDarkTheme) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            HistoryUI(
                allHistory = allHistoryFlow.collectAsLazyPagingItems(),
                onClearHistory = {},
                onOpenUrl = {},
                onDeleteVisit = { _, _ -> },
                faviconCache = previewFaviconCache,
                domainProvider = previewFaviconCache.domainProvider,
                filterTextFieldValue = filterTextFieldValue
            )
        }
    }
}
