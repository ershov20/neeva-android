package com.neeva.app.browsing

import androidx.compose.ui.text.input.TextFieldValue
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.SuggestionsQuery
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.entities.Site
import com.neeva.app.suggestions.Suggestions
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.suggestions.toNavSuggestion
import com.neeva.app.suggestions.toQueryRowSuggestion
import com.neeva.app.type.QuerySuggestionSource
import com.neeva.app.type.QuerySuggestionType
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
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
 *
 * TODO(dan.alcantara): Figure out how to get the collections in the SuggestionsModel.init to work
 *                      correctly in a testing environment without hanging the main thread.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionsModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock lateinit var domainProvider: DomainProvider

    private lateinit var siteSuggestions: MutableStateFlow<List<Site>>
    private lateinit var urlBarText: MutableStateFlow<TextFieldValue>
    private lateinit var urlBarIsEditing: MutableStateFlow<Boolean>
    private lateinit var responseData: String

    private lateinit var historyManager: HistoryManager
    private lateinit var apolloClient: ApolloClient

    private lateinit var model: SuggestionsModel

    override fun setUp() {
        super.setUp()
        siteSuggestions = MutableStateFlow(emptyList())
        urlBarText = MutableStateFlow(TextFieldValue(""))
        urlBarIsEditing = MutableStateFlow(false)

        historyManager = mock()
        Mockito.`when`(historyManager.siteSuggestions).thenReturn(siteSuggestions)

        apolloClient = ApolloClient.Builder()
            .serverUrl("https://fake.url")
            .okHttpClient(
                OkHttpClient.Builder()
                    .addInterceptor(TestInterceptor())
                    .build()
            )
            .build()

        model = SuggestionsModel(
            coroutineScopeRule.scope,
            historyManager,
            apolloClient,
            domainProvider
        )

        coroutineScopeRule.scope.advanceUntilIdle()
    }

    inner class TestInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val responseBody = ResponseBody.create(
                "application/json".toMediaTypeOrNull(),
                responseData
            )

            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .message("Success")
                .code(200)
                .body(responseBody)
                .addHeader("content-type", "application/json")
                .build()
        }
    }

    override fun tearDown() {
        super.tearDown()
        expectThat(coroutineScopeRule.scope.isActive).isTrue()
    }

    @Test
    fun init_collectsHistorySuggestions() {
        expectThat(model.autocompleteSuggestion.value).isNull()
        expectThat(model.suggestionFlow.value).isEqualTo(Suggestions())

        siteSuggestions.value = listOf(
            Site(
                siteURL = "https://www.reddit.com/r/android",
                lastVisitTimestamp = Date(),
                metadata = null,
                largestFavicon = null
            ),
            Site(
                siteURL = "https://www.ignored.url",
                lastVisitTimestamp = Date(),
                metadata = null,
                largestFavicon = null
            )
        )

        coroutineScopeRule.scope.advanceUntilIdle()
        val navSuggestions = siteSuggestions.value.map { it.toNavSuggestion(domainProvider) }
        expectThat(model.autocompleteSuggestion.value).isEqualTo(navSuggestions.first())
        expectThat(model.suggestionFlow.value).isEqualTo(
            Suggestions(autocompleteSuggestion = navSuggestions.first())
        )
    }

    @Test
    fun onUrlBarChanged_withEmptyString_doesNoQuery() = runTest {
        responseData = FULL_RESPONSE

        // Pass in a different CoroutineScope so that the subscriptions in the constructor don't
        // cause the thread to hang.
        val collectionJob = CoroutineScope(this.coroutineContext + Job())
        val model = SuggestionsModel(
            collectionJob,
            historyManager,
            apolloClient,
            domainProvider
        )
        advanceUntilIdle()

        model.getSuggestionsFromBackend("")
        expectThat(model.suggestionFlow.value.queryRowSuggestions).isEmpty()
        expectThat(model.suggestionFlow.value.navSuggestions).isEmpty()
    }

    @Test
    fun onUrlBarChanged_withNonEmptyString_firesRequestAndProcessesResult() = runTest {
        responseData = FULL_RESPONSE

        // Pass in a different CoroutineScope so that the subscriptions in the constructor don't
        // cause the thread to hang.
        val collectionJob = CoroutineScope(this.coroutineContext + Job())
        val model = SuggestionsModel(
            collectionJob,
            historyManager,
            apolloClient,
            domainProvider
        )
        advanceUntilIdle()

        // Set the autocomplete suggestion.
        siteSuggestions.value = listOf(
            Site(
                siteURL = "https://www.reddit.com/r/android",
                lastVisitTimestamp = Date(),
                metadata = null,
                largestFavicon = null
            )
        )
        advanceUntilIdle()

        // Make sure the autocomplete suggestion has been set and nothing else has.
        val expectedSuggestion = siteSuggestions.value.first().toNavSuggestion(domainProvider)
        expectThat(model.autocompleteSuggestion.value).isEqualTo(expectedSuggestion)
        expectThat(model.suggestionFlow.value).isEqualTo(
            Suggestions(autocompleteSuggestion = expectedSuggestion)
        )

        // Trigger the Apollo query and check the results.
        model.getSuggestionsFromBackend("query text")
        advanceUntilIdle()

        val emptyAnnotation = SuggestionsQuery.Annotation(null, null, null, null, null)
        expectThat(model.suggestionFlow.value.queryRowSuggestions).containsExactly(
            listOf(
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion(),
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit nfl streams",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion(),
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit news",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion(),
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit.com",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion(),
                SuggestionsQuery.QuerySuggestion(
                    type = QuerySuggestionType.Standard,
                    suggestedQuery = "reddit cfb",
                    boldSpan = listOf(SuggestionsQuery.BoldSpan(0, 5)),
                    source = QuerySuggestionSource.Bing,
                    annotation = emptyAnnotation
                ).toQueryRowSuggestion()
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
        expectThat(model.autocompleteSuggestion.value).isEqualTo(expectedSuggestion)
        expectThat(model.suggestionFlow.value.autocompleteSuggestion).isEqualTo(expectedSuggestion)
    }

    companion object {
        val FULL_RESPONSE = """{
            "data":{
                "suggest":{
                    "querySuggestion":[
                        {
                            "type":"Standard",
                            "suggestedQuery":"reddit",
                            "boldSpan":[
                                {
                                    "startInclusive":0,
                                    "endExclusive":5
                                }
                            ],
                            "source":"Bing",
                            "annotation":{
                                "annotationType":null,
                                "description":null,
                                "imageURL":null,
                                "stockInfo":null,
                                "dictionaryInfo":null
                            }
                        },
                        {
                            "type":"Standard",
                            "suggestedQuery":"reddit nfl streams",
                            "boldSpan":[
                                {
                                    "startInclusive":0,
                                    "endExclusive":5
                                }
                            ],
                            "source":"Bing",
                            "annotation":{
                                "annotationType":null,
                                "description":null,
                                "imageURL":null,
                                "stockInfo":null,
                                "dictionaryInfo":null
                            }
                        },
                        {
                            "type":"Standard",
                            "suggestedQuery":"reddit news",
                            "boldSpan":[
                                {
                                    "startInclusive":0,
                                    "endExclusive":5
                                }
                            ],
                            "source":"Bing",
                            "annotation":{
                                "annotationType":null,
                                "description":null,
                                "imageURL":null,
                                "stockInfo":null,
                                "dictionaryInfo":null
                            }
                        },
                        {
                            "type":"Standard",
                            "suggestedQuery":"reddit.com",
                            "boldSpan":[
                                {
                                    "startInclusive":0,
                                    "endExclusive":5
                                }
                            ],
                            "source":"Bing",
                            "annotation":{
                                "annotationType":null,
                                "description":null,
                                "imageURL":null,
                                "stockInfo":null,
                                "dictionaryInfo":null
                            }
                        },
                        {
                            "type":"Standard",
                            "suggestedQuery":"reddit cfb",
                            "boldSpan":[
                                {
                                    "startInclusive":0,
                                    "endExclusive":5
                                }
                            ],
                            "source":"Bing",
                            "annotation":{
                                "annotationType":null,
                                "description":null,
                                "imageURL":null,
                                "stockInfo":null,
                                "dictionaryInfo":null
                            }
                        }
                    ],
                    "urlSuggestion":[
                        {
                            "icon":{
                                "labels":null
                            },
                            "suggestedURL":"https://www.reddit.com/",
                            "title":"https://www.reddit.com/",
                            "author":"",
                            "timestamp":null,
                            "subtitle":"reddit: the front page of the internet",
                            "sourceQueryIndex":0,
                            "boldSpan":[
                                {
                                    "startInclusive":12,
                                    "endExclusive":17
                                }
                            ]
                        },
                        {
                            "icon":{
                                "labels":null
                            },
                            "suggestedURL":"https://nflthursday.com/reddit-nfl-streams/",
                            "title":"https://nflthursday.com/reddit-nfl-streams/",
                            "author":"",
                            "timestamp":null,
                            "subtitle":"Reddit NFL streams is banned - How to watch this weeks ...",
                            "sourceQueryIndex":1,
                            "boldSpan":[
                                {
                                    "startInclusive":24,
                                    "endExclusive":29
                                }
                            ]
                        },
                        {
                            "icon":{
                                "labels":null
                            },
                            "suggestedURL":"https://www.reddit.com/r/news/",
                            "title":"https://www.reddit.com/r/news/",
                            "author":"",
                            "timestamp":null,
                            "subtitle":"News - reddit",
                            "sourceQueryIndex":2,
                            "boldSpan":[
                                {
                                    "startInclusive":12,
                                    "endExclusive":17
                                }
                            ]
                        },
                        {
                            "icon":{
                                "labels":null
                            },
                            "suggestedURL":"https://www.reddit.com/r/cfb",
                            "title":"https://www.reddit.com/r/cfb",
                            "author":"",
                            "timestamp":null,
                            "subtitle":"r/CFB - Reddit",
                            "sourceQueryIndex":4,
                            "boldSpan":[
                                {
                                    "startInclusive":12,
                                    "endExclusive":17
                                }
                            ]
                        }
                    ],
                    "lenseSuggestion":null,
                    "bangSuggestion":null,
                    "activeLensBangInfo":null
                }
            }
        }
        """.trimIndent()
    }
}
