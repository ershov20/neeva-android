package com.neeva.app.neevascope

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun NeevascopeInfoScreen(
    onTapAction: () -> Unit
) {
    Surface {
        BoxWithConstraints {
            if (constraints.maxWidth > constraints.maxHeight) {
                // Landscape
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.weight(1.0f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cheatsheet),
                            contentDescription = null,
                            modifier = Modifier.height(224.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .padding(horizontal = Dimensions.PADDING_LARGE)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NeevascopeInfoHeader()

                        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

                        NeevascopeInfoBody()

                        Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

                        NeevascopeInfoButton(
                            onTapAction = onTapAction,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
            } else {
                // Portrait
                Column(
                    modifier = Modifier
                        .padding(horizontal = Dimensions.PADDING_LARGE)
                        .verticalScroll(rememberScrollState())
                ) {
                    NeevascopeInfoHeader()

                    Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

                    NeevascopeInfoBody()

                    Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

                    Image(
                        painter = painterResource(id = R.drawable.cheatsheet),
                        contentDescription = null,
                        modifier = Modifier
                            .height(224.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    NeevascopeInfoButton(
                        onTapAction = onTapAction,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun NeevascopeInfoHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = Dimensions.PADDING_LARGE)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_neeva_logo),
            contentDescription = null
        )

        Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))

        Text(
            text = stringResource(id = R.string.neevascope),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun NeevascopeInfoBody() {
    Column(
        modifier = Modifier.padding(horizontal = Dimensions.PADDING_LARGE)
    ) {
        Text(
            text = stringResource(id = R.string.neevascope_intro_title),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
        Text(
            text = stringResource(id = R.string.neevascope_intro_body),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun NeevascopeInfoButton(
    onTapAction: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onTapAction,
        modifier = modifier
            .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
            .height(48.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.neevascope_got_it),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun NeevascopeInfo_Light_Preview() {
    NeevaTheme {
        NeevascopeInfoScreen(
            onTapAction = {}
        )
    }
}

@Preview("Dark 1x scale", locale = "en", uiMode = UI_MODE_NIGHT_YES)
@Preview("Dark 2x scale", locale = "en", fontScale = 2.0f, uiMode = UI_MODE_NIGHT_YES)
@Preview(
    "Pixel 2 landscape, 1x scale", widthDp = 731, heightDp = 390, locale = "en",
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun NeevascopeInfo_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        NeevascopeInfoScreen(
            onTapAction = {}
        )
    }
}
