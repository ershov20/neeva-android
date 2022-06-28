package com.neeva.app.cookiecutter.ui.icon

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.neeva.app.R
import com.neeva.app.cookiecutter.TrackingData
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TrackingProtectionButton(
    showIncognitoBadge: Boolean,
    trackingDataFlow: StateFlow<TrackingData?>?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Putting collectAsState here so it does not cause unnecessary recompositions with the Popover
    val trackingData = trackingDataFlow?.collectAsState()?.value
    TrackingProtectionButton(
        showIncognitoBadge = showIncognitoBadge,
        trackersBlocked = trackingData?.numTrackers ?: 0,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun TrackingProtectionButton(
    showIncognitoBadge: Boolean,
    trackersBlocked: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(Dimensions.SIZE_TOUCH_TARGET)
    ) {
        ConstraintLayout {
            val (trackingProtectionIcon, numberBadge) = createRefs()
            val verticalMiddleGuideline = createGuidelineFromBottom(0.5f)

            val iconModifier = Modifier
                .size(21.dp)
                .constrainAs(trackingProtectionIcon) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    top.linkTo(parent.top)
                }

            if (showIncognitoBadge) {
                Icon(
                    painter = painterResource(R.drawable.ic_incognito),
                    contentDescription = stringResource(R.string.incognito),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = iconModifier
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_shield),
                    contentDescription = stringResource(R.string.tracking_protection),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = iconModifier
                )
            }

            val horizontalMiddleGuideline = createGuidelineFromStart(0.72f)
            NumberBadge(
                number = trackersBlocked,
                modifier = Modifier.constrainAs(numberBadge) {
                    bottom.linkTo(verticalMiddleGuideline)
                    centerAround(horizontalMiddleGuideline)
                }
            )
        }
    }
}

@Preview("ShieldIcon 1x font scale", locale = "en")
@Preview("ShieldIcon 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("ShieldIcon RTL, 1x font scale", locale = "he")
@Composable
fun ShieldIconButtonPreview() {
    val iconModifier = Modifier
        .padding(vertical = Dimensions.PADDING_TINY)
        .padding(start = Dimensions.PADDING_SMALL)

    LightDarkPreviewContainer {
        val testNumbers = listOf(0, 9, 99, 100)
        Row {
            testNumbers.forEach {
                TrackingProtectionButton(
                    showIncognitoBadge = false,
                    trackersBlocked = it,
                    modifier = iconModifier
                ) {}
            }
        }
    }
}

@Preview("IncognitoIconButton 1x font scale", locale = "en")
@Preview("IncognitoIconButton 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("IncognitoIconButton RTL, 1x font scale", locale = "he")
@Composable
fun IncognitoIconButtonPreview() {
    val iconModifier = Modifier
        .padding(vertical = Dimensions.PADDING_TINY)
        .padding(start = Dimensions.PADDING_SMALL)

    LightDarkPreviewContainer {
        val testNumbers = listOf(0, 9, 99, 100)
        Row {
            testNumbers.forEach {
                TrackingProtectionButton(
                    showIncognitoBadge = true,
                    trackersBlocked = it,
                    modifier = iconModifier
                ) {}
            }
        }
    }
}
