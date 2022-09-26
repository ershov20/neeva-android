// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.SuggestionsQuery
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.storage.entities.Site
import com.neeva.app.suggestions.NavSuggestion
import com.neeva.app.suggestions.Suggestions
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.suggestions.toNavSuggestion
import com.neeva.app.suggestions.toQueryRowSuggestion
import com.neeva.app.type.QuerySuggestionSource
import com.neeva.app.type.QuerySuggestionType
import com.neeva.app.userdata.LoginToken
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isTrue

/**
 * Tests that the [SuggestionsModel] triggers network queries for suggestions via Apollo and that
 * it processes them correctly.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionsModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var apolloWrapper: TestAuthenticatedApolloWrapper
    private lateinit var clientLogger: ClientLogger
    private lateinit var historyManager: HistoryManager
    private lateinit var model: SuggestionsModel
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var loginToken: LoginToken
    private lateinit var siteSuggestions: MutableStateFlow<List<NavSuggestion>>
    private lateinit var urlBarIsEditing: MutableStateFlow<Boolean>
    private lateinit var urlBarText: MutableStateFlow<TextFieldValue>
    private lateinit var testDispatcher: Dispatchers

    @Mock lateinit var domainProvider: DomainProvider

    override fun setUp() {
        super.setUp()
        testDispatcher = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )
        siteSuggestions = MutableStateFlow(emptyList())
        urlBarText = MutableStateFlow(TextFieldValue(""))
        urlBarIsEditing = MutableStateFlow(false)

        historyManager = mock()
        Mockito.`when`(historyManager.historySuggestions).thenReturn(siteSuggestions)

        neevaConstants = NeevaConstants()
        loginToken = mock()
        apolloWrapper = TestAuthenticatedApolloWrapper(loginToken, neevaConstants)

        val settingsDataModel = mock<SettingsDataModel> {
            on {
                getSettingsToggleValue(any())
            } doReturn true
        }

        clientLogger = mock()

        model = SuggestionsModel(
            coroutineScope = coroutineScopeRule.scope,
            historyManager = historyManager,
            settingsDataModel = settingsDataModel,
            apolloWrapper = apolloWrapper,
            dispatchers = testDispatcher,
            neevaConstants = neevaConstants,
            clientLogger = clientLogger
        )

        coroutineScopeRule.scope.advanceUntilIdle()
    }

    override fun tearDown() {
        super.tearDown()
        expectThat(coroutineScopeRule.scope.isActive).isTrue()
    }

    @Test
    fun init_collectsHistorySuggestions() {
        expectThat(model.autocompleteSuggestionFlow.value).isNull()
        expectThat(model.suggestionFlow.value).isEqualTo(Suggestions())

        siteSuggestions.value = listOf(
            Site(
                siteURL = "https://www.reddit.com/r/android",
                title = null,
                largestFavicon = null
            ).toNavSuggestion(domainProvider),
            Site(
                siteURL = "https://www.ignored.url",
                title = null,
                largestFavicon = null
            ).toNavSuggestion(domainProvider)
        )

        coroutineScopeRule.scope.advanceUntilIdle()

        val navSuggestions = siteSuggestions.value
        expectThat(model.autocompleteSuggestionFlow.value).isEqualTo(navSuggestions.first())
        expectThat(model.suggestionFlow.value).isEqualTo(
            Suggestions(autocompleteSuggestion = navSuggestions.first())
        )
    }

    @Test
    fun onUrlBarChanged_withEmptyString_doesNoQuery() {
        apolloWrapper.registerTestResponse(
            SuggestionsQuery(query = ""),
            FULL_RESPONSE_DATA
        )

        coroutineScopeRule.scope.advanceUntilIdle()
        runBlocking {
            model.getSuggestionsFromBackend("")
        }
        expectThat(model.suggestionFlow.value.queryRowSuggestions).isEmpty()
        expectThat(model.suggestionFlow.value.navSuggestions).isEmpty()
    }

    @Test
    fun onUrlBarChanged_withNonEmptyString_firesRequestAndProcessesResult() {
        apolloWrapper.registerTestResponse(
            SuggestionsQuery(query = "query text"),
            FULL_RESPONSE_DATA
        )

        // Set the autocomplete suggestion.
        siteSuggestions.value = listOf(
            Site(
                siteURL = "https://www.reddit.com/r/android",
                title = null,
                largestFavicon = null
            ).toNavSuggestion(domainProvider)
        )
        coroutineScopeRule.scope.advanceUntilIdle()

        // Make sure the autocomplete suggestion has been set and nothing else has.
        val expectedSuggestion = siteSuggestions.value.first()
        expectThat(model.autocompleteSuggestionFlow.value).isEqualTo(expectedSuggestion)
        expectThat(model.suggestionFlow.value).isEqualTo(
            Suggestions(autocompleteSuggestion = expectedSuggestion)
        )

        // Trigger the Apollo query and check the results.
        runBlocking {
            model.getSuggestionsFromBackend("query text")
        }
        coroutineScopeRule.scope.advanceUntilIdle()

        val emptyAnnotation = SuggestionsQuery.Annotation(null, null, null, null, null)
        expectThat(model.suggestionFlow.value.queryRowSuggestions).containsExactly(
            listOf(
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion(neevaConstants),
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit nfl streams",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion(neevaConstants),
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit news",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion(neevaConstants),
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit.com",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion(neevaConstants),
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit cfb",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion(neevaConstants)
            )
        )

        expectThat(model.suggestionFlow.value.navSuggestions).containsExactly(
            listOf(
                SuggestionsQuery.UrlSuggestion(
                    icon = SuggestionsQuery.Icon(null),
                    suggestedURL = "https://www.reddit.com/",
                    title = "https://www.reddit.com/",
                    author = "",
                    timestamp = null,
                    subtitle = "reddit: the front page of the internet",
                    sourceQueryIndex = 0,
                    boldSpan = listOf(SuggestionsQuery.BoldSpan1(12, 17))
                ).toNavSuggestion(),
                SuggestionsQuery.UrlSuggestion(
                    icon = SuggestionsQuery.Icon(null),
                    suggestedURL = "https://nflthursday.com/reddit-nfl-streams/",
                    title = "https://nflthursday.com/reddit-nfl-streams/",
                    author = "",
                    timestamp = null,
                    subtitle = "Reddit NFL streams is banned - How to watch this weeks ...",
                    sourceQueryIndex = 1,
                    boldSpan = listOf(SuggestionsQuery.BoldSpan1(24, 29))
                ).toNavSuggestion(),
                SuggestionsQuery.UrlSuggestion(
                    icon = SuggestionsQuery.Icon(null),
                    suggestedURL = "https://www.reddit.com/r/news/",
                    title = "https://www.reddit.com/r/news/",
                    author = "",
                    timestamp = null,
                    subtitle = "News - reddit",
                    sourceQueryIndex = 2,
                    boldSpan = listOf(SuggestionsQuery.BoldSpan1(12, 17))
                ).toNavSuggestion(),
                SuggestionsQuery.UrlSuggestion(
                    icon = SuggestionsQuery.Icon(null),
                    suggestedURL = "https://www.reddit.com/r/cfb",
                    title = "https://www.reddit.com/r/cfb",
                    author = "",
                    timestamp = null,
                    subtitle = "r/CFB - Reddit",
                    sourceQueryIndex = 4,
                    boldSpan = listOf(SuggestionsQuery.BoldSpan1(12, 17))
                ).toNavSuggestion(),
            )
        )

        // Make sure that autocomplete suggestion was retained
        expectThat(model.autocompleteSuggestionFlow.value).isEqualTo(expectedSuggestion)
        expectThat(model.suggestionFlow.value.autocompleteSuggestion).isEqualTo(expectedSuggestion)
    }

    companion object {
        val FULL_RESPONSE_DATA = SuggestionsQuery.Data(
            suggest = SuggestionsQuery.Suggest(
                querySuggestion = listOf(
                    SuggestionsQuery.QuerySuggestion(
                        type = QuerySuggestionType.Standard,
                        suggestedQuery = "reddit",
                        boldSpan = listOf(
                            SuggestionsQuery.BoldSpan(
                                startInclusive = 0,
                                endExclusive = 5
                            )
                        ),
                        source = QuerySuggestionSource.Bing,
                        annotation = SuggestionsQuery.Annotation(
                            annotationType = null,
                            description = null,
                            imageURL = null,
                            stockInfo = null,
                            dictionaryInfo = null
                        )
                    ),
                    SuggestionsQuery.QuerySuggestion(
                        type = QuerySuggestionType.Standard,
                        suggestedQuery = "reddit nfl streams",
                        boldSpan = listOf(
                            SuggestionsQuery.BoldSpan(
                                startInclusive = 0,
                                endExclusive = 5
                            )
                        ),
                        source = QuerySuggestionSource.Bing,
                        annotation = SuggestionsQuery.Annotation(
                            annotationType = null,
                            description = null,
                            imageURL = null,
                            stockInfo = null,
                            dictionaryInfo = null
                        )
                    ),
                    SuggestionsQuery.QuerySuggestion(
                        type = QuerySuggestionType.Standard,
                        suggestedQuery = "reddit news",
                        boldSpan = listOf(
                            SuggestionsQuery.BoldSpan(
                                startInclusive = 0,
                                endExclusive = 5
                            )
                        ),
                        source = QuerySuggestionSource.Bing,
                        annotation = SuggestionsQuery.Annotation(
                            annotationType = null,
                            description = null,
                            imageURL = null,
                            stockInfo = null,
                            dictionaryInfo = null
                        )
                    ),
                    SuggestionsQuery.QuerySuggestion(
                        type = QuerySuggestionType.Standard,
                        suggestedQuery = "reddit.com",
                        boldSpan = listOf(
                            SuggestionsQuery.BoldSpan(
                                startInclusive = 0,
                                endExclusive = 5
                            )
                        ),
                        source = QuerySuggestionSource.Bing,
                        annotation = SuggestionsQuery.Annotation(
                            annotationType = null,
                            description = null,
                            imageURL = null,
                            stockInfo = null,
                            dictionaryInfo = null
                        )
                    ),
                    SuggestionsQuery.QuerySuggestion(
                        type = QuerySuggestionType.Standard,
                        suggestedQuery = "reddit cfb",
                        boldSpan = listOf(
                            SuggestionsQuery.BoldSpan(
                                startInclusive = 0,
                                endExclusive = 5
                            )
                        ),
                        source = QuerySuggestionSource.Bing,
                        annotation = SuggestionsQuery.Annotation(
                            annotationType = null,
                            description = null,
                            imageURL = null,
                            stockInfo = null,
                            dictionaryInfo = null
                        )
                    )
                ),
                urlSuggestion = listOf(
                    SuggestionsQuery.UrlSuggestion(
                        icon = SuggestionsQuery.Icon(labels = null),
                        suggestedURL = "https://www.reddit.com/",
                        title = "https://www.reddit.com/",
                        author = "",
                        timestamp = null,
                        subtitle = "reddit: the front page of the internet",
                        sourceQueryIndex = 0,
                        boldSpan = listOf(
                            SuggestionsQuery.BoldSpan1(
                                startInclusive = 12,
                                endExclusive = 17
                            )
                        )
                    ),
                    SuggestionsQuery.UrlSuggestion(
                        icon = SuggestionsQuery.Icon(labels = null),
                        suggestedURL = "https://nflthursday.com/reddit-nfl-streams/",
                        title = "https://nflthursday.com/reddit-nfl-streams/",
                        author = "",
                        timestamp = null,
                        subtitle = "Reddit NFL streams is banned - How to watch this weeks ...",
                        sourceQueryIndex = 1,
                        boldSpan = listOf(
                            SuggestionsQuery.BoldSpan1(
                                startInclusive = 24,
                                endExclusive = 29
                            )
                        )
                    ),
                    SuggestionsQuery.UrlSuggestion(
                        icon = SuggestionsQuery.Icon(labels = null),
                        suggestedURL = "https://www.reddit.com/r/news/",
                        title = "https://www.reddit.com/r/news/",
                        author = "",
                        timestamp = null,
                        subtitle = "News - reddit",
                        sourceQueryIndex = 2,
                        boldSpan = listOf(
                            SuggestionsQuery.BoldSpan1(
                                startInclusive = 12,
                                endExclusive = 17
                            )
                        )
                    ),
                    SuggestionsQuery.UrlSuggestion(
                        icon = SuggestionsQuery.Icon(labels = null),
                        suggestedURL = "https://www.reddit.com/r/cfb",
                        title = "https://www.reddit.com/r/cfb",
                        author = "",
                        timestamp = null,
                        subtitle = "r/CFB - Reddit",
                        sourceQueryIndex = 4,
                        boldSpan = listOf(
                            SuggestionsQuery.BoldSpan1(
                                startInclusive = 12,
                                endExclusive = 17
                            )
                        )
                    )
                ),
                lenseSuggestion = null,
                bangSuggestion = null,
                activeLensBangInfo = null
            )
        )
    }
}
