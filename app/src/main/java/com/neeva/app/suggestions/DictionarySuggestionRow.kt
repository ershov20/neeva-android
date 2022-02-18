package com.neeva.app.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
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
                    withStyle(
                        MaterialTheme.typography.bodyLarge
                            .copy(color = MaterialTheme.colorScheme.onSurface)
                            .toSpanStyle()
                    ) {
                        append(word)
                    }

                    withStyle(
                        MaterialTheme.typography.bodyMedium
                            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

class DictionarySuggestionRowPreviews :
    BooleanPreviewParameterProvider<DictionarySuggestionRowPreviews.Params>(1) {
    data class Params(
        val useDarkTheme: Boolean
    )

    override fun createParams(booleanArray: BooleanArray): Params {
        return Params(
            useDarkTheme = booleanArray[0]
        )
    }

    @Preview("Short word, 1x scale", locale = "en")
    @Preview("Short word, 2x scale", locale = "en", fontScale = 2.0f)
    @Composable
    fun PreviewShortWord(
        @PreviewParameter(DictionarySuggestionRowPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.useDarkTheme) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                DictionarySuggestionRow(
                    onTapRow = {},
                    word = "short",
                    shortDefinition = "measuring a small distance from end to end",
                    phoneticSpelling = "ʃɔrt",
                    lexicalCategory = "adjective"
                )
            }
        }
    }

    @Preview("Short word, 1x scale", locale = "he")
    @Preview("Short word, 2x scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun PreviewShortHebrewWord(
        @PreviewParameter(DictionarySuggestionRowPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.useDarkTheme) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                DictionarySuggestionRow(
                    onTapRow = {},
                    word = "קצר",
                    shortDefinition = "מדידת מרחק קטן מקצה לקצה",
                    phoneticSpelling = "ʃɔrt",
                    lexicalCategory = "תוֹאַר"
                )
            }
        }
    }

    @Preview("Missing fields, 1x scale", locale = "en")
    @Preview("Missing fields, 2x scale", locale = "en", fontScale = 2.0f)
    @Composable
    fun PreviewMissingFields(
        @PreviewParameter(DictionarySuggestionRowPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.useDarkTheme) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                DictionarySuggestionRow(
                    onTapRow = {},
                    word = "short",
                    shortDefinition = "measuring a small distance from end to end",
                    phoneticSpelling = null,
                    lexicalCategory = null
                )
            }
        }
    }

    @Preview("Long word, 1x scale", locale = "en")
    @Preview("Long word, 2x scale", locale = "en", fontScale = 2.0f)
    @Composable
    fun PreviewLongWord(
        @PreviewParameter(DictionarySuggestionRowPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.useDarkTheme) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
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
}
