package com.neeva.app.ui.widgets.overlay

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OverlaySheet(
    onDismiss: () -> Unit,
    titleResId: Int? = null,
    config: OverlaySheetConfig = OverlaySheetConfig.default,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Spacer(
            modifier = Modifier
                .clickable { onDismiss() }
                .fillMaxWidth()
                .then(
                    when (config.height) {
                        OverlaySheetHeightConfig.HALF_SCREEN -> Modifier.fillMaxHeight(0.5f)
                        OverlaySheetHeightConfig.WRAP_CONTENT -> Modifier.weight(1f)
                    }
                )
                .background(Color.Transparent)
        )

        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(
                topStart = Dimensions.RADIUS_LARGE,
                topEnd = Dimensions.RADIUS_LARGE
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when (config.height) {
                        OverlaySheetHeightConfig.HALF_SCREEN -> Modifier.fillMaxHeight()
                        OverlaySheetHeightConfig.WRAP_CONTENT -> Modifier.wrapContentHeight()
                    }
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BaseRowLayout(
                    endComposable = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_close_24),
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    }
                ) {
                    titleResId?.let { titleResId ->
                        Text(
                            text = stringResource(titleResId),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }
                }
                content()
            }
        }
    }
}
