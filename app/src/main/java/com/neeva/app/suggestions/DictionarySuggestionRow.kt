package com.neeva.app.suggestions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

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
        drawableID = R.drawable.ic_dictionary,
        drawableTint = null
    ) { baseModifier ->
        Column(modifier = baseModifier) {
            Text(
                text = buildAnnotatedString {
                    withStyle(MaterialTheme.typography.body1.toSpanStyle()) {
                        append(word)
                    }

                    withStyle(
                        MaterialTheme.typography.body2
                            .copy(color = MaterialTheme.colors.onSecondary)
                            .toSpanStyle()
                    ) {
                        phoneticSpelling?.let {
                            append(" | ")
                            append(it)
                        }

                        lexicalCategory?.let {
                            append(" | ")
                            append(it)
                        }

                        Unit
                    }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        MaterialTheme.typography.body2
                            .copy(color = MaterialTheme.colors.onSecondary)
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
}

@Preview("Short word, 1x scale", locale = "en")
@Preview("Short word, 2x scale", locale = "en", fontScale = 2.0f)
@Composable
fun DictionarySuggestionRow_PreviewShortWord() {
    NeevaTheme {
        DictionarySuggestionRow(
            onTapRow = {},
            word = "short",
            shortDefinition = "measuring a small distance from end to end",
            phoneticSpelling = "ʃɔrt",
            lexicalCategory = "adjective"
        )
    }
}

@Preview("Short word, 1x scale", locale = "he")
@Preview("Short word, 2x scale", locale = "he", fontScale = 2.0f)
@Composable
fun DictionarySuggestionRow_PreviewShortHebrewWord() {
    NeevaTheme {
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
fun DictionarySuggestionRow_PreviewMissingFields() {
    NeevaTheme {
        DictionarySuggestionRow(
            onTapRow = {},
            word = "short",
            shortDefinition = "measuring a small distance from end to end",
            phoneticSpelling = null,
            lexicalCategory = null
        )
    }
}

@Preview("Long word, 1x scale")
@Preview("Long word, 2x scale", fontScale = 2.0f)
@Composable
fun DictionarySuggestionRow_PreviewLongWord() {
    NeevaTheme {
        DictionarySuggestionRow(
            onTapRow = {},
            word = "antidisestablishmentarianism",
            shortDefinition = "opposition to the withdrawal of state support or recognition from an established church",
            phoneticSpelling = "ˌæn tiˌdɪs əˌstæb lɪʃ mənˈtɛər i əˌnɪz əm",
            lexicalCategory = "noun"
        )
    }
}
