package com.neeva.app.ui.widgets.collapsingsection

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.AnimationConstants
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout

enum class CollapsingSectionState {
    COLLAPSED, COMPACT, EXPANDED;

    fun next(allowCompactState: Boolean = false): CollapsingSectionState {
        return when (this) {
            COLLAPSED -> if (allowCompactState) COMPACT else EXPANDED
            COMPACT -> EXPANDED
            EXPANDED -> COLLAPSED
        }
    }

    companion object {
        fun String.toCollapsingSectionState() = valueOf(this)
    }
}

fun MutableState<CollapsingSectionState>.setNextState() {
    this.value = this.value.next()
}

@Composable
fun CollapsingThreeStateHeader(
    label: String,
    state: CollapsingSectionState,
    onClick: () -> Unit
) {
    val chevronResourceId = when (state) {
        CollapsingSectionState.COLLAPSED -> R.drawable.ic_baseline_keyboard_arrow_up_24
        CollapsingSectionState.COMPACT -> R.drawable.ic_keyboard_double_arrow_up_black_24
        CollapsingSectionState.EXPANDED -> R.drawable.ic_baseline_keyboard_arrow_up_24
    }

    CollapsingHeader(
        state = state,
        label = label,
        chevronResourceId = chevronResourceId,
        onClick = onClick
    )
}

@Composable
fun CollapsingTwoStateHeader(
    label: String,
    state: CollapsingSectionState,
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
    state: CollapsingSectionState,
    label: String,
    chevronResourceId: Int,
    onClick: () -> Unit
) {
    val transition = updateTransition(targetState = state, "mode switch")
    val rotation = transition.animateFloat(
        transitionSpec = { tween(AnimationConstants.ANIMATION_DURATION_MS) },
        label = "mode switch rotation"
    ) {
        when (it) {
            CollapsingSectionState.COLLAPSED -> 180f
            CollapsingSectionState.COMPACT -> 180f
            CollapsingSectionState.EXPANDED -> 0f
        }
    }

    BaseRowLayout(
        onTapRow = onClick,
        backgroundColor = MaterialTheme.colorScheme.background,
        applyVerticalPadding = false,
        endComposable = {
            IconButton(onClick = onClick) {
                Icon(
                    painter = painterResource(chevronResourceId),
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation.value)
                )
            }
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview("CollapsingHeaderPreviews 2 states, 1x font scale", locale = "en")
@Preview("CollapsingHeaderPreviews 2 states, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("CollapsingHeaderPreviews 2 states, RTL, 1x font scale", locale = "he")
@Composable
fun CollapsingHeaderPreviews_TwoStates() {
    OneBooleanPreviewContainer { useLongLabel ->
        val label = if (useLongLabel) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            "Section header"
        }

        val state = remember { mutableStateOf(CollapsingSectionState.EXPANDED) }
        CollapsingTwoStateHeader(label = label, state = state.value) {
            state.value = state.value.next(false)
        }
    }
}

@Preview("CollapsingHeaderPreviews 3 states, 1x font scale", locale = "en")
@Preview("CollapsingHeaderPreviews 3 states, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("CollapsingHeaderPreviews 3 states, RTL, 1x font scale", locale = "he")
@Composable
fun CollapsingHeaderPreviews_ThreeStates() {
    OneBooleanPreviewContainer { useLongLabel ->
        val label = if (useLongLabel) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            "Section header"
        }

        val state = remember { mutableStateOf(CollapsingSectionState.EXPANDED) }
        CollapsingThreeStateHeader(label = label, state = state.value) {
            state.value = state.value.next(true)
        }
    }
}
