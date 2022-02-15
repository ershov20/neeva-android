package com.neeva.app.spaces

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.BaseTest
import com.neeva.app.GetSpacesDataQuery
import com.neeva.app.ListSpacesQuery
import com.neeva.app.NeevaUserToken
import com.neeva.app.TestApolloWrapper
import com.neeva.app.storage.NeevaUser
import com.neeva.app.storage.NeevaUserData
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.SnackbarModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
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

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class SpaceStoreTest : BaseTest() {
    @Mock
    private lateinit var snackbarModel: SnackbarModel

    private lateinit var context: Context
    private lateinit var neevaUser: NeevaUser
    private lateinit var apolloWrapper: TestApolloWrapper
    private lateinit var spaceStore: SpaceStore

    override fun setUp() {
        super.setUp()

        context = ApplicationProvider.getApplicationContext()
        val neevaUserToken: NeevaUserToken = mock()
        neevaUser = NeevaUser(
            data = NeevaUserData("c5rgtdldv9enb8j1gupg"),
            neevaUserToken = neevaUserToken
        )
        apolloWrapper = TestApolloWrapper(neevaUserToken = neevaUserToken)
        spaceStore = SpaceStore(context, apolloWrapper, neevaUser, snackbarModel)
    }

    override fun tearDown() {
        apolloWrapper.tearDown()
        super.tearDown()
    }

    @Test
    fun convertApolloSpace_givenAllRequiredData_createsSpace() {
        val original = SPACE_1
        original.toSpace(neevaUser.data.id)!!.apply {
            expectThat(id).isEqualTo("c5rgtmtdv9enb8j1gv60")
            expectThat(name).isEqualTo("Saved For Later")
            expectThat(lastModifiedTs).isEqualTo("2022-02-10T22:08:01Z")
            expectThat(thumbnail).isEqualTo("thumbnail data URI")
            expectThat(resultCount).isEqualTo(0)
            expectThat(isDefaultSpace).isTrue()
            expectThat(userACL).isEqualTo(SpaceACLLevel.Owner)
            expectThat(isPublic).isTrue()
            expectThat(isShared).isTrue()
        }
    }

    @Test
    fun convertApolloSpace_properlyHandlesAclInfo() {
        neevaUser.data = NeevaUserData(id = "wrong id")
        val original = SPACE_2.copy(
            space = SPACE_2.space!!.copy(hasPublicACL = false)
        )
        original.toSpace(neevaUser.data.id)!!.apply {
            expectThat(userACL).isEqualTo(SpaceACLLevel.Comment)
            expectThat(isPublic).isFalse()
            expectThat(isShared).isFalse()
        }
    }

    @Test
    fun convertApolloSpace_withMissingData_returnsNull() {
        expectThat(SPACE_1.copy(pageMetadata = null).toSpace(neevaUser.data.id)).isNull()

        expectThat(SPACE_1.copy(space = null).toSpace(neevaUser.data.id)).isNull()

        expectThat(
            SPACE_1.copy(space = SPACE_1.space!!.copy(name = null)).toSpace(neevaUser.data.id)
        ).isNull()

        expectThat(
            SPACE_1
                .copy(space = SPACE_1.space!!.copy(lastModifiedTs = null))
                .toSpace(neevaUser.data.id)
        ).isNull()

        expectThat(
            SPACE_1.copy(space = SPACE_1.space!!.copy(userACL = null)).toSpace(neevaUser.data.id)
        ).isNull()
    }

    @Test
    fun refresh_withValidSpaceListQueryResponse_returnsBothSpaces() = runTest {
        // Only allow the response to fetch the user's Spaces to succeed.
        apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)

        spaceStore.refresh()

        // The refresh will get data about the Spaces but not their contents.
        val allSpaces = spaceStore.allSpacesFlow.value
        expectThat(allSpaces).hasSize(2)
        allSpaces[0].apply {
            expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
            expectThat(contentURLs).isNull()
        }
        allSpaces[1].apply {
            expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
            expectThat(contentURLs).isNull()
        }

        expectThat(apolloWrapper.performedQueries).hasSize(2)
        expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
        expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
        expectThat(apolloWrapper.performedMutations).isEmpty()
    }

    @Test
    fun refresh_withValidResponses_returnsAllData() = runTest {
        apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)
        apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY)

        spaceStore.refresh()

        spaceStore.allSpacesFlow.value.let { allSpaces ->
            expectThat(allSpaces).hasSize(2)
            allSpaces[0].apply {
                expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                expectThat(contentURLs!!).isEmpty()
            }
            allSpaces[1].apply {
                expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
                expectThat(contentURLs!!).containsExactly(
                    Uri.parse("https://developer.android.com/jetpack/compose/testing")
                )
            }
        }
        spaceStore.editableSpacesFlow.value.let { editableSpaces ->
            expectThat(editableSpaces).hasSize(1)
            editableSpaces[0].apply {
                expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                expectThat(contentURLs!!).isEmpty()
            }
        }
        expectThat(
            spaceStore.spaceStoreContainsUrl(
                Uri.parse("https://developer.android.com/jetpack/compose/testing")
            )
        ).isTrue()

        expectThat(apolloWrapper.performedQueries).hasSize(2)
        expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
        expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
        expectThat(apolloWrapper.performedMutations).isEmpty()
    }

    @Test
    fun refresh_updatesWhenSpaceTimestampChanges() = runTest {
        apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY)
        apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY)

        spaceStore.refresh()

        spaceStore.allSpacesFlow.value.let { allSpaces ->
            expectThat(allSpaces).hasSize(2)
            allSpaces[0].apply {
                expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                expectThat(contentURLs!!).isEmpty()
            }
            allSpaces[1].apply {
                expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
                expectThat(lastModifiedTs).isEqualTo("2022-02-10T02:10:38Z")
                expectThat(contentURLs!!).containsExactly(
                    Uri.parse("https://developer.android.com/jetpack/compose/testing")
                )
            }
        }
        expectThat(
            spaceStore.spaceStoreContainsUrl(
                Uri.parse("https://developer.android.com/jetpack/compose/testing")
            )
        ).isTrue()
        expectThat(
            spaceStore.spaceStoreContainsUrl(Uri.parse("https://reddit.com/r/android"))
        ).isFalse()

        expectThat(apolloWrapper.performedQueries).hasSize(2)
        expectThat(apolloWrapper.performedQueries[0]).isA<ListSpacesQuery>()
        expectThat(apolloWrapper.performedQueries[1]).isA<GetSpacesDataQuery>()
        expectThat(apolloWrapper.performedMutations).isEmpty()

        apolloWrapper.addResponse(RESPONSE_LIST_SPACE_QUERY_WITH_SECOND_SPACE_UPDATED)
        apolloWrapper.addResponse(RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY)

        spaceStore.refresh()

        spaceStore.allSpacesFlow.value.let { allSpaces ->
            expectThat(allSpaces).hasSize(2)
            allSpaces[0].apply {
                expectThat(id).isEqualTo(SPACE_1.pageMetadata!!.pageID)
                expectThat(contentURLs!!).isEmpty()
            }
            allSpaces[1].apply {
                expectThat(id).isEqualTo(SPACE_2.pageMetadata!!.pageID)
                expectThat(lastModifiedTs).isEqualTo("2099-02-10T02:12:38Z")
                expectThat(contentURLs!!).containsExactly(
                    Uri.parse("https://reddit.com/r/android")
                )
            }
        }
        expectThat(
            spaceStore.spaceStoreContainsUrl(
                Uri.parse("https://developer.android.com/jetpack/compose/testing")
            )
        ).isFalse()
        expectThat(
            spaceStore.spaceStoreContainsUrl(
                Uri.parse("https://reddit.com/r/android")
            )
        ).isTrue()

        expectThat(apolloWrapper.performedQueries).hasSize(4)
        expectThat(apolloWrapper.performedQueries[2]).isA<ListSpacesQuery>()
        expectThat(apolloWrapper.performedQueries[3]).isA<GetSpacesDataQuery>()
        expectThat(apolloWrapper.performedMutations).isEmpty()
    }

    companion object {
        private val SPACE_1 = ListSpacesQuery.Space(
            pageMetadata = ListSpacesQuery.PageMetadata(pageID = "c5rgtmtdv9enb8j1gv60"),
            space = ListSpacesQuery.Space1(
                name = "Saved For Later",
                lastModifiedTs = "2022-02-10T22:08:01Z",
                userACL = ListSpacesQuery.UserACL(acl = SpaceACLLevel.Owner),
                acl = listOf(ListSpacesQuery.Acl("c5rgtdldv9enb8j1gupg")),
                hasPublicACL = true,
                thumbnail = "thumbnail data URI",
                thumbnailSize = ListSpacesQuery.ThumbnailSize(320, 320),
                resultCount = null,
                isDefaultSpace = true
            )
        )
        private val SPACE_2 = ListSpacesQuery.Space(
            pageMetadata = ListSpacesQuery.PageMetadata(
                pageID = "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
            ),
            space = ListSpacesQuery.Space1(
                name = "Jetpack Compose",
                lastModifiedTs = "2022-02-10T02:10:38Z",
                userACL = ListSpacesQuery.UserACL(acl = SpaceACLLevel.Comment),
                acl = listOf(ListSpacesQuery.Acl("c5rgtdldv9enb8j1gupg")),
                hasPublicACL = false,
                thumbnail = "data:image/jpeg;base64,asdf",
                thumbnailSize = ListSpacesQuery.ThumbnailSize(320, 320),
                resultCount = 1,
                isDefaultSpace = false
            )
        )

        private val RESPONSE_LIST_SPACE_QUERY = """{
            "data": {
                "listSpaces": {
                    "requestID": "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc991",
                    "space": [
                        {
                            "pageMetadata": {
                                "pageID": "c5rgtmtdv9enb8j1gv60"
                            },
                            "space": {
                                "name": "Saved For Later",
                                "lastModifiedTs": "2022-02-10T22:08:01Z",
                                "userACL": {
                                    "acl": "Owner"
                                },
                                "acl": [{
                                    "userID": "c5rgtdldv9enb8j1gupg"
                                }],
                                "hasPublicACL": false,
                                "thumbnail": "data:image/jpeg;base64,XXXXX",
                                "thumbnailSize": {
                                    "height": 320,
                                    "width": 320
                                },
                                "resultCount": null,
                                "isDefaultSpace": true
                            }
                        },
                        {
                            "pageMetadata": {
                                "pageID": "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space": {
                                "name": "Jetpack Compose",
                                "lastModifiedTs": "2022-02-10T02:10:38Z",
                                "userACL": {
                                    "acl": "Comment"
                                },
                                "acl": [{
                                    "userID": "c5rgtdldv9enb8j1gupg"
                                }],
                                "hasPublicACL": false,
                                "thumbnail": "data:image/jpeg;base64,asdf",
                                "thumbnailSize": {
                                    "height": 320,
                                    "width": 320
                                },
                                "resultCount": 1,
                                "isDefaultSpace": false
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_LIST_SPACE_QUERY_WITH_SECOND_SPACE_UPDATED = """{
            "data": {
                "listSpaces": {
                    "requestID": "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc992",
                    "space": [
                        {
                            "pageMetadata": {
                                "pageID": "c5rgtmtdv9enb8j1gv60"
                            },
                            "space": {
                                "name": "Saved For Later",
                                "lastModifiedTs": "2022-02-10T22:08:01Z",
                                "userACL": {
                                    "acl": "Owner"
                                },
                                "acl": [{
                                    "userID": "c5rgtdldv9enb8j1gupg"
                                }],
                                "hasPublicACL": false,
                                "thumbnail": "data:image/jpeg;base64,XXXXX",
                                "thumbnailSize": {
                                    "height": 320,
                                    "width": 320
                                },
                                "resultCount": null,
                                "isDefaultSpace": true
                            }
                        },
                        {
                            "pageMetadata": {
                                "pageID": "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space": {
                                "name": "Jetpack Compose",
                                "lastModifiedTs": "2099-02-10T02:12:38Z",
                                "userACL": {
                                    "acl": "Comment"
                                },
                                "acl": [{
                                    "userID": "c5rgtdldv9enb8j1gupg"
                                }],
                                "hasPublicACL": false,
                                "thumbnail": "data:image/jpeg;base64,asdf",
                                "thumbnailSize": {
                                    "height": 320,
                                    "width": 320
                                },
                                "resultCount": 1,
                                "isDefaultSpace": false
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_GET_SPACES_DATA_QUERY = """{
            "data":{
                "getSpace":{
                    "space":[
                        {
                            "pageMetadata":{
                                "pageID":"c5rgtmtdv9enb8j1gv60"
                            },
                            "space":{
                                "entities":[]
                            }
                        },
                        {
                            "pageMetadata":{
                                "pageID":"nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space":{
                                "entities":[
                                    {
                                        "spaceEntity":{
                                            "url":"https://developer.android.com/jetpack/compose/testing",
                                            "title":"Testing your Compose layout | Jetpack Compose | Android Developers",
                                            "snippet":null,
                                            "thumbnail":"data:image/jpeg;base64,garbage"
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        private val RESPONSE_GET_SPACES_DATA_QUERY_SECOND_SPACE_ONLY = """{
            "data":{
                "getSpace":{
                    "space":[
                        {
                            "pageMetadata":{
                                "pageID":"nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
                            },
                            "space":{
                                "entities":[
                                    {
                                        "spaceEntity":{
                                            "url":"https://reddit.com/r/android",
                                            "title":"Android subreddit",
                                            "snippet":null,
                                            "thumbnail":"data:image/jpeg;base64,also garbage"
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()
    }
}
