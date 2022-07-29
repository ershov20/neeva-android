package com.neeva.app.firstrun

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.StickyFooter
import com.neeva.app.firstrun.widgets.texts.WelcomeHeader
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LandscapePreviewsDark
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.PreviewCompositionLocals
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.widgets.ColumnWithMoreIndicator
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onShowDefaultBrowserSettings: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    loggingConsentState: MutableState<Boolean>,
    toggleLoggingConsentState: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = if (LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.background
    } else {
        ColorPalette.Brand.Offwhite
    }

    val scrollState = rememberScrollState()
    val onButtonClick = {
        if (scrollState.value != scrollState.maxValue) {
            if (!scrollState.isScrollInProgress) {
                coroutineScope.launch {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        } else {
            onShowDefaultBrowserSettings()
        }
    }

    Surface(
        color = backgroundColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints {
            if (constraints.maxWidth > constraints.maxHeight) {
                // Landscape
                // TODO(dan.alcantara): Update this if UX gives us landscape mocks.
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.default_browser_prompt),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = Dimensions.PADDING_HUGE)
                                .weight(1.0f)
                        )
                    }

                    Column(modifier = Modifier.weight(1.0f)) {
                        ColumnWithMoreIndicator(
                            modifier = Modifier.weight(1.0f, fill = true),
                            color = backgroundColor,
                            scrollState = scrollState
                        ) {
                            Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

                            WelcomeHeader(
                                primaryLabel = stringResource(id = R.string.first_run_intro),
                                secondaryLabel = stringResource(id = R.string.first_run_ad_free),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

                            ConsentCheckbox(
                                loggingConsentState = loggingConsentState,
                                toggleLoggingConsentState = toggleLoggingConsentState,
                                modifier = Modifier.padding(Dimensions.PADDING_SMALL)
                            )

                            Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))
                        }

                        WelcomeScreenGetStartedButton(
                            scrollState = scrollState,
                            modifier = Modifier.padding(Dimensions.PADDING_SMALL),
                            onClick = onButtonClick,
                            onOpenUrl = onOpenUrl
                        )
                    }
                }
            } else {
                // Portrait
                Column {
                    ColumnWithMoreIndicator(
                        modifier = Modifier
                            .weight(1.0f, fill = true)
                            .padding(horizontal = Dimensions.PADDING_HUGE),
                        color = backgroundColor,
                        scrollState = scrollState
                    ) {
                        WelcomeHeader(
                            primaryLabel = stringResource(id = R.string.first_run_intro),
                            secondaryLabel = stringResource(id = R.string.first_run_ad_free),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Dimensions.PADDING_HUGE)
                        )

                        Image(
                            painter = painterResource(R.drawable.default_browser_prompt),
                            contentDescription = null,
                            modifier = Modifier.padding(Dimensions.PADDING_HUGE)
                        )

                        ConsentCheckbox(
                            loggingConsentState = loggingConsentState,
                            toggleLoggingConsentState = toggleLoggingConsentState,
                            modifier = Modifier.padding(
                                vertical = Dimensions.PADDING_MEDIUM
                            )
                        )
                    }

                    StickyFooter(scrollState = scrollState) {
                        WelcomeScreenGetStartedButton(
                            scrollState = scrollState,
                            modifier = Modifier.padding(
                                horizontal = Dimensions.PADDING_HUGE,
                                vertical = Dimensions.PADDING_MEDIUM
                            ),
                            onClick = onButtonClick,
                            onOpenUrl = onOpenUrl
                        )
                    }
                }
            }
        }
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun WelcomeScreen_Light_Preview() {
    val currentState = remember { mutableStateOf(true) }
    PreviewCompositionLocals {
        NeevaTheme(useDarkTheme = false) {
            WelcomeScreen(
                onOpenUrl = {},
                onShowDefaultBrowserSettings = {},
                loggingConsentState = currentState,
                toggleLoggingConsentState = { currentState.value = !currentState.value }
            )
        }
    }
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun WelcomeScreen_Dark_Preview() {
    val currentState = remember { mutableStateOf(true) }
    PreviewCompositionLocals {
        NeevaTheme(useDarkTheme = true) {
            WelcomeScreen(
                onOpenUrl = {},
                onShowDefaultBrowserSettings = {},
                loggingConsentState = currentState,
                toggleLoggingConsentState = { currentState.value = !currentState.value }
            )
        }
    }
}

@Composable
fun LegalFooter(
    modifier: Modifier = Modifier,
    onOpenUrl: (Uri) -> Unit
) {
    val privacyUrl = Uri.parse(LocalNeevaConstants.current.appPrivacyURL)
    val termsUrl = Uri.parse(LocalNeevaConstants.current.appTermsURL)

    CompositionLocalProvider(
        LocalContentColor.provides(MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            FlowRow(mainAxisAlignment = FlowMainAxisAlignment.Center) {
                Text(
                    text = stringResource(R.string.terms_of_service),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .defaultMinSize(Dimensions.SIZE_TOUCH_TARGET)
                        .clickable { onOpenUrl(termsUrl) }
                )

                Spacer(modifier = Modifier.size(Dimensions.PADDING_TINY))

                Text(
                    text = stringResource(R.string.dot_separator),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.size(Dimensions.PADDING_TINY))

                Text(
                    text = stringResource(R.string.privacy_policy),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .defaultMinSize(Dimensions.SIZE_TOUCH_TARGET)
                        .clickable { onOpenUrl(privacyUrl) }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreenGetStartedButton(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onOpenUrl: (Uri) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    when (scrollState.maxValue) {
                        Int.MAX_VALUE, scrollState.value -> R.string.get_started
                        else -> R.string.continue_button
                    }
                ),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

        LegalFooter(
            modifier = Modifier
                .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                .fillMaxWidth(),
            onOpenUrl = onOpenUrl
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentCheckbox(
    loggingConsentState: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    toggleLoggingConsentState: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { toggleLoggingConsentState() }
    ) {
        Checkbox(
            checked = loggingConsentState.value,
            onCheckedChange = null,
            modifier = Modifier.size(Dimensions.SIZE_TOUCH_TARGET)
        )

        Text(stringResource(R.string.logging_consent))
    }
}
