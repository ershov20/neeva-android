package com.neeva.app.firstrun.widgets

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.neeva.app.firstrun.widgets.texts.ToggleSignUpText

@Composable
fun StickyFooter(
    scrollState: ScrollState,
    content: @Composable () -> Unit
) {
    Column {
        if (scrollState.maxValue != Int.MAX_VALUE && scrollState.value != scrollState.maxValue) {
            Divider(color = MaterialTheme.colorScheme.outline)
        }

        content()
    }
}

@Composable
fun OnboardingStickyFooter(
    signup: Boolean,
    scrollState: ScrollState,
    stickyFooterOnClick: () -> Unit
) {
    StickyFooter(scrollState) {
        ToggleSignUpText(
            signup = signup,
            onClick = stickyFooterOnClick
        )
    }
}
