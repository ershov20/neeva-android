package com.neeva.app.suggestions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.SkipFirstRunRule
import com.neeva.app.SuggestionsQuery
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.browsing.toSearchUri
import com.neeva.app.clickOnNodeWithContentDescription
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.clickOnUrlBar
import com.neeva.app.expectBrowserState
import com.neeva.app.getString
import com.neeva.app.navigateViaUrlBar
import com.neeva.app.onBackPressed
import com.neeva.app.openCardGrid
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.type.QuerySuggestionSource
import com.neeva.app.type.QuerySuggestionType
import com.neeva.app.typeIntoUrlBar
import com.neeva.app.visitMultipleSitesInSameTab
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNode
import com.neeva.app.waitForNodeWithTag
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForUrl
import com.neeva.testcommon.WebpageServingRule
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

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
            expectBrowserState(isIncognito = false, regularTabCount = 1)
        }
    }

    @Test
    fun hidesSuggestionsWhenDisabled() {
        androidComposeRule.apply {
            // Turn the "Show search suggestions" setting off.
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)
            waitForNodeWithTag("SettingsPaneItems").performScrollToNode(
                hasText(getString(R.string.settings_show_search_search_suggestions))
            )
            clickOnNodeWithText(getString(R.string.settings_show_search_search_suggestions))
            onBackPressed()
            waitForNavDestination(AppNavDestination.BROWSER)

            visitMultipleSitesInSameTab()
            clickOnUrlBar()
            typeIntoUrlBar("Page")

            waitForNodeWithTag("SuggestionList").apply {
                // Confirm that the history suggestions exist.
                performScrollToNode(hasText(getString(R.string.history)))
                performScrollToNode(hasText("Page 1"))
                performScrollToNode(hasText("Page 2"))
                performScrollToNode(hasText("Page 3"))
            }

            // Confirm that there are no search suggestions.
            onNodeWithText(getString(R.string.neeva_search)).assertDoesNotExist()
            onNodeWithText("Sports Page").assertDoesNotExist()
            onNodeWithText("Sports Page homepage").assertDoesNotExist()
            onNodeWithText("Front Page News").assertDoesNotExist()
            onNodeWithText("Front Page News Result 1").assertDoesNotExist()
            onNodeWithText("Front Page News Result 2").assertDoesNotExist()
        }
    }

    @Test
    fun hidesSuggestionsWhenIncognito() {
        val initialIncognitoUrl = WebpageServingRule.urlFor("big_link_element.html")

        androidComposeRule.apply {
            // Open a lazy Incognito tab.
            openCardGrid(incognito = true)
            clickOnNodeWithContentDescription(getString(R.string.create_new_tab_a11y))
            waitForNavDestination(AppNavDestination.BROWSER)

            // Confirm we see Incognito's Zero Query page.
            waitForNodeWithText(getString(R.string.incognito_zero_query_title))

            // Navigate somewhere with the currently open URL bar.
            navigateViaUrlBar(initialIncognitoUrl)

            val apolloMutationsBefore =
                testAuthenticatedApolloWrapper.testApolloClientWrapper.performedOperations.toList()

            // Start a new query.
            clickOnUrlBar()
            typeIntoUrlBar("Page")

            // Confirm no new Apollo operations were performed.
            val apolloMutationsAfter =
                testAuthenticatedApolloWrapper.testApolloClientWrapper.performedOperations.toList()
            expectThat(apolloMutationsAfter).isEqualTo(apolloMutationsBefore)

            // Confirm we still see Incognito's Zero Query page with no suggestions.
            waitForNodeWithText(getString(R.string.incognito_zero_query_title))
            onNodeWithTag("SuggestionList").assertDoesNotExist()

            // Confirm that we have the option to edit the current address.
            waitForNodeWithText(getString(R.string.edit_current_url)).assertIsDisplayed()
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
            val contentDescription = activity.getString(
                R.string.edit_suggested_query,
                "Sports Page"
            )
            waitForNode(
                hasContentDescription(contentDescription)
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
            waitForNode(hasText("Sports Page")).performClick()
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
            waitForNode(hasText("Sports Page homepage")).performClick()
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
