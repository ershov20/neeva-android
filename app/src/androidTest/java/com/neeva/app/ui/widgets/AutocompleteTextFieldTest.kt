// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

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
import com.neeva.app.BaseHiltTest
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue

@HiltAndroidTest
class AutocompleteTextFieldTest : BaseHiltTest() {
    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = false, skipNeevaScopeTooltip = true)

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
                        onTextEdited = {},
                        onTextCleared = { wasCleared = true },
                        onSubmitted = {},
                        onAcceptSuggestion = {},
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
                        onTextEdited = {},
                        onTextCleared = {},
                        onSubmitted = {},
                        onAcceptSuggestion = {},
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
                        onTextEdited = {},
                        onTextCleared = {},
                        onSubmitted = {},
                        onAcceptSuggestion = { wasTriggered = true },
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
