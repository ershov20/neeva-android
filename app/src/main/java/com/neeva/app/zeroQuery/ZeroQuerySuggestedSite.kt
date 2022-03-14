package com.neeva.app.zeroQuery

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.entities.Favicon.Companion.toBitmap
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.toUserVisibleString
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

@Composable
fun ZeroQuerySuggestedSite(
    suggestedSite: SuggestedSite,
    faviconCache: FaviconCache,
    domainProvider: DomainProvider,
    onClick: (Uri) -> Unit
) {
    val siteUri = Uri.parse(suggestedSite.site.siteURL)
    val faviconBitmap by faviconCache.getFaviconAsync(siteUri)
    val label = suggestedSite.site.toUserVisibleString(domainProvider)

    ZeroQuerySuggestedSite(
        faviconBitmap = faviconBitmap,
        iconOverride = suggestedSite.iconOverride,
        label = label,
        onClick = { onClick(siteUri) }
    )
}

@Composable
fun ZeroQuerySuggestedSite(
    faviconBitmap: Bitmap?,
    iconOverride: ImageVector?,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = label) {
                onClick()
            }
            .padding(
                start = Dimensions.PADDING_LARGE,
                end = Dimensions.PADDING_LARGE
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FaviconView(
                bitmap = faviconBitmap,
                imageOverride = iconOverride,
                bordered = false,
                size = 48.dp
            )

            Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

class ZeroQuerySuggestedSitePreviewParameterProvider :
    BooleanPreviewParameterProvider<ZeroQuerySuggestedSitePreviewParameterProvider.Params>(3) {
    data class Params(
        val useDarkTheme: Boolean,
        val useLongTitle: Boolean,
        val useLargeContainer: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        useDarkTheme = booleanArray[0],
        useLongTitle = booleanArray[1],
        useLargeContainer = booleanArray[2]
    )

    @Preview("1x font scale", locale = "en")
    @Preview("2x font scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x font scale", locale = "he")
    @Preview("RTL, 2x font scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun ZeroQuerySuggestedSitePreview_Default(
        @PreviewParameter(ZeroQuerySuggestedSitePreviewParameterProvider::class) params: Params
    ) {
        val label = if (params.useLongTitle) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            "Short"
        }

        val containerSize = if (params.useLargeContainer) {
            300.dp
        } else {
            96.dp
        }

        NeevaTheme(useDarkTheme = params.useDarkTheme) {
            Box(modifier = Modifier.width(containerSize)) {
                ZeroQuerySuggestedSite(
                    faviconBitmap = Uri.parse("http://neeva.com").toBitmap(),
                    iconOverride = null,
                    label = label,
                    onClick = {}
                )
            }
        }
    }
}
