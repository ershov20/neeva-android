// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CheatsheetInfoQuery
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.SearchQuery
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.type.UserPreference
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserImpl
import com.neeva.app.userdata.UserInfo
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class NeevaScopeModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var dispatchers: Dispatchers
    private lateinit var neevaScopeModel: NeevaScopeModel
    private lateinit var context: Context
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var neevaUser: NeevaUser
    private lateinit var apolloWrapper: TestAuthenticatedApolloWrapper

    override fun setUp() {
        super.setUp()

        neevaConstants = NeevaConstants()

        context = ApplicationProvider.getApplicationContext()
        val loginToken = LoginToken(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            sharedPreferencesModel = SharedPreferencesModel(context),
            neevaConstants = neevaConstants
        )
        loginToken.updateCachedCookie("NotAnEmptyToken")

        neevaUser = NeevaUserImpl(
            sharedPreferencesModel = SharedPreferencesModel(context),
            loginToken = loginToken
        )
        neevaUser.setUserInfo(UserInfo("c5rgtdldv9enb8j1gupg"))

        apolloWrapper = TestAuthenticatedApolloWrapper(
            loginToken = loginToken,
            neevaConstants = neevaConstants
        )

        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )

        neevaScopeModel = NeevaScopeModel(
            apolloWrapper = apolloWrapper,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = dispatchers,
            appContext = context
        )
    }

    @Test
    fun onQueryChanged_withNonEmptyString_fetchNeevaScopeDataAndTestResult() {
        runBlocking {
            val result = neevaScopeModel.updateNeevaScopeResult(
                SEARCH_DATA,
                CHEATSHEET_DATA,
                context
            )

            expectThat(result.webSearches).containsExactly(
                NeevaScopeWebResult(
                    faviconURL = "",
                    displayURLHost = "neeva.com",
                    displayURLPath = listOf("neeva", "search"),
                    actionURL = Uri.parse("www.neeva.com"),
                    title = "Neeva search",
                    snippet = ""
                )
            )

            expectThat(result.relatedSearches).containsExactly(
                "related search 1",
                "related search 2"
            )

            expectThat(result.memorizedSearches).containsExactly(
                "memorized query 1",
                "memorized query 2"
            )

            expectThat(result.redditDiscussions).containsExactly(
                NeevaScopeDiscussion(
                    title = "GTA Vice City",
                    content = DiscussionContent(
                        body = "This is forum body",
                        comments = listOf(
                            DiscussionComment(
                                body = "Comment 1",
                                url = Uri.parse(""),
                                upvotes = 1
                            ),
                            DiscussionComment(
                                body = "Comment 2",
                                url = Uri.parse(""),
                                upvotes = 2
                            )
                        )
                    ),
                    url = Uri.parse(
                        "https://www.reddit.com/r/GTA/comments/85uyan/gta_vice_city"
                    ),
                    slash = "r/GTA",
                    upvotes = 1,
                    numComments = 4,
                    interval = "4 years ago"
                )
            )

            expectThat(result.recipe).equals(
                NeevaScopeRecipe(
                    title = "Lemon Bars",
                    imageURL = "",
                    totalTime = "",
                    prepTime = "",
                    yield = "24 Servings",
                    ingredients = listOf(),
                    instructions = listOf(),
                    recipeRating = RecipeRating(5.0, 4.7, 877),
                    reviews = listOf(),
                    preference = null
                )
            )
        }
    }

    @Test
    fun toDiscussionSlash() {
        expectThat("https://www.reddit.com/r/android".toDiscussionSlash()).isEqualTo("r/android")
        expectThat("https://www.reddit.com/r/airsoft/comments/airsoft_maryland".toDiscussionSlash())
            .isEqualTo("r/airsoft")
        expectThat("https://www.reddit.com/airsoft/comments/airsoft_maryland".toDiscussionSlash())
            .isEqualTo(null)
    }

    companion object {
        val SEARCH_DATA = SearchQuery.Data(
            search = SearchQuery.Search(
                resultGroup = listOf(
                    SearchQuery.ResultGroup(
                        result = listOf(
                            SearchQuery.Result(
                                title = "Neeva search",
                                appIcon = SearchQuery.AppIcon(listOf("")),
                                actionURL = "www.neeva.com",
                                snippet = "",
                                typeSpecific = SearchQuery.TypeSpecific(
                                    __typename = "web",
                                    onWeb = SearchQuery.OnWeb(
                                        SearchQuery.Web(
                                            favIconURL = "",
                                            displayUrl = "",
                                            publicationDate = null,
                                            structuredUrl = SearchQuery.StructuredUrl(
                                                paths = listOf("neeva", "search"),
                                                hostname = "neeva.com"
                                            ),
                                            highlightedSnippet = SearchQuery.HighlightedSnippet(
                                                listOf(
                                                    SearchQuery.Segment(
                                                        "This",
                                                        false
                                                    ),
                                                    SearchQuery.Segment(
                                                        "is",
                                                        true
                                                    ),
                                                    SearchQuery.Segment(
                                                        "neeva search",
                                                        false
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    onRelatedSearches = null
                                )
                            ),
                            SearchQuery.Result(
                                title = "Related search",
                                appIcon = SearchQuery.AppIcon(listOf("")),
                                actionURL = "",
                                snippet = "",
                                typeSpecific = SearchQuery.TypeSpecific(
                                    __typename = "relatedSearches",
                                    onWeb = null,
                                    onRelatedSearches = SearchQuery.OnRelatedSearches(
                                        SearchQuery.RelatedSearches(
                                            listOf(
                                                SearchQuery.Entry(
                                                    searchText = "related search 1",
                                                    displayText = null
                                                ),
                                                SearchQuery.Entry(
                                                    searchText = "related search 2",
                                                    displayText = null
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        val CHEATSHEET_DATA = CheatsheetInfoQuery.Data(
            getCheatsheetInfo = CheatsheetInfoQuery.GetCheatsheetInfo(
                MemorizedQuery = listOf(
                    "memorized query 1",
                    "memorized query 2"
                ),
                BacklinkURL = listOf(
                    CheatsheetInfoQuery.BacklinkURL(
                        URL = "https://www.reddit.com/r/GTA/comments/85uyan/gta_vice_city",
                        Title = "GTA Vice City",
                        Domain = "www.reddit.com",
                        Snippet = "This is snippet",
                        Forum = CheatsheetInfoQuery.Forum(
                            url = "https://www.reddit.com/r/GTA/comments/85uyan/gta_vice_city",
                            source = "https://www.reddit.com/r/GTA/comments/85uyan/gta_vice_city",
                            domain = "reddit.com",
                            score = 1,
                            date = "2018-03-20 18:13:16 +0000 UTC",
                            title = "How do you get Vice City Mp3 Control to work?",
                            body = "This is forum body",
                            percentUpvoted = 0,
                            numComments = 4,
                            comments = listOf(
                                CheatsheetInfoQuery.Comment(
                                    url = "",
                                    score = 1,
                                    date = "2018-03-20 23:59:53 +0000 UTC",
                                    body = "Comment 1"
                                ),
                                CheatsheetInfoQuery.Comment(
                                    url = "",
                                    score = 2,
                                    date = "2018-03-21 23:59:53 +0000 UTC",
                                    body = "Comment 2"
                                )
                            )
                        )
                    )
                ),
                Recipe = CheatsheetInfoQuery.Recipe(
                    title = "Lemon Bars",
                    preference = UserPreference.UNKNOWN__,
                    imageURL = "",
                    totalTime = "",
                    prepTime = "",
                    yield = "24 Servings",
                    ingredients = listOf(),
                    instructions = listOf(),
                    recipeRating = CheatsheetInfoQuery.RecipeRating(
                        maxStars = 5.0,
                        recipeStars = 4.7,
                        numReviews = 877
                    ),
                    reviews = listOf()
                )
            )
        )
    }
}
