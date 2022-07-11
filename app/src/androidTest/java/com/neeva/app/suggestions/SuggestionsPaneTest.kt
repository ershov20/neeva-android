package com.neeva.app.suggestions

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.SuggestionsQuery
import com.neeva.app.WebpageServingRule
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.apollo.TestAuthenticatedApolloWrapper
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.clickOnUrlBar
import com.neeva.app.expectTabListState
import com.neeva.app.getString
import com.neeva.app.type.QuerySuggestionSource
import com.neeva.app.type.QuerySuggestionType
import com.neeva.app.typeIntoUrlBar
import com.neeva.app.visitMultipleSitesInSameTab
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForMatchingNode
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForUrl
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SuggestionsPaneTest : BaseBrowserTest() {
    @get:Rule
    val skipFirstRunRule = SkipFirstRunRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Inject lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper
    @Inject lateinit var neevaConstants: NeevaConstants

    private lateinit var testAuthenticatedApolloWrapper: TestAuthenticatedApolloWrapper

    @Before
    override fun setUp() {
        super.setUp()

        testAuthenticatedApolloWrapper =
            authenticatedApolloWrapper as TestAuthenticatedApolloWrapper

        testAuthenticatedApolloWrapper.registerTestResponse(
            SuggestionsQuery(query = "Page"),
            FULL_RESPONSE_DATA
        )
        testAuthenticatedApolloWrapper.registerTestResponse(
            SuggestionsQuery(query = "Sports Page"),
            SPORTS_PAGE_RESPONSE_DATA
        )

        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectTabListState(isIncognito = false, regularTabCount = 1)
        }
    }

    @Test
    fun displaysNeevaSuggestionsAndHistory() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()
            clickOnUrlBar()
            typeIntoUrlBar("Page")

            waitForNodeWithTag("SuggestionList").apply {
                // Confirm that the search suggestions exist.
                performScrollToNode(hasText(getString(R.string.neeva_search)))
                performScrollToNode(hasText("Sports Page"))
                performScrollToNode(hasText("Sports Page homepage"))
                performScrollToNode(hasText("Front Page News"))
                performScrollToNode(hasText("Front Page News Result 1"))
                performScrollToNode(hasText("Front Page News Result 2"))

                // Confirm that the history suggestions exist.
                performScrollToNode(hasText(getString(R.string.history)))
                performScrollToNode(hasText("Page 1"))
                performScrollToNode(hasText("Page 2"))
                performScrollToNode(hasText("Page 3"))
            }
        }
    }

    @Test
    fun queryRefiningPutsTextIntoUrlBar() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()
            clickOnUrlBar()
            typeIntoUrlBar("Page")

            // Wait for a query suggestion to appear.
            waitForNodeWithTag("SuggestionList").apply {
                performScrollToNode(hasText(getString(R.string.neeva_search)))
                performScrollToNode(hasText("Sports Page"))
            }

            // Click on the "refine" button so that the text pops into the URL bar.
            waitForMatchingNode(
                hasContentDescription(getString(R.string.refine_content_description))
                    .and(hasAnyAncestor(hasText("Sports Page")))
            ).performClick()
            waitForNodeWithTag("AutocompleteTextField").assertTextEquals("Sports Page")

            // Confirm that the search suggestions updated.
            waitForNodeWithTag("SuggestionList").apply {
                performScrollToNode(hasText(getString(R.string.neeva_search)))
                performScrollToNode(hasText("Sports Page"))
                performScrollToNode(hasText("Sports Page homepage"))
                performScrollToNode(hasText("Not Sports Page"))
                performScrollToNode(hasText("Not Sports Page Result"))
            }
        }
    }

    @Test
    fun queryResultClickNavigatesToPage() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()
            clickOnUrlBar()
            typeIntoUrlBar("Page")

            // Wait for a query suggestion to appear.
            waitForNodeWithTag("SuggestionList").apply {
                performScrollToNode(hasText(getString(R.string.neeva_search)))
                performScrollToNode(hasText("Sports Page"))
            }

            // Click on the query suggestion and confirm that we navigated to the search page.
            waitForMatchingNode(hasText("Sports Page")).performClick()
            waitForUrl("Sports Page".toSearchUri(neevaConstants).toString())
            waitForNodeWithTag("LocationLabel").assertTextEquals("Sports Page")
        }
    }

    @Test
    fun resultClickNavigatesToPage() {
        androidComposeRule.apply {
            visitMultipleSitesInSameTab()
            clickOnUrlBar()
            typeIntoUrlBar("Page")

            // Wait for a query suggestion to appear.
            waitForNodeWithTag("SuggestionList").apply {
                performScrollToNode(hasText(getString(R.string.neeva_search)))
                performScrollToNode(hasText("Sports Page"))
            }

            // Click on the site suggestion and confirm that we navigated to the page directly.
            waitForMatchingNode(hasText("Sports Page homepage")).performClick()
            waitForUrl(resultUrl)
        }
    }

    companion object {
        val resultUrl = WebpageServingRule.urlFor("big_link_element.html")

        private val FULL_RESPONSE_DATA = SuggestionsQuery.Data(
            suggest = SuggestionsQuery.Suggest(
                querySuggestion = listOf(
                    SuggestionsQuery.QuerySuggestion(
                        type = QuerySuggestionType.Standard,
                        suggestedQuery = "Sports Page",
                        boldSpan = listOf(),
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
                        suggestedQuery = "Front Page News",
                        boldSpan = listOf(),
                        source = QuerySuggestionSource.Bing,
                        annotation = SuggestionsQuery.Annotation(
                            annotationType = null,
                            description = null,
                            imageURL = null,
                            stockInfo = null,
                            dictionaryInfo = null
                        )
                    ),
                ),
                urlSuggestion = listOf(
                    SuggestionsQuery.UrlSuggestion(
                        icon = SuggestionsQuery.Icon(labels = null),
                        suggestedURL = resultUrl,
                        title = resultUrl,
                        author = "",
                        timestamp = null,
                        subtitle = "Sports Page homepage",
                        sourceQueryIndex = 0,
                        boldSpan = listOf()
                    ),
                    SuggestionsQuery.UrlSuggestion(
                        icon = SuggestionsQuery.Icon(labels = null),
                        suggestedURL = "https://www.frontpagenews.com/1",
                        title = "https://www.frontpagenews.com/1",
                        author = "",
                        timestamp = null,
                        subtitle = "Front Page News Result 1",
                        sourceQueryIndex = 1,
                        boldSpan = listOf()
                    ),
                    SuggestionsQuery.UrlSuggestion(
                        icon = SuggestionsQuery.Icon(labels = null),
                        suggestedURL = "https://www.frontpagenews.com/2",
                        title = "https://www.frontpagenews.com/2",
                        author = "",
                        timestamp = null,
                        subtitle = "Front Page News Result 2",
                        sourceQueryIndex = 1,
                        boldSpan = listOf()
                    )
                ),
                lenseSuggestion = null,
                bangSuggestion = null,
                activeLensBangInfo = null
            )
        )

        private val SPORTS_PAGE_RESPONSE_DATA = SuggestionsQuery.Data(
            suggest = SuggestionsQuery.Suggest(
                querySuggestion = listOf(
                    SuggestionsQuery.QuerySuggestion(
                        type = QuerySuggestionType.Standard,
                        suggestedQuery = "Sports Page",
                        boldSpan = listOf(),
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
                        suggestedQuery = "Not Sports Page",
                        boldSpan = listOf(),
                        source = QuerySuggestionSource.Bing,
                        annotation = SuggestionsQuery.Annotation(
                            annotationType = null,
                            description = null,
                            imageURL = null,
                            stockInfo = null,
                            dictionaryInfo = null
                        )
                    ),
                ),
                urlSuggestion = listOf(
                    SuggestionsQuery.UrlSuggestion(
                        icon = SuggestionsQuery.Icon(labels = null),
                        suggestedURL = resultUrl,
                        title = resultUrl,
                        author = "",
                        timestamp = null,
                        subtitle = "Sports Page homepage",
                        sourceQueryIndex = 0,
                        boldSpan = listOf()
                    ),
                    SuggestionsQuery.UrlSuggestion(
                        icon = SuggestionsQuery.Icon(labels = null),
                        suggestedURL = "https://www.notsportspage.com/",
                        title = "https://www.notsportspage.com/",
                        author = "",
                        timestamp = null,
                        subtitle = "Not Sports Page Result",
                        sourceQueryIndex = 1,
                        boldSpan = listOf()
                    )
                ),
                lenseSuggestion = null,
                bangSuggestion = null,
                activeLensBangInfo = null
            )
        )
    }
}
