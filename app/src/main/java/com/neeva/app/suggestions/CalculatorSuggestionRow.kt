package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun CalculatorSuggestionRow(
    suggestion: QueryRowSuggestion,
    onTapRow: () -> Unit,
) {
    QueryNavSuggestionRow(
        query = suggestion.query,
        description = suggestion.description,
        drawableID = R.drawable.ic_calculator,
        drawableTint = null,  // The calculator is already colored.
        onTapRow = onTapRow
    )
}

@Preview("1x scale")
@Preview("2x scale", fontScale = 2.0f)
@Preview("RTL, 1x scale", locale = "he")
@Preview("RTL, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
fun CalculatorSuggestionRow_Preview() {
    NeevaTheme {
        CalculatorSuggestionRow(
            suggestion = QueryRowSuggestion(
                url = Uri.parse(""),
                query = "3 + 3",
                description = "6",
                drawableID = R.drawable.globe,
            ),
            onTapRow = {}
        )
    }
}