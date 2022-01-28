package com.neeva.app.urlbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.Button

@Composable
fun LocationLabel(
    urlBarValue: String,
    backgroundColor: Color,
    foregroundColor: Color,
    showIncognitoBadge: Boolean,
    locationInfoResource: Int?,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(backgroundColor)
            .wrapContentSize(Alignment.Center)
            .defaultMinSize(minHeight = 40.dp)
    ) {
        val iconSize = 24.dp
        val iconModifier = Modifier.padding(8.dp).size(iconSize)
        if (showIncognitoBadge) {
            Image(
                painter = painterResource(R.drawable.ic_incognito),
                contentDescription = stringResource(R.string.incognito),
                modifier = iconModifier,
                colorFilter = ColorFilter.tint(foregroundColor)
            )
        } else {
            Box(modifier = iconModifier)
        }

        Spacer(modifier = Modifier.weight(1.0f))

        if (locationInfoResource != null) {
            Image(
                painter = painterResource(locationInfoResource),
                contentDescription = "secure site",
                modifier = Modifier.padding(8.dp).size(16.dp),
                colorFilter = ColorFilter.tint(foregroundColor),
                contentScale = ContentScale.Fit
            )
        }
        Text(
            text = urlBarValue.ifEmpty { "Search or enter address" },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            color = foregroundColor
        )

        Spacer(modifier = Modifier.weight(1.0f))

        Button(
            enabled = true,
            resID = R.drawable.ic_baseline_refresh_24,
            contentDescription = stringResource(R.string.reload),
            onClick = onReload,
            colorTint = foregroundColor
        )
    }
}

class LocationLabelPreviews : BooleanPreviewParameterProvider<LocationLabelPreviews.Params>(4) {
    data class Params(
        val darkTheme: Boolean,
        val isIncognito: Boolean,
        val showLock: Boolean,
        val showMagnifier: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isIncognito = booleanArray[1],
        showLock = booleanArray[2],
        showMagnifier = booleanArray[3]
    )

    @Preview("1x font scale")
    @Preview("2x font scale", fontScale = 2.0f)
    @Composable
    fun LocationLabelPreview(
        @PreviewParameter(LocationLabelPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            LocationLabel(
                urlBarValue = "www.reddit.com",
                backgroundColor = MaterialTheme.colorScheme.background,
                foregroundColor = MaterialTheme.colorScheme.onSurface,
                showIncognitoBadge = params.isIncognito,
                locationInfoResource = when {
                    params.showMagnifier -> R.drawable.ic_baseline_search_24
                    params.showLock -> R.drawable.ic_baseline_lock_18
                    else -> null
                },
                onReload = {}
            )
        }
    }
}
