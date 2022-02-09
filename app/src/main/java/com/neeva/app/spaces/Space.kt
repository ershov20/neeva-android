package com.neeva.app.spaces

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.neeva.app.AddToSpaceMutation
import com.neeva.app.DeleteSpaceResultByURLMutation
import com.neeva.app.GetSpacesDataQuery
import com.neeva.app.ListSpacesQuery
import com.neeva.app.NeevaConstants.appSpacesURL
import com.neeva.app.storage.NeevaUser
import com.neeva.app.type.AddSpaceResultByURLInput
import com.neeva.app.type.DeleteSpaceResultByURLInput
import com.neeva.app.type.SpaceACLLevel
import kotlinx.coroutines.flow.MutableStateFlow

data class SpaceEntityData(
    val url: Uri,
    val title: String?,
    val snippet: String?,
    val thumbnail: String?,
)

data class Space(
    val id: String,
    val name: String,
    val lastModifiedTs: String,
    val thumbnail: String?,
    val resultCount: Int,
    val isDefaultSpace: Boolean,
    val isShared: Boolean,
    val isPublic: Boolean,
    val userACL: SpaceACLLevel,
) {
    companion object {
        fun interface SpaceModifier {
            fun addOrRemoveCurrentTabToSpace(space: Space)
        }
    }
    val url: Uri = Uri.parse("$appSpacesURL/$id")

    var contentURLs: Set<Uri>? = null
    var contentData: List<SpaceEntityData>? = null

    fun thumbnailAsBitmap(): Bitmap? {
        val encoded = thumbnail ?: return null
        if (!thumbnail.startsWith("data:image/jpeg;base64,")) return null
        val byteArray = Base64.decode(
            encoded.substring("data:image/jpeg;base64,".length),
            Base64.DEFAULT
        )
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    suspend fun addOrRemove(
        apolloClient: ApolloClient,
        url: Uri,
        title: String,
        description: String? = null
    ) {
        if (contentURLs?.contains(url) == true) {
            val response = apolloClient
                .mutation(
                    DeleteSpaceResultByURLMutation(
                        input = DeleteSpaceResultByURLInput(
                            spaceID = id,
                            url = url.toString(),
                        )
                    )
                ).execute()

            response.data?.deleteSpaceResultByURL.let {
                // TODO Add a toast here
                android.util.Log.i("Spaces", "Deleted item from space")
            }
        } else {
            val response = apolloClient
                .mutation(
                    AddToSpaceMutation(
                        input = AddSpaceResultByURLInput(
                            spaceID = id,
                            url = url.toString(),
                            title = title,
                            data = description?.let { Optional.presentIfNotNull(it) }
                                ?: Optional.Absent,
                            mediaType = Optional.presentIfNotNull("text/plain")
                        )
                    )
                ).execute()

            response.data?.entityId.let {
                // TODO Add a toast here
                android.util.Log.i("Spaces", "Added item to space with id=$it")
            }
        }
    }
}

class SpaceStore(val apolloClient: ApolloClient, val neevaUser: NeevaUser) {
    enum class State {
        READY,
        REFRESHING,
        FAILED
    }
    val allSpacesFlow = MutableStateFlow<List<Space>>(emptyList())
    val editableSpacesFlow = MutableStateFlow<List<Space>>(emptyList())
    private var urlToSpacesMap = HashMap<Uri, ArrayList<Space>>()
    val stateFlow = MutableStateFlow(State.READY)

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun refresh() {
        stateFlow.emit(State.REFRESHING)
        val response = apolloClient
            .query(ListSpacesQuery())
            .execute()

        if (response.hasErrors()) stateFlow.emit(State.FAILED)

        response.data?.listSpaces?.let { listSpaces ->
            val oldSpaceMap = HashMap<String, Space>()
            allSpacesFlow.value.forEach { oldSpaceMap[it.id] = it }

            // Clear to avoid holding stale data. Will be rebuilt below.
            urlToSpacesMap = HashMap()

            var spacesToFetch = arrayListOf<Space>()

            val spaceList = listSpaces.space.map { listSpacesQuery ->
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
                    isShared = space.acl?.map { it.userID }?.any { it == neevaUser.data.id }
                        ?: false,
                    isPublic = space.hasPublicACL ?: false,
                    userACL = userACL
                )

                val oldSpace = oldSpaceMap[id]

                if (oldSpace != null && oldSpace.lastModifiedTs == newSpace.lastModifiedTs &&
                    oldSpace.contentData != null &&
                    oldSpace.contentURLs != null
                ) {
                    onUpdateSpaceURLs(newSpace, oldSpace.contentURLs!!, oldSpace.contentData!!)
                } else {
                    spacesToFetch.add(newSpace)
                }

                newSpace
            }.filterNotNull()

            allSpacesFlow.value = spaceList
            editableSpacesFlow.value =
                spaceList.filter {
                    it.userACL == SpaceACLLevel.Owner || it.userACL == SpaceACLLevel.Edit
                }

            val spacesDataResponse = apolloClient
                .query(GetSpacesDataQuery(Optional.presentIfNotNull(spacesToFetch.map { it.id })))
                .execute()

            spacesDataResponse.data?.getSpace?.space?.let { it ->
                it.forEach { spaceQuery ->
                    val entityQueries = spaceQuery.space?.entities ?: return@let
                    val entities = entityQueries.map { entityQuery ->
                        val url = Uri.parse(entityQuery.spaceEntity?.url) ?: return@map null
                        SpaceEntityData(
                            url, title = entityQuery.spaceEntity?.title,
                            snippet = entityQuery.spaceEntity?.snippet,
                            thumbnail = entityQuery.spaceEntity?.thumbnail
                        )
                    }.filterNotNull()
                    val contentUrls = mutableSetOf<Uri>()
                    entities.forEach { contentUrls.add(it.url) }
                    val space = spacesToFetch.first { space ->
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
