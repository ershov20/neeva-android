package com.neeva.app.suggestions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.widgets.SplitStringRow

@Composable
fun DictionarySuggestionRow(
    onTapRow: () -> Unit,
    onTapRowContentDescription: String? = null,
    word: String,
    shortDefinition: String,
    phoneticSpelling: String?,
    lexicalCategory: String?
) {
    BaseSuggestionRow(
        onTapRow = onTapRow,
        onTapRowContentDescription = onTapRowContentDescription,
        iconParams = SuggestionRowIconParams(
            drawableID = R.drawable.ic_dictionary
        )
    ) {
        SplitStringRow(
            primary = word,
            secondaryPieces = mutableListOf<String>().apply {
                phoneticSpelling?.let { add(it) }
                lexicalCategory?.let { add(it) }
            },
            separator = "|",
            primaryStyle = MaterialTheme.typography.bodyLarge,
            secondaryStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = buildAnnotatedString {
                withStyle(
                    MaterialTheme.typography.bodyMedium
                        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        .toSpanStyle()
                ) {
                    append(shortDefinition)
                }
            },
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

class DictionarySuggestionRowPreviews {
    @Preview("Short word LTR, 1x scale", locale = "en")
    @Preview("Short word LTR, 2x scale", locale = "en", fontScale = 2.0f)
    @Composable
    fun PreviewShortWord() {
        LightDarkPreviewContainer {
            DictionarySuggestionRow(
                onTapRow = {},
                word = "short",
                shortDefinition = "measuring a small distance from end to end",
                phoneticSpelling = "ʃɔrt",
                lexicalCategory = "adjective"
            )
        }
    }

    @Preview("Short word RTL, 1x scale", locale = "he")
    @Preview("Short word RTL, 2x scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun PreviewShortHebrewWord() {
        LightDarkPreviewContainer {
            DictionarySuggestionRow(
                onTapRow = {},
                word = "קצר",
                shortDefinition = "מדידת מרחק קטן מקצה לקצה",
                phoneticSpelling = "ʃɔrt",
                lexicalCategory = "תוֹאַר"
            )
        }
    }

    @Preview("Missing fields, 1x scale", locale = "en")
    @Preview("Missing fields, 2x scale", locale = "en", fontScale = 2.0f)
    @Composable
    fun PreviewMissingFields() {
        LightDarkPreviewContainer {
            DictionarySuggestionRow(
                onTapRow = {},
                word = "short",
                shortDefinition = "measuring a small distance from end to end",
                phoneticSpelling = null,
                lexicalCategory = null
            )
        }
    }

    @Preview("Long word, 1x scale", locale = "en")
    @Preview("Long word, 2x scale", locale = "en", fontScale = 2.0f)
    @Composable
    fun PreviewLongWord() {
        LightDarkPreviewContainer {
            DictionarySuggestionRow(
                onTapRow = {},
                word = "antidisestablishmentarianism",
                shortDefinition = "opposition to the withdrawal of state support or " +
                    "recognition from an established church",
                phoneticSpelling = "ˌæn tiˌdɪs əˌstæb lɪʃ mənˈtɛər i əˌnɪz əm",
                lexicalCategory = "noun"
            )
        }
    }
}
