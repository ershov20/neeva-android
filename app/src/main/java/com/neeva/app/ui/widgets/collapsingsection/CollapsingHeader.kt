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
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.AnimationConstants
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.PortraitPreviews
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
        allowCompactState = true,
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
        allowCompactState = false,
        label = label,
        chevronResourceId = R.drawable.ic_baseline_keyboard_arrow_up_24,
        onClick = onClick
    )
}

@Composable
private fun CollapsingHeader(
    state: CollapsingSectionState,
    allowCompactState: Boolean,
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

    val contentDescription = stringResource(
        when (state.next(allowCompactState)) {
            CollapsingSectionState.COLLAPSED -> R.string.section_collapse
            CollapsingSectionState.COMPACT -> R.string.section_expand
            CollapsingSectionState.EXPANDED -> R.string.section_fully_expand
        },
        label
    )

    BaseRowLayout(
        onTapRow = onClick,
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        verticalPadding = 0.dp,
        endComposable = {
            IconButton(onClick = onClick) {
                Icon(
                    painter = painterResource(chevronResourceId),
                    contentDescription = contentDescription,
                    modifier = Modifier.rotate(rotation.value)
                )
            }
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@PortraitPreviews
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

@PortraitPreviews
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
