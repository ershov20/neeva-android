package com.neeva.app.browsing.urlbar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue

class AutocompleteTextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clearButton_withText_appearsAndWorks() {
        var wasCleared = false

        composeTestRule.setContent {
            val fullText = "example.com"
            val typedLength = 4

            NeevaTheme {
                Surface {
                    AutocompleteTextField(
                        textFieldValue = TextFieldValue(
                            text = fullText.take(typedLength),
                            selection = TextRange(typedLength)
                        ),
                        suggestionText = fullText.drop(typedLength),
                        faviconBitmap = null,
                        onLocationEdited = {},
                        onLocationReplaced = { wasCleared = it.isEmpty() },
                        onLoadUrl = {},
                        onAcceptAutocompleteSuggestion = {},
                        placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val clearDescription = targetContext.getString(R.string.clear)

        composeTestRule.onNodeWithContentDescription(clearDescription).apply {
            assertIsDisplayed()
            performClick()
        }
        expectThat(wasCleared).isTrue()
    }

    @Test
    fun clearButton_withNoText_isHidden() {
        composeTestRule.setContent {
            NeevaTheme {
                Surface {
                    AutocompleteTextField(
                        textFieldValue = TextFieldValue(),
                        suggestionText = null,
                        faviconBitmap = null,
                        onLocationEdited = {},
                        onLocationReplaced = {},
                        onLoadUrl = {},
                        onAcceptAutocompleteSuggestion = {},
                        placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val description = targetContext.getString(R.string.clear)
        val placeholder = targetContext.getString(R.string.url_bar_placeholder)

        composeTestRule.onNodeWithContentDescription(description).assertDoesNotExist()
        composeTestRule.onNodeWithText(placeholder).assertIsDisplayed()
    }

    @Test
    fun acceptAutocompleteTapTarget_whenSuggestionVisible_appearsAndWorks() {
        var wasTriggered = false

        composeTestRule.setContent {
            val fullText = "example.com"
            val typedLength = 4

            NeevaTheme {
                Surface {
                    AutocompleteTextField(
                        textFieldValue = TextFieldValue(
                            text = fullText.take(typedLength),
                            selection = TextRange(typedLength)
                        ),
                        suggestionText = fullText.drop(typedLength),
                        faviconBitmap = null,
                        onLocationEdited = {},
                        onLocationReplaced = {},
                        onLoadUrl = {},
                        onAcceptAutocompleteSuggestion = { wasTriggered = true },
                        placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val description = targetContext.getString(
            R.string.url_bar_accept_autocomplete,
            "example.com"
        )

        composeTestRule.onNodeWithText(description).apply {
            assertIsDisplayed()
            performClick()
        }
        expectThat(wasTriggered).isTrue()
    }
}
