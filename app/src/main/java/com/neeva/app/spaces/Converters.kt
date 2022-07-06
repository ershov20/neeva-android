package com.neeva.app.spaces

import android.net.Uri
import com.neeva.app.ListSpacesQuery
import com.neeva.app.storage.entities.Space

/**
 * Converts the data provided in the Apollo response into a [Space].
 *
 * @return The [Space] if it was successfully created, null otherwise.
 */
internal fun ListSpacesQuery.Space.toSpace(
    userId: String?
): Space? {
    val id = pageMetadata?.pageID ?: return null
    val querySpace = space ?: return null
    val name = querySpace.name ?: return null
    val lastModifiedTs = querySpace.lastModifiedTs ?: return null
    val userACL = querySpace.userACL?.acl ?: return null

    return Space(
        id = id,
        name = name,
        description = querySpace.description ?: "",
        lastModifiedTs = lastModifiedTs,
        thumbnail = null,
        resultCount = querySpace.resultCount ?: 0,
        isDefaultSpace = querySpace.isDefaultSpace ?: false,
        isShared = querySpace.acl?.any { it.userID == userId } ?: false,
        isPublic = querySpace.hasPublicACL ?: false,
        userACL = userACL,
        ownerName = querySpace.owner?.displayName ?: "",
        ownerPictureURL = querySpace.owner?.pictureURL?.let { Uri.parse(it) },
        numViews = stats?.views ?: 0,
        numFollowers = stats?.followers ?: 0
    )
}
