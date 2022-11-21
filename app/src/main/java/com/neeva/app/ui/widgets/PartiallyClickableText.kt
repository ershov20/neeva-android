// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import com.neeva.app.R

/**
 * A Compose-style API for building a [ClickableText] element with one or more clickable spans.
 *
 * Non-clickable sections can be added with [Span] elements, which can take unstyled [Text] or
 * an [AnnotatedString] if custom styling should be added.
 *
 * Clickable sections can be added with [ClickableSpan] elements, which take a string and a
 * callback lambda.  ClickableSpan can take either unstyled [Text], which will have the linkStyle
 * applied, or an [AnnotatedString], in which case no additional styling will be added.
 *
 * @param baseStyle a [TextStyle] that applies to all unstyled text.
 * @param linkStyle a [SpanStyle] for clickable sections of text.
 * @param defaultSeparator a whitespace or other character that will be inserted between spans.
 * [Span] and [ClickableSpan] elements can accept a [separator] param to override this for the
 * preceding join.
 * @param modifier a [Modifier] that will be passed to the resulting ClickableText element.
 * @param builder the builder lambda.  Note that a ClickableStringBuilder instance is the receiver
 * of this lambda, so unlike true compose elements, [Span] and [ClickableSpan] must be direct
 * children.
 * @return the string data associated with the resource
 */
@Composable
inline fun PartiallyClickableText(
    // ClickableText uses BasicText, which does not pull color, so we specifically set it here.
    baseStyle: TextStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),

    // Applies reasonable defaults (underline and highlighted color from theme)
    linkStyle: SpanStyle = baseStyle.toSpanStyle().copy(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline
    ),
    defaultSeparator: String? = stringResource(id = R.string.sentence_separator),
    modifier: Modifier = Modifier,
    builder: (ClickableStringBuilder).() -> Unit
) {

    // Applies a typical link style if linkStyle is not specified
    ClickableStringBuilder(baseStyle.toSpanStyle(), linkStyle, defaultSeparator)
        .apply(builder)
        .Build(modifier)
}

class ClickableStringBuilder(
    baseStyle: SpanStyle,
    val linkStyle: SpanStyle,
    val defaultSeparator: String?
) {
    private val annotatedStringBuilder = AnnotatedString.Builder()
    private val clickActions = mutableMapOf<String, () -> Unit>()

    init {
        annotatedStringBuilder.pushStyle(baseStyle)
    }

    private fun addSeparator(separator: String?) {
        if (separator != null && annotatedStringBuilder.length > 0) {
            annotatedStringBuilder.append(separator)
        }
    }

    /**
     * Adds a non-clickable span of text with the default style
     */
    @Composable
    fun Span(text: String, separator: String? = defaultSeparator) {
        addSeparator(separator)
        annotatedStringBuilder.append(text)
    }

    /**
     * Adds a non-clickable span of text with custom style
     */
    @Composable
    fun Span(text: AnnotatedString, separator: String? = defaultSeparator) {
        addSeparator(separator)
        annotatedStringBuilder.append(text)
    }

    /**
     * Adds a clikable span of text. It will apply the linkStyle if provided
     * If no linkStyle was provided, typical link styling will be applied.
     */
    @Composable
    @OptIn(ExperimentalTextApi::class)
    fun ClickableSpan(text: String, separator: String? = defaultSeparator, onClick: () -> Unit) {
        addSeparator(separator)
        val tag = getUniqueHashedTag(text)
        clickActions.put(tag, onClick)

        annotatedStringBuilder.append(
            buildAnnotatedString {
                withStyle(linkStyle) {
                    withAnnotation(tag = tag, annotation = ANNOTATION) {
                        append(text)
                    }
                }
            }
        )
    }

    /**
     * Adds a clickable span of text with custom styling.
     * Will not apply the linkStyle.
     */
    @OptIn(ExperimentalTextApi::class)
    @Composable
    fun ClickableSpan(
        text: AnnotatedString,
        separator: String? = defaultSeparator,
        onClick: () -> Unit
    ) {
        addSeparator(separator)
        val tag = getUniqueHashedTag(text)
        clickActions.put(tag, onClick)

        annotatedStringBuilder.append(
            buildAnnotatedString {
                withAnnotation(tag = tag, annotation = ANNOTATION) {
                    append(text)
                }
            }
        )
    }

    @Composable
    fun Build(modifier: Modifier) {
        val annotated = annotatedStringBuilder.toAnnotatedString()

        ClickableText(annotated, style = LocalTextStyle.current, modifier = modifier) { offset ->
            annotated.getStringAnnotations(offset, offset).forEach { annotation ->
                clickActions[annotation.tag]?.invoke()
            }
        }
    }

    /**
     * Generates a unique string to use for the tag for string annotation.
     * The current clickActions.size is hashed into the tag to avoid hash collisions when
     * the same base string appears multiple times.
     */
    private fun getUniqueHashedTag(content: Any): String {
        return(content to clickActions.size).hashCode().toString(32)
    }

    companion object {
        const val ANNOTATION = "Clickable"
    }
}
