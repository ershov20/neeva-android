package com.neeva.app.firstrun

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.OnboardingStickyFooter
import com.neeva.app.firstrun.widgets.buttons.CloseButton
import com.neeva.app.firstrun.widgets.textfields.rememberIsKeyboardOpen
import com.neeva.app.ui.theme.ColorPalette

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingContainer(
    showBrowser: () -> Unit,
    useSignUpStickyFooter: Boolean,
    stickyFooterOnClick: () -> Unit,
    useDarkTheme: Boolean,
    content: @Composable (Modifier) -> Unit
) {
    val backgroundColor = if (useDarkTheme) {
        MaterialTheme.colorScheme.background
    } else {
        ColorPalette.Brand.Offwhite
    }

    Surface(color = backgroundColor) {
        Box(Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            val isKeyboardOpen by rememberIsKeyboardOpen()
            Column {
                content(
                    Modifier
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(horizontal = dimensionResource(id = R.dimen.first_run_padding))
                        .then(Modifier.weight(1f, false))
                )

                if (!isKeyboardOpen) {
                    OnboardingStickyFooter(useSignUpStickyFooter, scrollState, stickyFooterOnClick)
                }
            }

            CloseButton(
                onClick = showBrowser,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
