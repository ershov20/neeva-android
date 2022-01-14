package com.neeva.app.suggestions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme
import kotlin.math.absoluteValue

@Composable
fun StockSuggestionRow(
    onTapRow: () -> Unit,
    onTapRowContentDescription: String? = null,
    companyName: String?,
    ticker: String?,
    currentPrice: Double?,
    changeFromPreviousClose: Double?,
    percentChangeFromPreviousClose: Double?,
    fetchedAtTime: String?
) {
    val elementSpacing = 4.dp
    val drawableId: Int
    val stockColor: Color
    if ((changeFromPreviousClose ?: 0.0) < 0.0) {
        drawableId = R.drawable.ic_stock_down
        stockColor = Color.Red
    } else {
        drawableId = R.drawable.ic_stock_up
        stockColor = Color(0xFF028961)
    }

    BaseSuggestionRow(
        onTapRow = onTapRow,
        onTapRowContentDescription = onTapRowContentDescription,
        drawableID = drawableId,
        drawableTint = null
    ) { baseModifier ->
        Column(modifier = baseModifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentPrice?.let {
                    Text(
                        text = formatCurrency(it),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }

                changeFromPreviousClose?.let {
                    Text(
                        text = if (it < 0.0) {
                            " -${formatCurrency(it.absoluteValue)}"
                        } else {
                            " +${formatCurrency(it.absoluteValue)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = stockColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier.padding(start = elementSpacing)
                    )
                }

                percentChangeFromPreviousClose?.let {
                    Text(
                        text = if (it < 0.0) {
                            " -(${it.absoluteValue}%)"
                        } else {
                            " +(${it.absoluteValue}%)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = stockColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier.padding(start = elementSpacing)
                    )
                }

                fetchedAtTime?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1.0f).padding(start = elementSpacing),
                        textAlign = TextAlign.End
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                companyName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                ticker?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(start = elementSpacing)
                    )
                }
            }
        }
    }
}

private fun formatCurrency(currencyValue: Double) = String.format("%.2f", currencyValue)

@Preview("Stock down, 1x scale", locale = "en")
@Preview("Stock down, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Stock down, RTL, 1x scale", locale = "he")
@Preview("Stock down, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
fun StockSuggestionRow_PreviewStockDown() {
    NeevaTheme {
        StockSuggestionRow(
            onTapRow = {},
            companyName = "Tesla Inc",
            ticker = "TSLA",
            currentPrice = 643.38,
            changeFromPreviousClose = -5.88,
            percentChangeFromPreviousClose = -0.91,
            fetchedAtTime = "Aug 5, 3:31 PM EDT"
        )
    }
}

@Preview("Stock up, 1x scale", locale = "en")
@Preview("Stock up, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Stock up, RTL, 1x scale", locale = "he")
@Preview("Stock up, RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
fun StockSuggestionRow_PreviewStockUp() {
    NeevaTheme {
        StockSuggestionRow(
            onTapRow = {},
            companyName = "Tesla Inc",
            ticker = "TSLA",
            currentPrice = 643.38,
            changeFromPreviousClose = 0.60,
            percentChangeFromPreviousClose = 0.09,
            fetchedAtTime = "Aug 5, 3:31 PM EDT"
        )
    }
}
