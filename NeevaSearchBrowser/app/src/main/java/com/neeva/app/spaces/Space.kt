package com.neeva.app.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.asLiveData
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.neeva.app.*
import com.neeva.app.NeevaConstants.appSpacesURL
import com.neeva.app.type.AddSpaceResultByURLInput
import com.neeva.app.type.DeleteSpaceResultByURLInput
import com.neeva.app.type.SpaceACLLevel
import kotlinx.coroutines.flow.MutableStateFlow

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
    companion object {
        val defaultThumbnail: Bitmap by lazy {
            BitmapFactory.decodeResource(NeevaBrowser.context.resources, R.drawable.spaces)
        }
    }
    val url: Uri = Uri.parse("$appSpacesURL/$id")

    var contentURLs: Set<Uri>? = null
    var contentData: List<SpaceEntityData>? = null

    fun thumbnailAsBitmap() : Bitmap {
        val encoded = thumbnail ?: return defaultThumbnail
        if (!thumbnail.startsWith("data:image/jpeg;base64,")) return defaultThumbnail
        val byteArray = Base64.decode(encoded.substring("data:image/jpeg;base64,".length), Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    suspend fun addOrRemove(url: Uri, title: String, description: String? = null) {
        if (contentURLs?.contains(url) == true) {
            val response = apolloClient(NeevaBrowser.context).mutate(
                DeleteSpaceResultByURLMutation(input =
                DeleteSpaceResultByURLInput(
                    spaceID = id,
                    url = url.toString(),
                )
                )
            ).await()

            response.data?.deleteSpaceResultByURL.let {
                // TODO Add a toast here
                android.util.Log.i("Spaces","Deleted item from space")
            }
        } else {
            val response = apolloClient(NeevaBrowser.context).mutate(
                AddToSpaceMutation(input =
                AddSpaceResultByURLInput(
                    spaceID = id,
                    url = url.toString(),
                    title = title,
                    data = description?.let { Input.fromNullable(it)} ?: Input.absent(),
                    mediaType = Input.fromNullable("text/plain"),
                    snapshotExpected = Input.fromNullable(false)
                )
                )
            ).await()

            response.data?.entityId.let {
                // TODO Add a toast here
                android.util.Log.i("Spaces","Added item to space with id=$it")
            }
        }
    }
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
    var allSpacesFlow = MutableStateFlow(listOf<Space>())
    var allSpaces = allSpacesFlow.asLiveData()
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
            allSpaces.value?.forEach { oldSpaceMap[it.id] = it }

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

            allSpacesFlow.emit(spaceList)

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

