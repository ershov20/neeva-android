package com.neeva.app.spaces

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.apollographql.apollo3.api.Optional
import com.neeva.app.AddToSpaceMutation
import com.neeva.app.ApolloWrapper
import com.neeva.app.DeleteSpaceResultByURLMutation
import com.neeva.app.Dispatchers
import com.neeva.app.GetSpacesDataQuery
import com.neeva.app.ListSpacesQuery
import com.neeva.app.R
import com.neeva.app.storage.BitmapIO
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.type.AddSpaceResultByURLInput
import com.neeva.app.type.DeleteSpaceResultByURLInput
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.userdata.NeevaUser
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/** Manages interactions with the user's Spaces. */
class SpaceStore(
    private val appContext: Context,
    private val historyDatabase: HistoryDatabase,
    private val coroutineScope: CoroutineScope,
    private val apolloWrapper: ApolloWrapper,
    private val neevaUser: NeevaUser,
    private val snackbarModel: SnackbarModel,
    private val dispatchers: Dispatchers
) {
    companion object {
        private val TAG = SpaceStore::class.simpleName
        private const val DIRECTORY = "spaces"
    }

    enum class State {
        READY,
        REFRESHING,
        FAILED
    }

    private val dao = historyDatabase.spaceDao()

    val allSpacesFlow = dao.allSpacesFlow()
        .flowOn(dispatchers.io)
        .distinctUntilChanged()
        .stateIn(coroutineScope, SharingStarted.Lazily, emptyList())
    val editableSpacesFlow = allSpacesFlow
        .map { it.filterNot { space -> space.userACL >= SpaceACLLevel.Edit } }
    val stateFlow = MutableStateFlow(State.READY)

    @VisibleForTesting
    val thumbnailDirectory = File(appContext.cacheDir, DIRECTORY)

    private var isRefreshPending: Boolean = false
    private var cleanUpThumbnails: Boolean = true
    private var lastFetchedSpaceIds = emptyList<String>()

    suspend fun spaceStoreContainsUrl(url: Uri): Boolean = spaceIDsContainingURL(url).isNotEmpty()

    suspend fun refresh(space: Space? = null) {
        if (neevaUser.neevaUserToken.getToken().isEmpty()) return
        // TODO(yusuf) : Early return here if there is no connectivity

        if (stateFlow.value == State.REFRESHING) {
            isRefreshPending = true
            return
        }

        stateFlow.value = State.REFRESHING
        val succeeded = if (space == null) {
            performRefresh()
        } else {
            performFetch(listOf(space))
        }

        stateFlow.value = if (succeeded) {
            State.READY
        } else {
            State.FAILED
        }

        withContext(dispatchers.io) {
            cleanupDatabaseAfterRefresh()
        }

        if (cleanUpThumbnails) {
            cleanUpThumbnails = false
            withContext(dispatchers.io) {
                cleanupSpacesThumbnails()
            }
        }

        if (isRefreshPending) {
            isRefreshPending = false
            refresh()
        }
    }

    private suspend fun performRefresh(): Boolean = withContext(dispatchers.io) {
        val response =
            apolloWrapper.performQuery(ListSpacesQuery(), userMustBeLoggedIn = true)
                ?: return@withContext false

        // If there are no spaces to process, but the response was fine, just indicate success.
        val listSpaces = response.data?.listSpaces ?: return@withContext true
        val oldSpaceMap = dao.allSpaces().associateBy { it.id }

        // Fetch all the of the user's Spaces.
        val spacesToFetch = mutableListOf<Space>()

        lastFetchedSpaceIds = listSpaces.space
            .map { listSpacesQuery ->
                val newSpace = listSpacesQuery.toSpace(
                    neevaUser.data.id
                ) ?: return@map null

                val oldSpace = oldSpaceMap[newSpace.id]
                newSpace.thumbnail = oldSpace?.thumbnail
                if (oldSpace == null || oldSpace.lastModifiedTs != newSpace.lastModifiedTs) {
                    spacesToFetch.add(newSpace)
                }
                dao.upsert(newSpace)

                newSpace
            }
            .mapNotNull { it?.id }

        return@withContext performFetch(spacesToFetch)
    }

    private suspend fun performFetch(spacesToFetch: List<Space>): Boolean {
        if (spacesToFetch.isEmpty()) return true

        // Get updated data for any Spaces that have changed since the last fetch.
        val spacesDataResponse = apolloWrapper.performQuery(
            GetSpacesDataQuery(Optional.presentIfNotNull(spacesToFetch.map { it.id })),
            userMustBeLoggedIn = true
        ) ?: return false

        spacesDataResponse.data?.getSpace?.space?.forEach { spaceQuery ->
            val spaceID = spaceQuery.pageMetadata?.pageID ?: return@forEach
            val entityQueries = spaceQuery.space?.entities ?: return@forEach

            val entities =
                entityQueries.filter { it.metadata?.docID != null }.map { entityQuery ->
                    val thumbnailUri = entityQuery.spaceEntity?.thumbnail?.let {
                        BitmapIO.saveBitmap(
                            thumbnailDirectory.resolve(spaceID),
                            dispatchers,
                            entityQuery.metadata?.docID,
                            it
                        )?.toUri()
                    }
                    SpaceItem(
                        id = entityQuery.metadata?.docID!!,
                        spaceID = spaceID,
                        url = entityQuery.spaceEntity?.url?.let { Uri.parse(it) },
                        title = entityQuery.spaceEntity?.title,
                        snippet = entityQuery.spaceEntity?.snippet,
                        thumbnail = thumbnailUri
                    )
                }
            entities.forEach { dao.upsert(it) }
            dao.getItemsFromSpace(spaceID)
                .filterNot { entities.contains(it) }
                .forEach { dao.deleteSpaceItem(it) }

            spacesToFetch
                .firstOrNull { space -> space.id == spaceID }
                ?.let { space ->
                    space.thumbnail = BitmapIO.saveBitmap(
                        directory = thumbnailDirectory.resolve(space.id),
                        dispatchers = dispatchers,
                        id = spaceID,
                        bitmapString = spaceQuery.space.thumbnail
                    )?.toUri()
                    dao.upsert(space)
                }
        }

        return true
    }

    private suspend fun contentURLsForSpace(spaceID: String) =
        contentDataForSpace(spaceID = spaceID).mapNotNull { it.url }
    private suspend fun contentDataForSpace(spaceID: String) =
        dao.getItemsFromSpace(spaceID = spaceID)
    suspend fun spaceIDsContainingURL(url: Uri?) = dao.getSpaceIDsWithURL(url = url)

    /** Cleans up the [Space] and [SpaceItem] tables by taking [allSpacesFlow] as source of truth */
    private suspend fun cleanupDatabaseAfterRefresh() {
        dao.allSpaceIds()
            .filterNot { lastFetchedSpaceIds.contains(it) }
            .forEach { dao.deleteSpaceById(it) }
        dao.deleteOrphanedSpaceItems()
    }

    suspend fun addOrRemoveFromSpace(
        spaceID: String,
        url: Uri,
        title: String,
        description: String? = null
    ): Boolean = withContext(dispatchers.io) {
        val space = dao.getSpaceById(spaceID) ?: return@withContext false

        return@withContext if (contentURLsForSpace(space.id).contains(url)) {
            removeFromSpace(space, url)
        } else {
            addToSpace(space, url, title, description)
        }
    }

    private suspend fun cleanupSpacesThumbnails() {
        val idList = dao.allSpaceIds()
        thumbnailDirectory
            .list { file, _ -> file.isDirectory }
            ?.filterNot { folderName -> idList.contains(folderName) }
            ?.forEach {
                thumbnailDirectory.resolve(it).deleteRecursively()
            }
    }

    suspend fun addToSpace(
        space: Space,
        url: Uri,
        title: String,
        description: String? = null
    ): Boolean {
        val spaceID = space.id
        val response = apolloWrapper.performMutation(
            AddToSpaceMutation(
                input = AddSpaceResultByURLInput(
                    spaceID = spaceID,
                    url = url.toString(),
                    title = title,
                    data = description?.let { Optional.presentIfNotNull(it) }
                        ?: Optional.Absent,
                    mediaType = Optional.presentIfNotNull("text/plain")
                )
            ),
            userMustBeLoggedIn = true
        )

        return response?.data?.entityId?.let {
            Log.i(TAG, "Added item to space with id=$it")
            snackbarModel.show(appContext.getString(R.string.space_add_url, space.name))
            refresh(space = space)
            true
        } ?: run {
            val errorString = appContext.getString(R.string.generic_error)
            snackbarModel.show(errorString)
            false
        }
    }

    suspend fun removeFromSpace(space: Space, uri: Uri): Boolean {
        val spaceID = space.id
        val response = apolloWrapper.performMutation(
            DeleteSpaceResultByURLMutation(
                input = DeleteSpaceResultByURLInput(
                    spaceID = spaceID,
                    url = uri.toString(),
                )
            ),
            userMustBeLoggedIn = true
        )

        return response?.data?.deleteSpaceResultByURL?.let {
            val successString = appContext.getString(R.string.space_remove_url, space.name)
            Log.i(TAG, successString)
            snackbarModel.show(successString)
            refresh(space = space)
            true
        } ?: run {
            val errorString = appContext.getString(R.string.generic_error)
            snackbarModel.show(errorString)
            false
        }
    }
}
