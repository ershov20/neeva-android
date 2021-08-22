package com.neeva.app.storage

import android.net.Uri
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.neeva.app.*
import com.neeva.app.type.SpaceACLLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.last

data class SpaceEntityData(
    val url: Uri,
    val title: String?,
    val snippet : String?,
    val thumbnail: String?,
)

data class Space(
    val id: String,
    val name: String,
    val lastModifiedTs : String,
    val thumbnail : String?,
    val resultCount : Int,
    val isDefaultSpace : Boolean,
    val isShared : Boolean,
    val isPublic : Boolean,
    val userACL : SpaceACLLevel,
) {
    val url: Uri = Uri.parse("$appSpacesURL/$id")

    var contentURLs: Set<Uri>? = null
    var contentData: List<SpaceEntityData>? = null
}

class SpaceStore {
    companion object {
        val shared = SpaceStore()
    }

    enum class State {
        READY,
        REFRESHING,
        FAILED
    }

    var allSpaces = listOf<Space>()
    private var urlToSpacesMap = HashMap<Uri, ArrayList<Space>>()
    val stateFlow = MutableStateFlow(State.READY)

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun refresh() {
        stateFlow.emit(State.REFRESHING)
        val response = apolloClient(NeevaBrowser.context).query(
            ListSpacesQuery()
        ).await()
        if (response.hasErrors()) stateFlow.emit(State.FAILED)
        response.data?.listSpaces?.let { listSpaces ->
            val oldSpaceMap = HashMap<String, Space>()
            allSpaces.forEach { oldSpaceMap[it.id] = it }

            // Clear to avoid holding stale data. Will be rebuilt below.
            urlToSpacesMap = HashMap()

            var spacesToFetch = arrayListOf<Space>()

            allSpaces = listSpaces.space.map { listSpacesQuery ->
                val id = listSpacesQuery.pageMetadata?.pageID ?: return@map null
                val space = listSpacesQuery.space ?: return@map null
                val name = space.name ?: return@map null
                val lastModifiedTs = space.lastModifiedTs ?: return@map null
                val userACL = space.userACL?.acl ?: return@map null
                val newSpace = Space(
                    id = id,
                    name = name,
                    lastModifiedTs = lastModifiedTs,
                    thumbnail = space.thumbnail,
                    resultCount = space.resultCount ?: 0,
                    isDefaultSpace = space.isDefaultSpace ?: false,
                    isShared = space.acl?.map { it.userID }?.any { it == NeevaUserInfo.shared.id }
                        ?: false,
                    isPublic = space.hasPublicACL ?: false,
                    userACL = userACL
                )

                val oldSpace = oldSpaceMap[id]

                if (oldSpace != null && oldSpace.lastModifiedTs == newSpace.lastModifiedTs
                    && oldSpace.contentData != null
                    && oldSpace.contentURLs != null)
                {
                    onUpdateSpaceURLs(newSpace, oldSpace.contentURLs!!, oldSpace.contentData!!)
                } else {
                    spacesToFetch.add(newSpace)
                }

                newSpace
            }.filterNotNull()

            val response = apolloClient(NeevaBrowser.context).query(
                GetSpacesDataQuery(Input.fromNullable(spacesToFetch.map { it.id }))
            ).await()

            response.data?.getSpace?.space?.let { it ->
                it.forEach { spaceQuery ->
                    val entityQueries = spaceQuery.space?.entities ?: return@let
                    val entities = entityQueries.map { entityQuery ->
                        val url = Uri.parse(entityQuery.spaceEntity?.url) ?: return@map null
                        SpaceEntityData(url, title = entityQuery.spaceEntity?.title,
                            snippet = entityQuery.spaceEntity?.snippet,
                            thumbnail = entityQuery.spaceEntity?.thumbnail)
                    }.filterNotNull()
                    val contentUrls = mutableSetOf<Uri>()
                    entities.forEach { contentUrls.add(it.url) }
                    val space = spacesToFetch.first {   space ->
                        space.id == spaceQuery.pageMetadata?.pageID
                    }
                    onUpdateSpaceURLs(space, contentUrls, entities)
                }
            }

            stateFlow.emit(State.READY)
        }
    }

    private fun onUpdateSpaceURLs(space: Space, urls: Set<Uri>, data: List<SpaceEntityData>) {
        space.contentURLs = urls
        space.contentData = data
        urls.forEach {
            val spaces = urlToSpacesMap[it] ?: arrayListOf<Space>()
            spaces.add(space)
            urlToSpacesMap[it] = spaces
        }
    }
}

