// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.spaces

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.apollographql.apollo3.api.Optional
import com.neeva.app.AddToSpaceMutation
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.DeleteSpaceResultByURLMutation
import com.neeva.app.Dispatchers
import com.neeva.app.GetSpacesDataQuery
import com.neeva.app.ListSpacesQuery
import com.neeva.app.NeevaConstants
import com.neeva.app.billing.billingclient.BillingClientController
import com.neeva.app.network.NetworkHandler
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.Directories
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserImpl
import com.neeva.app.userdata.PreviewSessionToken
import com.neeva.app.userdata.UserInfo
import com.neeva.testcommon.apollo.MockListSpacesQueryData
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import com.neeva.testcommon.apollo.TestUnauthenticatedApolloWrapper
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class SpaceStoreTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var popupModel: PopupModel
    @Mock private lateinit var previewSessionToken: PreviewSessionToken
    @Mock private lateinit var networkHandler: NetworkHandler
    @Mock private lateinit var billingClientController: BillingClientController

    private lateinit var context: Context
    private lateinit var neevaUser: NeevaUser
    private lateinit var database: HistoryDatabase
    private lateinit var spaceStore: SpaceStore
    private lateinit var file: File
    private lateinit var dispatchers: Dispatchers
    private lateinit var neevaConstants: NeevaConstants

    private lateinit var authenticatedApolloWrapper: TestAuthenticatedApolloWrapper
    private lateinit var unauthenticatedApolloWrapper: TestUnauthenticatedApolloWrapper

    override fun setUp() {
        super.setUp()

        neevaConstants = NeevaConstants()

        context = ApplicationProvider.getApplicationContext()
        database = HistoryDatabase.createInMemory(context)
        val sharedPreferencesModel = SharedPreferencesModel(context)

        val loginToken = LoginToken(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            sharedPreferencesModel = sharedPreferencesModel,
            neevaConstants = neevaConstants
        )
        loginToken.updateCachedCookie("NotAnEmptyToken")

        neevaUser = NeevaUserImpl(
            sharedPreferencesModel = sharedPreferencesModel,
            loginToken = loginToken,
            networkHandler = networkHandler,
            billingClientController = billingClientController
        )

        neevaUser.setUserInfo(UserInfo("c5rgtdldv9enb8j1gupg"))

        authenticatedApolloWrapper = TestAuthenticatedApolloWrapper(
            loginToken = loginToken,
            previewSessionToken = previewSessionToken,
            neevaConstants = neevaConstants
        )
        unauthenticatedApolloWrapper = TestUnauthenticatedApolloWrapper(
            neevaConstants = neevaConstants
        )

        dispatchers = Dispatchers(
            main = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
            io = StandardTestDispatcher(coroutineScopeRule.scope.testScheduler),
        )
        spaceStore = SpaceStore(
            appContext = context,
            historyDatabase = database,
            coroutineScope = coroutineScopeRule.scope,
            unauthenticatedApolloWrapper = unauthenticatedApolloWrapper,
            authenticatedApolloWrapper = authenticatedApolloWrapper,
            neevaUser = neevaUser,
            neevaConstants = neevaConstants,
            popupModel = popupModel,
            dispatchers = dispatchers,
            directories = Directories(
                context = context,
                coroutineScope = coroutineScopeRule.scope,
                dispatchers = dispatchers
            )
        )
        file = context.cacheDir.resolve("space_store_test")
    }

    @Test fun convertApolloSpace_givenAllRequiredData_createsSpace() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            val original = MockListSpacesQueryData.SPACE_1
            original.toSpace(neevaUser.userInfoFlow.value!!.id)!!.apply {
                expectThat(id).isEqualTo("c5rgtmtdv9enb8j1gv60")
                expectThat(name).isEqualTo("Saved For Later")
                expectThat(lastModifiedTs).isEqualTo("2022-02-10T22:08:01Z")
                expectThat(thumbnail).isEqualTo(null)
                expectThat(resultCount).isEqualTo(1)
                expectThat(isDefaultSpace).isTrue()
                expectThat(userACL).isEqualTo(SpaceACLLevel.Owner)
                expectThat(isPublic).isTrue()
                expectThat(isShared).isTrue()
            }
        }

    @Test fun convertApolloSpace_properlyHandlesAclInfo() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            neevaUser.setUserInfo(UserInfo(id = "wrong id"))
            val original = MockListSpacesQueryData.SPACE_2.copy(
                space = MockListSpacesQueryData.SPACE_2.space!!.copy(hasPublicACL = false)
            )
            original.toSpace(neevaUser.userInfoFlow.value!!.id)!!.apply {
                expectThat(userACL).isEqualTo(SpaceACLLevel.Comment)
                expectThat(isPublic).isFalse()
                expectThat(isShared).isFalse()
            }
        }

    @Test
    fun refresh_spaceThumbnailsWrittenToDiskAndCleanedUpOnce() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            val oldDirectory = File(spaceStore.spacesDirectory.await(), "oldspaceID")
            oldDirectory.mkdirs()

            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            expectThat(oldDirectory.exists()).isTrue()

            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                MockListSpacesQueryData.LIST_SPACES_QUERY_RESPONSE
            )
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY,
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY_RESPONSE
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            val spaceID = MockListSpacesQueryData.SPACE_1.pageMetadata!!.pageID!!
            val directory = File(spaceStore.spacesDirectory.await(), spaceID)
            val file = File(directory, spaceID)
            expectThat(directory.exists()).isTrue()
            expectThat(file.exists()).isTrue()

            // Confirm old directory is gone.
            expectThat(oldDirectory.exists()).isFalse()

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }

            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                RESPONSE_LIST_SPACE_QUERY_WITH_FIRST_SPACE_DELETED_AND_SECOND_UPDATED
            )
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY,
                RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            // Confirm SPACE_1 directory remains because the cleanup only runs once.
            expectThat(directory.exists()).isTrue()
            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(4)
                expectThat(get(2)).isA<ListSpacesQuery>()
                expectThat(get(3)).isA<GetSpacesDataQuery>()
            }
        }

    @Test
    fun refresh_schedulesAnExtraRefresh() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                MockListSpacesQueryData.LIST_SPACES_QUERY_RESPONSE
            )
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY,
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY_RESPONSE
            )
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                MockListSpacesQueryData.LIST_SPACES_QUERY_RESPONSE
            )

            spaceStore.refresh()
            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(3)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
                expectThat(get(2)).isA<ListSpacesQuery>()
            }
        }

    @Test
    fun refresh_skipsWhileLoggedOut() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            neevaUser.loginToken.updateCachedCookie("")

            expectThat(neevaUser.loginToken.cachedValue).isEmpty()

            spaceStore.refresh()

            expectThat(
                authenticatedApolloWrapper.testApolloClientWrapper.performedOperations
            ).isEmpty()
        }

    @Test fun convertApolloSpace_withMissingData_returnsNull() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            expectThat(
                MockListSpacesQueryData.SPACE_1.copy(pageMetadata = null)
                    .toSpace(neevaUser.userInfoFlow.value!!.id)
            ).isNull()

            expectThat(
                MockListSpacesQueryData.SPACE_1.copy(space = null)
                    .toSpace(neevaUser.userInfoFlow.value!!.id)
            ).isNull()

            expectThat(
                MockListSpacesQueryData.SPACE_1.copy(
                    space = MockListSpacesQueryData.SPACE_1.space!!.copy(name = null)
                )
                    .toSpace(neevaUser.userInfoFlow.value!!.id)
            ).isNull()

            expectThat(
                MockListSpacesQueryData.SPACE_1
                    .copy(
                        space = MockListSpacesQueryData.SPACE_1.space!!.copy(
                            lastModifiedTs = null
                        )
                    )
                    .toSpace(neevaUser.userInfoFlow.value!!.id)
            ).isNull()

            expectThat(
                MockListSpacesQueryData.SPACE_1
                    .copy(space = MockListSpacesQueryData.SPACE_1.space!!.copy(userACL = null))
                    .toSpace(neevaUser.userInfoFlow.value!!.id)
            ).isNull()
        }

    @Test
    fun refresh_withValidSpaceListQueryResponse_returnsBothSpaces() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            // Only allow the response to fetch the user's Spaces to succeed.
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                MockListSpacesQueryData.LIST_SPACES_QUERY_RESPONSE
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            // The refresh will get data about the Spaces but not their contents.
            val allSpaces = database.spaceDao().allSpaces()
            expectThat(allSpaces).hasSize(2)
            allSpaces[0].apply {
                expectThat(id).isEqualTo(MockListSpacesQueryData.SPACE_1.pageMetadata!!.pageID)
                expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
            }
            allSpaces[1].apply {
                expectThat(id).isEqualTo(MockListSpacesQueryData.SPACE_2.pageMetadata!!.pageID)
                expectThat(database.spaceDao().getItemsFromSpace(id)).isEmpty()
            }

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }
        }

    @Test
    fun refresh_withValidResponses_returnsAllData() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                MockListSpacesQueryData.LIST_SPACES_QUERY_RESPONSE
            )
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY,
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY_RESPONSE
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id).map { it.url })
                        .containsExactly(
                            Uri.parse(MockListSpacesQueryData.SPACE_1_ITEM_1.spaceEntity!!.url!!)
                        )
                }
                allSpaces[1].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_2.pageMetadata!!.pageID)
                    expectThat(
                        database.spaceDao()
                            .getItemsFromSpace(id)
                            .map { it.url }
                    ).containsExactly(
                        Uri.parse("https://developer.android.com/jetpack/compose/"),
                        Uri.parse("http://example.com/")
                    )
                }
            }
            database.spaceDao().allSpaces()
                .filterNot { it.userACL >= SpaceACLLevel.Edit }
                .let { editableSpaces ->
                    expectThat(editableSpaces).hasSize(1)
                    editableSpaces[0].apply {
                        expectThat(id)
                            .isEqualTo(MockListSpacesQueryData.SPACE_1.pageMetadata!!.pageID)
                        expectThat(database.spaceDao().getItemsFromSpace(id).map { it.url })
                            .containsExactly(
                                Uri.parse(
                                    MockListSpacesQueryData.SPACE_1_ITEM_1.spaceEntity!!.url!!
                                )
                            )
                    }
                }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isTrue()

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }
        }

    @Test
    fun refresh_updatesWhenSpaceTimestampChanges() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                MockListSpacesQueryData.LIST_SPACES_QUERY_RESPONSE
            )
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY,
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY_RESPONSE
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id).map { it.url })
                        .containsExactly(
                            Uri.parse(MockListSpacesQueryData.SPACE_1_ITEM_1.spaceEntity!!.url!!)
                        )
                }
                allSpaces[1].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_2.pageMetadata!!.pageID)
                    expectThat(lastModifiedTs).isEqualTo("2022-02-10T02:10:38Z")
                    expectThat(
                        database.spaceDao()
                            .getItemsFromSpace(id)
                            .map { it.url }
                    ).containsExactly(
                        Uri.parse("https://developer.android.com/jetpack/compose/"),
                        Uri.parse("http://example.com/")
                    )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isTrue()
            expectThat(
                spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
            ).isFalse()

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }

            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                RESPONSE_LIST_SPACE_QUERY_WITH_SECOND_SPACE_UPDATED
            )
            authenticatedApolloWrapper.registerTestResponse(
                GetSpacesDataQuery(
                    ids = Optional.presentIfNotNull(
                        listOf(MockListSpacesQueryData.SPACE_2.pageMetadata!!.pageID!!)
                    )
                ),
                RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY
            )

            spaceStore.refresh()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id).map { it.url })
                        .containsExactly(
                            Uri.parse(MockListSpacesQueryData.SPACE_1_ITEM_1.spaceEntity!!.url!!)
                        )
                }
                allSpaces[1].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_2.pageMetadata!!.pageID)
                    expectThat(lastModifiedTs).isEqualTo("2099-02-10T02:12:38Z")
                    expectThat(
                        database.spaceDao().getItemsFromSpace(id)
                            .map { it.url }
                    ).containsExactly(
                        Uri.parse("https://reddit.com/r/android")
                    )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isFalse()
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://reddit.com/r/android")
                )
            ).isTrue()

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(4)
                expectThat(get(2)).isA<ListSpacesQuery>()
                expectThat(get(3)).isA<GetSpacesDataQuery>()
            }
        }

    @Test
    fun addToSpace_mutatesAndUpdatesLocalStateOnly() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                MockListSpacesQueryData.LIST_SPACES_QUERY_RESPONSE
            )
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY,
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY_RESPONSE
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id).map { it.url })
                        .containsExactly(
                            Uri.parse(MockListSpacesQueryData.SPACE_1_ITEM_1.spaceEntity!!.url!!)
                        )
                }
                allSpaces[1].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_2.pageMetadata!!.pageID)
                    expectThat(lastModifiedTs).isEqualTo("2022-02-10T02:10:38Z")
                    expectThat(
                        database.spaceDao().getItemsFromSpace(id)
                            .map { it.url }
                    ).containsExactly(
                        Uri.parse("https://developer.android.com/jetpack/compose/"),
                        Uri.parse("http://example.com/")
                    )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isTrue()
            expectThat(
                spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
            ).isFalse()

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }

            authenticatedApolloWrapper.registerTestResponse(
                SpaceStore.createAddToSpaceMutation(
                    space = MockListSpacesQueryData.SPACE_2
                        .toSpace(neevaUser.userInfoFlow.value!!.id)!!,
                    url = Uri.parse("https://example.com/"),
                    title = "Example page"
                ),
                RESPONSE_ADD_TO_SPACE_MUTATION
            )

            val success = spaceStore.addOrRemoveFromSpace(
                MockListSpacesQueryData.SPACE_2.pageMetadata?.pageID!!,
                Uri.parse("https://example.com/"),
                "Example page"
            )

            expectThat(success).isTrue()

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(3)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
                expectThat(get(2)).isA<AddToSpaceMutation>()
            }

            expectThat(spaceStore.spaceStoreContainsUrl(Uri.parse("https://example.com/"))).isTrue()
        }

    @Test
    fun deleteFromSpace_mutatesAndUpdatesLocalStateOnly() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.LIST_SPACES_QUERY,
                MockListSpacesQueryData.LIST_SPACES_QUERY_RESPONSE
            )
            authenticatedApolloWrapper.registerTestResponse(
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY,
                MockListSpacesQueryData.GET_SPACES_DATA_BOTH_SPACES_QUERY_RESPONSE
            )

            spaceStore.refresh()
            coroutineScopeRule.scope.testScheduler.advanceUntilIdle()

            database.spaceDao().allSpaces().let { allSpaces ->
                expectThat(allSpaces).hasSize(2)
                allSpaces[0].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_1.pageMetadata!!.pageID)
                    expectThat(database.spaceDao().getItemsFromSpace(id).map { it.url })
                        .containsExactly(
                            Uri.parse(MockListSpacesQueryData.SPACE_1_ITEM_1.spaceEntity!!.url!!)
                        )
                }
                allSpaces[1].apply {
                    expectThat(id)
                        .isEqualTo(MockListSpacesQueryData.SPACE_2.pageMetadata!!.pageID)
                    expectThat(lastModifiedTs)
                        .isEqualTo(MockListSpacesQueryData.SPACE_2.space!!.lastModifiedTs)
                    expectThat(database.spaceDao().getItemsFromSpace(id).map { it.url })
                        .containsExactly(
                            Uri.parse(MockListSpacesQueryData.SPACE_2_ITEM_1.spaceEntity!!.url!!),
                            Uri.parse(MockListSpacesQueryData.SPACE_2_ITEM_2.spaceEntity!!.url!!)
                        )
                }
            }
            expectThat(
                spaceStore.spaceStoreContainsUrl(
                    Uri.parse("https://developer.android.com/jetpack/compose/")
                )
            ).isTrue()
            expectThat(
                spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
            ).isFalse()

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(2)
                expectThat(get(0)).isA<ListSpacesQuery>()
                expectThat(get(1)).isA<GetSpacesDataQuery>()
            }

            val urlToRemove = database.spaceDao()
                .getItemsFromSpace(MockListSpacesQueryData.SPACE_2.pageMetadata?.pageID!!)
                .first()
                .url!!
            authenticatedApolloWrapper.registerTestResponse(
                SpaceStore.createDeleteSpaceResultByURLMutation(
                    MockListSpacesQueryData.SPACE_2.toSpace(neevaUser.userInfoFlow.value!!.id)!!,
                    urlToRemove
                ),
                RESPONSE_DELETE_FROM_SPACE_MUTATION
            )

            val success = spaceStore.addOrRemoveFromSpace(
                spaceID = MockListSpacesQueryData.SPACE_2.pageMetadata?.pageID!!,
                url = urlToRemove,
                title = ""
            )

            expectThat(success).isTrue()

            authenticatedApolloWrapper.testApolloClientWrapper.performedOperations.apply {
                expectThat(this).hasSize(3)
                expectThat(get(2)).isA<DeleteSpaceResultByURLMutation>()
            }

            expectThat(spaceStore.spaceStoreContainsUrl(urlToRemove)).isFalse()
        }

    companion object {
        private val RESPONSE_LIST_SPACE_QUERY_WITH_SECOND_SPACE_UPDATED = ListSpacesQuery.Data(
            listSpaces = ListSpacesQuery.ListSpaces(
                requestID = "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc991",
                space = listOf(
                    MockListSpacesQueryData.SPACE_1,
                    MockListSpacesQueryData.SPACE_2.copy(
                        space = MockListSpacesQueryData.SPACE_2.space!!.copy(
                            lastModifiedTs = "2099-02-10T02:12:38Z"
                        )
                    )
                )
            )
        )

        private val RESPONSE_LIST_SPACE_QUERY_WITH_FIRST_SPACE_DELETED_AND_SECOND_UPDATED =
            ListSpacesQuery.Data(
                listSpaces = ListSpacesQuery.ListSpaces(
                    requestID = "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc991",
                    space = listOf(
                        MockListSpacesQueryData.SPACE_2.copy(
                            space = MockListSpacesQueryData.SPACE_2.space!!.copy(
                                lastModifiedTs = "2099-02-10T02:12:38Z"
                            )
                        )
                    )
                )
            )

        private val RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY = GetSpacesDataQuery.Data(
            getSpace = GetSpacesDataQuery.GetSpace(
                space = listOf(
                    GetSpacesDataQuery.Space(
                        pageMetadata = GetSpacesDataQuery.PageMetadata(
                            pageID = MockListSpacesQueryData.SPACE_2.pageMetadata!!.pageID!!
                        ),
                        space = GetSpacesDataQuery.Space1(
                            thumbnail = "data:image/jpeg;base64,different garbage",
                            entities = listOf(
                                GetSpacesDataQuery.Entity(
                                    metadata = GetSpacesDataQuery.Metadata(
                                        docID = "skadksnflkaamda345"
                                    ),
                                    spaceEntity = GetSpacesDataQuery.SpaceEntity(
                                        url = "https://reddit.com/r/android",
                                        title = "Android subreddit",
                                        snippet = null,
                                        thumbnail = "data:image/jpeg;base64,also garbage",
                                        content = null
                                    )
                                )
                            ),
                            description = null,
                            name = null,
                            owner = null
                        ),
                        stats = null
                    )
                )
            )
        )

        private val RESPONSE_ADD_TO_SPACE_MUTATION = AddToSpaceMutation.Data(
            entityId = "nEgvD5HST7e62eEmfg0kkxx4xnEuNHBeEXxbGcoo"
        )

        private val RESPONSE_DELETE_FROM_SPACE_MUTATION = DeleteSpaceResultByURLMutation.Data(
            deleteSpaceResultByURL = true
        )
    }
}
