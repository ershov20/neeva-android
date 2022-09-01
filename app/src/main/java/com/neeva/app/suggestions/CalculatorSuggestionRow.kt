// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer

@Composable
fun CalculatorSuggestionRow(
    suggestion: QueryRowSuggestion,
    onTapRow: () -> Unit,
) {
    QueryNavSuggestionRow(
        query = suggestion.query,
        description = suggestion.description,
        drawableID = R.drawable.ic_calculator,
        drawableTint = Color.Unspecified, // The calculator is already colored.
        onTapRow = onTapRow
    )
}

@Preview("LTR, 1x scale", locale = "en")
@Preview("LTR, 2x scale", locale = "en", fontScale = 2.0f)
@Preview("RTL, 1x scale", locale = "he")
@Composable
fun CalculatorSuggestionRow_Preview() {
    LightDarkPreviewContainer {
        CalculatorSuggestionRow(
            suggestion = QueryRowSuggestion(
                url = Uri.parse(""),
                query = "3 + 3",
                description = "6",
                drawableID = R.drawable.ic_public_black_24,
            ),
            onTapRow = {}
        )
    }
}
