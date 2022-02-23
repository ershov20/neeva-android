package com.neeva.app.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SuggestionSectionHeader(stringRes: Int) {
    Text(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .fillMaxWidth()
            .padding(
                start = 18.dp,
                end = 12.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        text = stringResource(id = stringRes),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(name = "1x scale")
@Preview(name = "2x scale", fontScale = 2.0f)
@Composable
fun SuggestionSectionHeader_Preview() {
    NeevaTheme {
        SuggestionSectionHeader(stringRes = R.string.neeva_search)
    }
}
