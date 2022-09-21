// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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

        // First run
        FIRST_RUN_IMPRESSION("FirstRunImpression", Category.FIRST_RUN),
        LOGIN_AFTER_FIRST_RUN("LoginAfterFirstRun", Category.FIRST_RUN),
        GET_STARTED_IN_WELCOME("GetStartedInWelcome", Category.FIRST_RUN),
        DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_IMP(
            "DefaultBrowserOnboardingInterstitialImp",
            Category.FIRST_RUN
        ),
        DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_OPEN(
            "DefaultBrowserOnboardingInterstitialOpen",
            Category.FIRST_RUN
        ),
        DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_REMIND(
            "DefaultBrowserOnboardingInterstitialRemind",
            Category.FIRST_RUN
        ),
        SET_DEFAULT_BROWSER("SetDefaultBrowser", Category.FIRST_RUN),
        SKIP_DEFAULT_BROWSER("SkipDefaultBrowser", Category.FIRST_RUN),
        OPEN_DEFAULT_BROWSER_URL("OpenDefaultBrowserURL", Category.FIRST_RUN),

        // Browsing
        BROWSER_PAGE_LOAD("PageLoad", Category.BROWSING)
    }

    enum class Category(val categoryName: String) {
        BROWSING("Browsing"),
        FIRST_RUN("FirstRun"),
        STABILITY("Stability"),
        SUGGESTIONS("Suggestion"),
        UI("UI"),
    }

    companion object {
        fun sessionID(sharedPreferencesModel: SharedPreferencesModel): String {
            var sessionId = SharedPrefFolder.App.SessionIdV2Key.get(sharedPreferencesModel)
            if (sessionId == "") {
                sessionId = UUID.randomUUID().toString()
                SharedPrefFolder.App.SessionIdV2Key.set(
                    sharedPreferencesModel,
                    sessionId
                )
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
