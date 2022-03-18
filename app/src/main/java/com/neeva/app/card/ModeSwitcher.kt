package com.neeva.app.card

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.ui.AnimationConstants
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

private val buttonHeight = 48.dp
private val buttonWidth = 64.dp
private val buttonRadius = buttonHeight / 2

@Composable
fun ModeSwitcher(
    webLayerModel: WebLayerModel,
    onSwitchScreen: (SelectedScreen) -> Unit
) {
    val currentBrowser by webLayerModel.currentBrowserFlow.collectAsState()

    // Keep track of what screen is currently being viewed by the user.
    val selectedScreen: MutableState<SelectedScreen> = remember(currentBrowser) {
        mutableStateOf(
            if (currentBrowser.isIncognito) {
                SelectedScreen.INCOGNITO_TABS
            } else {
                SelectedScreen.REGULAR_TABS
            }
        )
    }

    ModeSwitcher(
        selectedScreen = selectedScreen,
        onSwitchScreen = onSwitchScreen
    )
}

@Composable
fun ModeSwitcher(
    selectedScreen: State<SelectedScreen>,
    onSwitchScreen: (SelectedScreen) -> Unit
) {
    // Set up the animation between the different states.
    val transition = updateTransition(
        targetState = selectedScreen.value,
        label = "mode switch"
    )

    val bubbleColor = transition.animateColor(
        transitionSpec = { tween(AnimationConstants.ANIMATION_DURATION_MS) },
        label = "bubble color"
    ) {
        if (it == SelectedScreen.INCOGNITO_TABS) {
            MaterialTheme.colorScheme.inverseSurface
        } else {
            MaterialTheme.colorScheme.primary
        }
    }

    val incognitoIconColor = transition.animateColor(
        transitionSpec = { tween(AnimationConstants.ANIMATION_DURATION_MS) },
        label = "incognito icon color"
    ) {
        if (it == SelectedScreen.INCOGNITO_TABS) {
            MaterialTheme.colorScheme.inverseOnSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    val regularIconColor = transition.animateColor(
        transitionSpec = { tween(AnimationConstants.ANIMATION_DURATION_MS) },
        label = "regular icon color"
    ) {
        if (it == SelectedScreen.INCOGNITO_TABS) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onPrimary
        }
    }

    val bubblePosition = transition.animateDp(
        transitionSpec = { tween(AnimationConstants.ANIMATION_DURATION_MS) },
        label = "bubble position"
    ) {
        buttonWidth * it.ordinal
    }

    ModeSwitcher(
        selectedScreen = selectedScreen.value,
        onSwitchScreen = onSwitchScreen,
        bubblePosition = bubblePosition.value,
        bubbleColor = bubbleColor.value,
        incognitoIconColor = incognitoIconColor.value,
        regularIconColor = regularIconColor.value
    )
}

@Composable
fun ModeSwitcher(
    selectedScreen: SelectedScreen,
    onSwitchScreen: (SelectedScreen) -> Unit,
    bubblePosition: Dp,
    bubbleColor: Color,
    incognitoIconColor: Color,
    regularIconColor: Color
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .height(buttonHeight),
        contentAlignment = Alignment.Center
    ) {
        // Slider: The slider is represented by a Row with rounded corners and a button-sized Box
        //         that is offset according to which button is currently pressed.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(buttonRadius)
                )
                .width(buttonWidth * SelectedScreen.values().size)
                .fillMaxHeight()
                .offset(x = bubblePosition)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = bubbleColor,
                        shape = RoundedCornerShape(buttonRadius)
                    )
                    .width(buttonWidth)
                    .fillMaxHeight()
            )
        }

        Row {
            Icon(
                painter = painterResource(R.drawable.ic_incognito),
                contentDescription = stringResource(R.string.incognito),
                tint = incognitoIconColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(buttonRadius))
                    .clickable(enabled = selectedScreen != SelectedScreen.INCOGNITO_TABS) {
                        onSwitchScreen(SelectedScreen.INCOGNITO_TABS)
                    }
                    .width(buttonWidth)
                    .fillMaxHeight()
                    .padding(Dimensions.PADDING_MEDIUM)
            )

            Icon(
                painter = painterResource(R.drawable.ic_baseline_filter_none_24),
                contentDescription = stringResource(R.string.tabs),
                tint = regularIconColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(buttonRadius))
                    .clickable(enabled = selectedScreen != SelectedScreen.REGULAR_TABS) {
                        onSwitchScreen(SelectedScreen.REGULAR_TABS)
                    }
                    .width(buttonWidth)
                    .fillMaxHeight()
                    .padding(Dimensions.PADDING_MEDIUM)
            )
        }
    }
}

@Preview("LTR", locale = "en")
@Preview("RTL", locale = "he")
@Composable
private fun ModeSwitcherPreview() {
    OneBooleanPreviewContainer { isIncognito ->
        val selectedScreen = remember {
            mutableStateOf(
                when (isIncognito) {
                    false -> SelectedScreen.REGULAR_TABS
                    else -> SelectedScreen.INCOGNITO_TABS
                }
            )
        }

        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            ModeSwitcher(
                selectedScreen = selectedScreen,
                onSwitchScreen = { selectedScreen.value = it }
            )
        }
    }
}
