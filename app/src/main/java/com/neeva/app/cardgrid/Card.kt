package com.neeva.app.cardgrid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.neeva.app.ui.theme.Dimensions

@Composable
fun Card(
    label: String,
    onSelect: () -> Unit,
    labelStartComposable: @Composable (() -> Unit)? = null,
    labelEndComposable: @Composable (() -> Unit)? = null,
    topContent: @Composable () -> Unit,
) {
    Surface(modifier = Modifier.clickable { onSelect() }) {
        Column(
            modifier = Modifier.padding(Dimensions.PADDING_SMALL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(MINIMUM_CARD_CONTENT_HEIGHT)) {
                topContent()
            }

            Spacer(Modifier.height(Dimensions.PADDING_SMALL))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
            ) {
                if (labelStartComposable != null) {
                    labelStartComposable()
                    Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (labelEndComposable != null) {
                    Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                    labelEndComposable()
                }
            }
        }
    }
}
