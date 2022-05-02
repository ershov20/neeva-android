package com.neeva.app.browsing.urlbar.trackingprotection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.neeva.app.R
import com.neeva.app.browsing.urlbar.trackingprotection.badge.NumberBadge
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun ShieldIconButton(trackersBlocked: Int, modifier: Modifier) {
    IconButton(
        onClick = { /*TODO*/ },
        modifier = modifier
    ) {
        ConstraintLayout {
            val (shieldIcon, numberBadge) = createRefs()
            val middleGuideline = createGuidelineFromStart(0.5f)

            Icon(
                painter = painterResource(R.drawable.ic_shield),
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = stringResource(R.string.tracking_protection),
                modifier = Modifier.fillMaxSize().constrainAs(shieldIcon) {}
            )

            NumberBadge(
                number = trackersBlocked,
                modifier = Modifier
                    .constrainAs(numberBadge) {
                        start.linkTo(middleGuideline)
                        top.linkTo(shieldIcon.top, margin = 8.dp)
                    }
            )
        }
    }
}

@Preview("ShieldIcon 1x font scale", locale = "en")
@Preview("ShieldIcon 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("ShieldIcon RTL, 1x font scale", locale = "he")
@Composable
private fun ShieldIconPreview() {
    val iconModifier = Modifier
        .padding(Dimensions.PADDING_SMALL)
        .size(Dimensions.SIZE_TOUCH_TARGET)

    LightDarkPreviewContainer {
        val testNumbers = listOf(0, 9, 99, 100)
        Row {
            testNumbers.forEach {
                ShieldIconButton(trackersBlocked = it, modifier = iconModifier)
            }
        }
    }
}

@Preview("ShieldIcon RTL, 1x font scale", locale = "he")
@Composable
private fun ShieldIcon_RTL_Preview() {
    val iconModifier = Modifier
        .padding(Dimensions.PADDING_SMALL)
        .size(Dimensions.SIZE_TOUCH_TARGET)
    // TODO(kobec): ask dan why RTL previews require so much extra work
    NeevaTheme {
        val testNumbers = listOf(0, 9, 99, 100)
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            testNumbers.forEach {
                Row {
                    ShieldIconButton(trackersBlocked = it, modifier = iconModifier)
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
