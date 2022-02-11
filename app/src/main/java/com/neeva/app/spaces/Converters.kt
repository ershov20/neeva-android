package com.neeva.app.spaces

import com.neeva.app.ListSpacesQuery

/**
 * Converts the data provided in the Apollo response into a [Space].
 *
 * @return The [Space] if it was successfully created, null otherwise.
 */
internal fun ListSpacesQuery.Space.toSpace(userId: String?): Space? {
    val id = pageMetadata?.pageID ?: return null
    val querySpace = space ?: return null
    val name = querySpace.name ?: return null
    val lastModifiedTs = querySpace.lastModifiedTs ?: return null
    val userACL = querySpace.userACL?.acl ?: return null

    return Space(
        id = id,
        name = name,
        lastModifiedTs = lastModifiedTs,
        thumbnail = querySpace.thumbnail,
        resultCount = querySpace.resultCount ?: 0,
        isDefaultSpace = querySpace.isDefaultSpace ?: false,
        isShared = querySpace.acl?.any { it.userID == userId } ?: false,
        isPublic = querySpace.hasPublicACL ?: false,
        userACL = userACL
    )
}
