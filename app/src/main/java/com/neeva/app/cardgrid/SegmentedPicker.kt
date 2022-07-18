package com.neeva.app.cardgrid

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.browsing.toolbar.TabSwitcherIcon
import com.neeva.app.ui.AnimationConstants
import com.neeva.app.ui.LightDarkPreviewContainer

private val buttonHeight = 48.dp
private val buttonRadius = buttonHeight / 2
private val buttonWidth = 64.dp

@Composable
fun SegmentedPicker(
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
            MaterialTheme.colorScheme.onSurface
        }
    }

    val regularIconColor = transition.animateColor(
        transitionSpec = { tween(AnimationConstants.ANIMATION_DURATION_MS) },
        label = "regular icon color"
    ) {
        if (it == SelectedScreen.REGULAR_TABS) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    }

    val spacesIconColor = transition.animateColor(
        transitionSpec = { tween(AnimationConstants.ANIMATION_DURATION_MS) },
        label = "spaces icon color"
    ) {
        if (it == SelectedScreen.SPACES) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    }

    val bubblePosition = transition.animateDp(
        transitionSpec = { tween(AnimationConstants.ANIMATION_DURATION_MS) },
        label = "bubble position"
    ) {
        buttonWidth * it.ordinal
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
    ) {
        SegmentedPicker(
            selectedScreen = selectedScreen.value,
            onSwitchScreen = onSwitchScreen,
            bubblePosition = bubblePosition.value,
            bubbleColor = bubbleColor.value,
            incognitoIconColor = incognitoIconColor.value,
            regularIconColor = regularIconColor.value,
            spacesIconColor = spacesIconColor.value
        )
    }
}

@Composable
fun SegmentedPicker(
    selectedScreen: SelectedScreen,
    onSwitchScreen: (SelectedScreen) -> Unit,
    bubblePosition: Dp,
    bubbleColor: Color,
    incognitoIconColor: Color,
    regularIconColor: Color,
    spacesIconColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(buttonRadius),
        tonalElevation = 2.dp,
        modifier = Modifier.height(buttonHeight)
    ) {
        // Selected-button indicator.  Offset according to which button is pressed.
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(buttonRadius),
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(buttonWidth)
                .height(buttonHeight)
                .offset(x = bubblePosition)
        ) {}

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(buttonHeight)
        ) {
            Surface(
                color = Color.Transparent,
                contentColor = incognitoIconColor,
                shape = RoundedCornerShape(buttonRadius),
                modifier = Modifier
                    .width(buttonWidth)
                    .height(buttonHeight)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = selectedScreen != SelectedScreen.INCOGNITO_TABS) {
                            onSwitchScreen(SelectedScreen.INCOGNITO_TABS)
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_incognito),
                        contentDescription = stringResource(R.string.view_incognito_tabs)
                    )
                }
            }

            Surface(
                color = Color.Transparent,
                contentColor = regularIconColor,
                shape = RoundedCornerShape(buttonRadius),
                modifier = Modifier
                    .width(buttonWidth)
                    .fillMaxHeight()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = selectedScreen != SelectedScreen.REGULAR_TABS) {
                            onSwitchScreen(SelectedScreen.REGULAR_TABS)
                        }
                ) {
                    TabSwitcherIcon(contentDescription = stringResource(R.string.view_regular_tabs))
                }
            }

            Surface(
                color = Color.Transparent,
                contentColor = spacesIconColor,
                shape = RoundedCornerShape(buttonRadius),
                modifier = Modifier
                    .width(buttonWidth)
                    .fillMaxHeight()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = selectedScreen != SelectedScreen.SPACES) {
                            onSwitchScreen(SelectedScreen.SPACES)
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bookmarks_black_24),
                        contentDescription = stringResource(R.string.spaces),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Preview("Incognito tabs selected LTR", locale = "en")
@Preview("Incognito tabs selected RTL", locale = "he")
@Composable
fun SegmentedPickerPreview_Incognito() {
    LightDarkPreviewContainer {
        val selectedScreen = remember { mutableStateOf(SelectedScreen.INCOGNITO_TABS) }
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            SegmentedPicker(
                selectedScreen = selectedScreen
            ) { selectedScreen.value = it }
        }
    }
}

@Preview("Regular tabs selected LTR", locale = "en")
@Preview("Regular tabs selected RTL", locale = "he")
@Composable
fun SegmentedPickerPreview_Regular() {
    LightDarkPreviewContainer {
        val selectedScreen = remember { mutableStateOf(SelectedScreen.REGULAR_TABS) }
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            SegmentedPicker(
                selectedScreen = selectedScreen
            ) { selectedScreen.value = it }
        }
    }
}

@Preview("Spaces selected LTR", locale = "en")
@Preview("Spaces selected RTL", locale = "he")
@Composable
fun SegmentedPickerPreview_Spaces() {
    LightDarkPreviewContainer {
        val selectedScreen = remember { mutableStateOf(SelectedScreen.SPACES) }
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            SegmentedPicker(
                selectedScreen = selectedScreen
            ) { selectedScreen.value = it }
        }
    }
}
