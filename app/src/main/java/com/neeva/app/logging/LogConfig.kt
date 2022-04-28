package com.neeva.app.logging

import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import java.util.UUID

class LogConfig {
    enum class Interaction(val interactionName: String, val category: Category) {
        // Search as you type (SAYT)
        QUERY_SUGGESTION("QuerySuggestion", Category.SUGGESTIONS),
        MEMORIZED_SUGGESTION("MemorizedSuggestion", Category.SUGGESTIONS),
        HISTORY_SUGGESTION("HistorySuggestion", Category.SUGGESTIONS),
        AUTOCOMPLETE_SUGGESTION("AutocompleteSuggestion", Category.SUGGESTIONS),
        PERSONAL_SUGGESTION("PersonalSuggestion", Category.SUGGESTIONS),
        BANG_SUGGESTION("BangSuggestion", Category.SUGGESTIONS),
        LENS_SUGGESTION("LensSuggestion", Category.SUGGESTIONS),
        NO_SUGGESTION_QUERY("NoSuggestionQuery", Category.SUGGESTIONS),
        NO_SUGGESTION_URL("NoSuggestionUrl", Category.SUGGESTIONS),
        FIND_ON_PAGE_SUGGESTION("FindOnPageSuggestion", Category.SUGGESTIONS),
        OPEN_SUGGESTED_SEARCH("OpenSuggsetedSearch", Category.SUGGESTIONS),
        OPEN_SUGGESTED_SITE("OpenSuggestedSite", Category.SUGGESTIONS),
        TAB_SUGGESTION("TabSuggestion", Category.SUGGESTIONS),
        EDIT_CURRENT_URL("EditCurrentUrl", Category.SUGGESTIONS),

        // Session
        APP_ENTER_FOREGROUND("AppEnterForeground", Category.STABILITY),

        // Auth
        AUTH_IMPRESSION_LANDING("AuthLandingImpression", Category.FIRST_RUN),
        AUTH_IMPRESSION_OTHER("AuthOthersSignUpImpression", Category.FIRST_RUN),
        AUTH_IMPRESSION_SIGN_IN("AuthSignInImpression", Category.FIRST_RUN),
        AUTH_SIGN_UP_WITH_GOOGLE("AuthOptionsSignupWithGoogle", Category.FIRST_RUN),
        AUTH_SIGN_UP_WITH_MICROSOFT("AuthOptionsSignupWithMicrosoft", Category.FIRST_RUN),
        AUTH_CLOSE("AuthClose", Category.FIRST_RUN),
        FIRST_RUN_IMPRESSION("FirstRunImpression", Category.FIRST_RUN),
        LOGIN_AFTER_FIRST_RUN("LoginAfterFirstRun", Category.FIRST_RUN)
    }

    enum class Category(val categoryName: String) {
        UI("UI"),
        SUGGESTIONS("Suggestion"),
        STABILITY("Stability"),
        FIRST_RUN("FirstRun")
    }

    companion object {
        private const val SESSION_ID_V2_KEY = "SESSION_ID_V2"

        fun sessionID(sharedPreferencesModel: SharedPreferencesModel): String {
            var sessionId = sharedPreferencesModel.getValue(
                SharedPrefFolder.APP,
                SESSION_ID_V2_KEY,
                ""
            )
            if (sessionId == "") {
                sessionId = UUID.randomUUID().toString()
                sharedPreferencesModel.setValue(SharedPrefFolder.APP, SESSION_ID_V2_KEY, sessionId)
            }
            return sessionId
        }
    }

    enum class Attributes(val attributeName: String) {
        SESSION_UUID_V2("SessionUUIDv2")
    }

    enum class SuggestionAttributes(val attributeName: String) {
        SUGGESTION_POSITION("SuggestionPosition"),
        NUMBER_OF_MEMORIZED_SUGGESTIONS("NumberOfMemorizedSuggestions"),
        NUMBER_OF_HISTORY_SUGGESTIONS("NumberOfHistorySuggestions"),
        NUMBER_OF_PERSONAL_SUGGESTIONS("NumberOfPersonalSuggestions"),
        NUMBER_OF_CALCULATOR_ANNOTATIONS("NumberOfCalculatorAnnotations"),
        NUMBER_OF_WIKI_ANNOTATIONS("NumberOfWikiAnnotations"),
        NUMBER_OF_STOCK_ANNOTATIONS("NumberOfStockAnnotations"),
        QUERY_INPUT_FOR_SELECTED_SUGGESTION("QueryInputForSelectedSuggestion"),
        QUERY_SUGGESTION_POSITION("QuerySuggestionPosition"),
        SELECTED_MEMORIZED_URL_SUGGESTION("SelectedMemorizedURLSuggestion"),
        SELECTED_QUERY_SUGGESTION("SelectedQuerySuggestion")
    }
}
