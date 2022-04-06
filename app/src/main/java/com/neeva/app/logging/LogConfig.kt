package com.neeva.app.logging

class LogConfig {
    enum class Interaction(val interactionName: String) {
        // Search as you type (SAYT)
        QUERY_SUGGESTION("QuerySuggestion"),
        MEMORIZED_SUGGESTION("MemorizedSuggestion"),
        HISTORY_SUGGESTION("HistorySuggestion"),
        AUTOCOMPLETE_SUGGESTION("AutocompleteSuggestion"),
        PERSONAL_SUGGESTION("PersonalSuggestion"),
        BANG_SUGGESTION("BangSuggestion"),
        LENS_SUGGESTION("LensSuggestion"),
        NO_SUGGESTION_QUERY("NoSuggestionQuery"),
        NO_SUGGESTION_URL("NoSuggestionUrl"),
        FIND_ON_PAGE_SUGGESTION("FindOnPageSuggestion"),
        OPEN_SUGGESTED_SEARCH("OpenSuggsetedSearch"),
        OPEN_SUGGESTED_SITE("OpenSuggestedSite"),
        TAB_SUGGESTION("TabSuggestion"),
        EDIT_CURRENT_URL("EditCurrentUrl"),

        // Session
        APP_ENTER_FOREGROUND("AppEnterForeground"),

        // Auth
        AUTH_IMPRESSION_LANDING("AuthLandingImpression"),
        AUTH_IMPRESSION_OTHER("AuthOthersSignUpImpression"),
        AUTH_IMPRESSION_SIGN_IN("AuthSignInImpression"),
        AUTH_SIGN_UP_WITH_GOOGLE("AuthOptionsSignupWithGoogle"),
        AUTH_SIGN_UP_WITH_MICROSOFT("AuthOptionsSignupWithMicrosoft"),
        AUTH_CLOSE("AuthClose"),
        LOGIN_AFTER_FIRST_RUN("LoginAfterFirstRun")
    }

    companion object {
        // Suggestion Attributes
        const val SUGGESTION_POSITION = "SuggestionPosition"
        const val NUMBER_OF_MEMORIZED_SUGGESTIONS = "NumberOfMemorizedSuggestions"
        const val NUMBER_OF_HISTORY_SUGGESTIONS = "NumberOfHistorySuggestions"
        const val NUMBER_OF_PERSONAL_SUGGESTIONS = "NumberOfPersonalSuggestions"
        const val NUMBER_OF_CALCULATOR_ANNOTATIONS = "NumberOfCalculatorAnnotations"
        const val NUMBER_OF_WIKI_ANNOTATIONS = "NumberOfWikiAnnotations"
        const val NUMBER_OF_STOCK_ANNOTATIONS = "NumberOfStockAnnotations"
        const val QUERY_INPUT_FOR_SELECTED_SUGGESTION = "QueryInputForSelectedSuggestion"
        const val QUERY_SUGGESTION_POSITION = "QuerySuggestionPosition"
        const val SELECTED_MEMORIZED_URL_SUGGESTION = "SelectedMemorizedURLSuggestion"
        const val SELECTED_QUERY_SUGGESTION = "SelectedQuerySuggestion"
    }
}
