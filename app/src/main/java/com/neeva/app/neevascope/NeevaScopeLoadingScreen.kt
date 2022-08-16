package com.neeva.app.neevascope

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.neeva.app.R
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun NeevaScopeLoadingScreen() {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.PADDING_LARGE),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // TODO: Add Lottie animation
            Image(
                painter = painterResource(id = R.drawable.neevascope),
                contentDescription = null
            )

            Spacer(Modifier.padding(Dimensions.PADDING_HUGE))

            Text(
                text = stringResource(id = R.string.neevascope_loading),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(Dimensions.PADDING_LARGE)
            )
        }
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun NeevaScopeLoading_Preview() {
    NeevaTheme {
        NeevaScopeLoadingScreen()
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun NeevaScopeLoading_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        NeevaScopeLoadingScreen()
    }
}
