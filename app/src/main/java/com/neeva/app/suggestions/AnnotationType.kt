package com.neeva.app.suggestions

/**
 * All the different types of annotations that apply to a [SuggestionsQuery.QuerySuggestion].
 *
 * Must be kept in sync with the latest version in the main repository:
 * https://github.com/neevaco/neeva/blob/dc84abf8d65745ffc9524bed67409d9e4f637233/suggest/schema.go
 */
enum class AnnotationType(val value: String) {
    Default(""),
    Calculator("Calculator"),
    Wikipedia("Wikipedia"),
    Stock("Stock"),
    Dictionary("Dictionary"),
    Contact("Contact");

    companion object {
        fun fromString(value: String?): AnnotationType {
            return values().firstOrNull { it.value == value } ?: Default
        }
    }
}
