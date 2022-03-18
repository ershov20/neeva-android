package com.neeva.app.widgets.collapsible

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.AnimationConstants
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

enum class CollapsingSectionState {
    COLLAPSED, COMPACT, EXPANDED;

    fun next(allowCompactState: Boolean = false): CollapsingSectionState {
        return when (this) {
            COLLAPSED -> if (allowCompactState) COMPACT else EXPANDED
            COMPACT -> EXPANDED
            EXPANDED -> COLLAPSED
        }
    }
}

fun MutableState<CollapsingSectionState>.setNextState() {
    this.value = this.value.next()
}

@Composable
fun CollapsibleThreeStateHeader(
    label: String,
    state: State<CollapsingSectionState>,
    onClick: () -> Unit
) {
    val chevronResourceId = when (state.value) {
        CollapsingSectionState.COLLAPSED -> R.drawable.ic_baseline_keyboard_arrow_up_24
        CollapsingSectionState.COMPACT -> R.drawable.ic_baseline_keyboard_arrow_up_24
        CollapsingSectionState.EXPANDED -> R.drawable.ic_keyboard_double_arrow_up_black_24
    }

    CollapsingHeader(
        state = state,
        label = label,
        chevronResourceId = chevronResourceId,
        onClick = onClick
    )
}

@Composable
fun CollapsibleTwoStateHeader(
    label: String,
    state: State<CollapsingSectionState>,
    onClick: () -> Unit
) {
    CollapsingHeader(
        state = state,
        label = label,
        chevronResourceId = R.drawable.ic_baseline_keyboard_arrow_up_24,
        onClick = onClick
    )
}

@Composable
private fun CollapsingHeader(
    state: State<CollapsingSectionState>,
    label: String,
    chevronResourceId: Int,
    onClick: () -> Unit
) {
    val transition = updateTransition(targetState = state.value, "mode switch")
    val rotation = transition.animateFloat(
        transitionSpec = { tween(AnimationConstants.ANIMATION_DURATION_MS) },
        label = "mode switch rotation"
    ) {
        when (it) {
            CollapsingSectionState.COLLAPSED -> 0f
            CollapsingSectionState.COMPACT -> 180f
            CollapsingSectionState.EXPANDED -> 180f
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(1.0f)
                .padding(horizontal = Dimensions.PADDING_LARGE),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )

        Icon(
            painter = painterResource(chevronResourceId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(Dimensions.PADDING_SMALL)
                .size(32.dp)
                .rotate(rotation.value)
        )
    }
}

@Preview("CollapsingHeaderPreviews, 1x font scale", locale = "en")
@Preview("CollapsingHeaderPreviews, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("CollapsingHeaderPreviews, RTL, 1x font scale", locale = "he")
@Composable
fun CollapsingHeaderPreviews_TwoStates() {
    OneBooleanPreviewContainer { useLongLabel ->
        val label = if (useLongLabel) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            "Section header"
        }

        val state = remember { mutableStateOf(CollapsingSectionState.EXPANDED) }
        CollapsibleTwoStateHeader(label = label, state = state) {
            state.value = state.value.next(false)
        }
    }
}

@Preview("CollapsingHeaderPreviews, 1x font scale", locale = "en")
@Preview("CollapsingHeaderPreviews, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("CollapsingHeaderPreviews, RTL, 1x font scale", locale = "he")
@Composable
fun CollapsingHeaderPreviews_ThreeStates() {
    OneBooleanPreviewContainer { useLongLabel ->
        val label = if (useLongLabel) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            "Section header"
        }

        val state = remember { mutableStateOf(CollapsingSectionState.EXPANDED) }
        CollapsibleThreeStateHeader(label = label, state = state) {
            state.value = state.value.next(false)
        }
    }
}
