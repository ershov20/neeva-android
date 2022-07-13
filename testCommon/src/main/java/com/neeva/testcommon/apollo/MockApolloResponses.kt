package com.neeva.testcommon.apollo

import com.apollographql.apollo3.api.Optional
import com.neeva.app.GetSpacesDataQuery
import com.neeva.app.ListSpacesQuery
import com.neeva.app.UserInfoQuery
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.type.SubscriptionType
import com.neeva.app.userdata.NeevaUser
import com.neeva.testcommon.WebpageServingRule

val RESPONSE_USER_INFO_QUERY = UserInfoQuery.Data(
    user = UserInfoQuery.User(
        id = "UserID",
        profile = UserInfoQuery.Profile(
            displayName = "Injected user",
            email = "injected@email.com",
            pictureURL = WebpageServingRule.urlFor("image.png")
        ),
        flags = emptyList(),
        featureFlags = emptyList(),
        authProvider = NeevaUser.SSOProvider.OKTA.url,
        subscriptionType = SubscriptionType.Basic
    )
)

/**
 * Provides data that indicates that the user has 2 spaces, where the first space has 1 item and the
 * second space has 2 items.
 */
object MockListSpacesQueryData {
    /** Space that the user has full control of. */
    val SPACE_1 = ListSpacesQuery.Space(
        pageMetadata = ListSpacesQuery.PageMetadata(pageID = "c5rgtmtdv9enb8j1gv60"),
        stats = null,
        space = ListSpacesQuery.Space1(
            name = "Saved For Later",
            description = null,
            lastModifiedTs = "2022-02-10T22:08:01Z",
            userACL = ListSpacesQuery.UserACL(acl = SpaceACLLevel.Owner),
            acl = listOf(ListSpacesQuery.Acl("c5rgtdldv9enb8j1gupg")),
            hasPublicACL = true,
            resultCount = 1,
            isDefaultSpace = true,
            owner = null,
        )
    )

    /** Space that the user is not allowed to directly edit. */
    val SPACE_2 = ListSpacesQuery.Space(
        pageMetadata = ListSpacesQuery.PageMetadata(
            pageID = "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo"
        ),
        stats = null,
        space = ListSpacesQuery.Space1(
            name = "Jetpack Compose",
            description = null,
            lastModifiedTs = "2022-02-10T02:10:38Z",
            userACL = ListSpacesQuery.UserACL(acl = SpaceACLLevel.Comment),
            acl = listOf(ListSpacesQuery.Acl("c5rgtdldv9enb8j1gupg")),
            hasPublicACL = false,
            resultCount = 2,
            isDefaultSpace = false,
            owner = null
        )
    )

    val SPACE_1_ITEM_1 =
        GetSpacesDataQuery.Entity(
            metadata = GetSpacesDataQuery.Metadata(
                docID = "space 1 item 1"
            ),
            spaceEntity = GetSpacesDataQuery.SpaceEntity(
                url = "https://space1.com/item1",
                title = "Space 1, Item 1",
                snippet = null,
                thumbnail = "data:image/jpeg;base64,garbage",
                content = null
            )
        )

    val SPACE_2_ITEM_1 =
        GetSpacesDataQuery.Entity(
            metadata = GetSpacesDataQuery.Metadata(
                docID = "skadksnflkaamda345"
            ),
            spaceEntity = GetSpacesDataQuery.SpaceEntity(
                url = "https://developer.android.com/jetpack/compose/",
                title = "Testing your Compose layout | Jetpack Compose",
                snippet = null,
                thumbnail = "data:image/jpeg;base64,garbage",
                content = null
            )
        )

    val SPACE_2_ITEM_2 = GetSpacesDataQuery.Entity(
        metadata = GetSpacesDataQuery.Metadata(
            docID = "ksjkjkdadkma"
        ),
        spaceEntity = GetSpacesDataQuery.SpaceEntity(
            url = "http://example.com/",
            title = "Just another website",
            snippet = null,
            thumbnail = "data:image/jpeg;base64,garbage also",
            content = null
        )
    )

    val LIST_SPACES_QUERY = ListSpacesQuery()
    val LIST_SPACES_QUERY_RESPONSE = ListSpacesQuery.Data(
        listSpaces = ListSpacesQuery.ListSpaces(
            requestID = "1644533881932776642~1~745d8580e31ea8a5296d25694bf061d0ef0cc991",
            space = listOf(SPACE_1, SPACE_2)
        )
    )

    val GET_SPACES_DATA_BOTH_SPACES_QUERY = GetSpacesDataQuery(
        ids = Optional.presentIfNotNull(
            listOf(SPACE_1.pageMetadata!!.pageID!!, SPACE_2.pageMetadata!!.pageID!!)
        )
    )
    val GET_SPACES_DATA_BOTH_SPACES_QUERY_RESPONSE = GetSpacesDataQuery.Data(
        getSpace = GetSpacesDataQuery.GetSpace(
            space = listOf(
                GetSpacesDataQuery.Space(
                    pageMetadata = GetSpacesDataQuery.PageMetadata(
                        pageID = "c5rgtmtdv9enb8j1gv60"
                    ),
                    space = GetSpacesDataQuery.Space1(
                        thumbnail = "data:image/jpeg;base64,still garbage",
                        entities = listOf(SPACE_1_ITEM_1),
                        description = null,
                        name = null,
                        owner = null
                    ),
                    stats = null
                ),
                GetSpacesDataQuery.Space(
                    pageMetadata = GetSpacesDataQuery.PageMetadata(
                        pageID = SPACE_2.pageMetadata!!.pageID!!
                    ),
                    space = GetSpacesDataQuery.Space1(
                        thumbnail = "data:image/jpeg;base64,garbage data",
                        entities = listOf(SPACE_2_ITEM_1, SPACE_2_ITEM_2),
                        description = null,
                        name = null,
                        owner = null
                    ),
                    stats = null
                )
            )
        )
    )
}