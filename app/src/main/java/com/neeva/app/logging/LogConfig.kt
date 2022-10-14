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
        REQUEST_INSTALL_REFERRER("RequestInstallReferrer", Category.FIRST_RUN),

        // Browsing
        NAVIGATION_INBOUND("NavigationInbound", Category.BROWSING),
        NAVIGATION_OUTBOUND("NavigationOutbound", Category.BROWSING),
        PREVIEW_SEARCH("PreviewSearch", Category.BROWSING)
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

        // When add/remove a new interaction to this list make sure
        // it's updated on the server side as well
        private val sessionIDAllowList = listOf(
            Interaction.APP_ENTER_FOREGROUND,
            Interaction.AUTH_IMPRESSION_LANDING,
            Interaction.AUTH_IMPRESSION_OTHER,
            Interaction.AUTH_IMPRESSION_SIGN_IN,
            Interaction.AUTH_SIGN_UP_WITH_GOOGLE,
            Interaction.AUTH_SIGN_UP_WITH_MICROSOFT,
            Interaction.AUTH_CLOSE,
            Interaction.FIRST_RUN_IMPRESSION,
            Interaction.LOGIN_AFTER_FIRST_RUN,
            Interaction.GET_STARTED_IN_WELCOME,
            Interaction.DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_IMP,
            Interaction.DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_OPEN,
            Interaction.DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_REMIND,
            Interaction.SET_DEFAULT_BROWSER,
            Interaction.SKIP_DEFAULT_BROWSER,
            Interaction.OPEN_DEFAULT_BROWSER_URL,
            Interaction.REQUEST_INSTALL_REFERRER,
            Interaction.NAVIGATION_INBOUND,
            Interaction.NAVIGATION_OUTBOUND,
            Interaction.PREVIEW_SEARCH
        )

        fun shouldAddSessionID(interaction: Interaction): Boolean {
            return sessionIDAllowList.contains(interaction)
        }
    }

    enum class Attributes(val attributeName: String) {
        SESSION_UUID_V2("SessionUUIDv2")
    }

    enum class FirstRunAttributes(val attributeName: String) {
        REFERRER_RESPONSE("ReferrerResponse"),
        INSTALL_REFERRER("Referrer"),
        REFERRER_TYPE("ReferrerType")
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
