package com.neeva.app.zeroQuery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun IncognitoZeroQuery() {
    val backgroundColor = MaterialTheme.colorScheme.inverseSurface

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimensions.PADDING_LARGE)
    ) {
        Surface(
            color = backgroundColor,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shape = RoundedCornerShape(Dimensions.RADIUS_LARGE)
        ) {
            val spacingDp = with(LocalDensity.current) {
                MaterialTheme.typography.bodyMedium.lineHeight.toDp()
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Dimensions.PADDING_LARGE)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_incognito),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(backgroundColor),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = LocalContentColor.current, shape = CircleShape)
                        .padding(Dimensions.PADDING_SMALL)
                )

                Spacer(modifier = Modifier.height(spacingDp))

                Text(
                    text = stringResource(id = R.string.incognito_zero_query_title),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(spacingDp))

                Text(
                    text = stringResource(id = R.string.incognito_zero_query_body),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(spacingDp))

                Text(
                    text = stringResource(id = R.string.incognito_zero_query_footer),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

class IncognitoZeroQueryPreviews :
    BooleanPreviewParameterProvider<IncognitoZeroQueryPreviews.Params>(1) {
    data class Params(
        val darkTheme: Boolean
    )

    override fun createParams(booleanArray: BooleanArray): Params {
        return Params(darkTheme = booleanArray[0])
    }

    @Preview(fontScale = 1.0f, locale = "en")
    @Preview(fontScale = 2.0f, locale = "en")
    @Composable
    fun DefaultPreview(
        @PreviewParameter(IncognitoZeroQueryPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            IncognitoZeroQuery()
        }
    }
}
