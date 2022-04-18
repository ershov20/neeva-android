package com.neeva.app.firstrun.widgets

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neeva.app.firstrun.ToggleSignUpText

@Composable
fun StickyFooter(
    scrollState: ScrollState,
    content: @Composable () -> Unit
) {
    if (scrollState.value != scrollState.maxValue) {
        Divider(color = MaterialTheme.colorScheme.outline)
    }

    content()

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun OnboardingStickyFooter(scrollState: ScrollState, stickyFooterOnClick: () -> Unit) {
    StickyFooter(scrollState) {
        ToggleSignUpText(true) {
            stickyFooterOnClick()
        }
    }
}
