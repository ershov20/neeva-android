package com.neeva.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.constraintlayout.compose.ConstraintLayout
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun CrashedTabContainer(
    onReload: () -> Unit,
    modifier: Modifier
) {
    val browserWrapper = LocalBrowserWrapper.current
    val shouldDisplayCrashedTab by browserWrapper.shouldDisplayCrashedTab.collectAsState(false)

    if (shouldDisplayCrashedTab) {
        CrashedTab(onReload, modifier)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CrashedTab(
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxSize(1.0f)
            // TODO(dan.alcantara): Temporarily add this until we change how BrowserScaffold
            //                      sandwiches the content between the two toolbars.
            .padding(vertical = dimensionResource(id = R.dimen.bottom_toolbar_height))
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.PADDING_LARGE)
        ) {
            val (buttonId, bodyId) = createRefs()

            Button(
                onClick = onReload,
                modifier = Modifier
                    .constrainAs(buttonId) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.reload),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .constrainAs(bodyId) {
                        top.linkTo(parent.top)
                        bottom.linkTo(buttonId.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(Dimensions.PADDING_LARGE)
                    .scrollable(rememberScrollState(), orientation = Orientation.Vertical)
            ) {
                Text(
                    text = stringResource(id = R.string.tab_crashed_title),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(id = R.string.tab_crashed_body),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_crashed_tab),
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

class CrashedTabPreviews : BooleanPreviewParameterProvider<CrashedTabPreviews.Params>(1) {
    data class Params(val useDarkTheme: Boolean)

    override fun createParams(booleanArray: BooleanArray) = Params(
        useDarkTheme = booleanArray[0]
    )

    @Preview("1x font scale", locale = "en")
    @Preview("2x font scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x font scale", locale = "he")
    @Preview("RTL, 2x font scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun Preview(@PreviewParameter(CrashedTabPreviews::class) params: Params) {
        NeevaTheme(useDarkTheme = params.useDarkTheme) {
            CrashedTab(onReload = {})
        }
    }
}
