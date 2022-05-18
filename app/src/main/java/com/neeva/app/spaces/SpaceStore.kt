package com.neeva.app.spaces

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.apollographql.apollo3.api.Optional
import com.neeva.app.AddToSpaceMutation
import com.neeva.app.AuthenticatedApolloWrapper
import com.neeva.app.CreateSpaceMutation
import com.neeva.app.DeleteSpaceResultByURLMutation
import com.neeva.app.Dispatchers
import com.neeva.app.GetSpacesDataQuery
import com.neeva.app.ListSpacesQuery
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.UnauthenticatedApolloWrapper
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.storage.BitmapIO
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.storage.entities.spaceItem
import com.neeva.app.storage.scaleDownMaintainingAspectRatio
import com.neeva.app.storage.toByteArray
import com.neeva.app.type.AddSpaceResultByURLInput
import com.neeva.app.type.DeleteSpaceResultByURLInput
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.userdata.NeevaUser
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Manages interactions with the user's Spaces. */
class SpaceStore(
    private val appContext: Context,
    historyDatabase: HistoryDatabase,
    private val coroutineScope: CoroutineScope,
    private val unauthenticatedApolloWrapper: UnauthenticatedApolloWrapper,
    private val authenticatedApolloWrapper: AuthenticatedApolloWrapper,
    private val neevaUser: NeevaUser,
    private val neevaConstants: NeevaConstants,
    private val snackbarModel: SnackbarModel,
    private val dispatchers: Dispatchers
) {
    companion object {
        private val TAG = SpaceStore::class.simpleName
        private const val DIRECTORY = "spaces"
        private const val MAX_THUMBNAIL_SIZE = 300
        private const val MAKER_COMMUNITY_SPACE_ID = "xlvaUJmdPRSrcqRHPEzVPuWf4RP74EyHvz5QvxLN"
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

    /** This ID determines what to show when we are at [AppNavDestination.SPACE_DETAIL] */
    val detailedSpaceIDFlow = MutableStateFlow<String?>(null)

    val spacesFromCommunityFlow: MutableStateFlow<List<SpaceRowData>> =
        MutableStateFlow(emptyList())

    @VisibleForTesting
    val thumbnailDirectory = File(appContext.cacheDir, DIRECTORY)

    private var isRefreshPending: Boolean = false
    private var cleanUpThumbnails: Boolean = true
    private var lastFetchedSpaceIds = emptyList<String>()

    suspend fun spaceStoreContainsUrl(url: Uri): Boolean = spaceIDsContainingURL(url).isNotEmpty()

    suspend fun deleteAllData() {
        dao.deleteAllSpaces()
        dao.deleteAllSpaceItems()
        cleanupSpacesThumbnails()
        lastFetchedSpaceIds = emptyList()
    }

    suspend fun refresh(space: Space? = null) {
        if (neevaUser.neevaUserToken.getToken().isEmpty()) {
            if (space == null) {
                fetchCommunitySpaces(appSpacesURL = neevaConstants.appSpacesURL)
            }
            return
        }
        // TODO(yusuf) : Early return here if there is no connectivity

        if (stateFlow.value == State.REFRESHING) {
            isRefreshPending = true
            return
        }

        stateFlow.value = State.REFRESHING
        val succeeded = if (space == null) {
            fetchCommunitySpaces(appSpacesURL = neevaConstants.appSpacesURL)
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

    private suspend fun fetchCommunitySpaces(appSpacesURL: String) = withContext(dispatchers.io) {
        val response =
            unauthenticatedApolloWrapper.performQuery(
                GetSpacesDataQuery(Optional.presentIfNotNull(listOf(MAKER_COMMUNITY_SPACE_ID))),
                userMustBeLoggedIn = false
            )

        val entities = response?.data?.getSpace?.space?.first()?.space?.entities
            ?.filter {
                it.spaceEntity?.url?.startsWith(appSpacesURL) == true &&
                    it.spaceEntity.title?.isNotEmpty() == true &&
                    Uri.parse(it.spaceEntity.url).pathSegments.size == 2
            }
            ?: return@withContext
        spacesFromCommunityFlow.emit(
            entities.map {
                val spaceData = SpaceRowData(
                    id = Uri.parse(it.spaceEntity?.url!!).pathSegments[1],
                    name = it.spaceEntity.title!!,
                    thumbnail = null,
                    isPublic = true,
                    appSpacesURL = appSpacesURL
                )
                spaceData.thumbnail = saveBitmap(
                    directory = thumbnailDirectory.resolve(MAKER_COMMUNITY_SPACE_ID),
                    dispatchers = dispatchers,
                    id = spaceData.id,
                    bitmapString = it.spaceEntity.thumbnail
                )?.toUri()
                return@map spaceData
            }
        )
    }

    private suspend fun performRefresh(): Boolean = withContext(dispatchers.io) {
        val response =
            authenticatedApolloWrapper.performQuery(ListSpacesQuery(), userMustBeLoggedIn = true)
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
        val spacesDataResponse = authenticatedApolloWrapper.performQuery(
            GetSpacesDataQuery(Optional.presentIfNotNull(spacesToFetch.map { it.id })),
            userMustBeLoggedIn = true
        ) ?: return false

        spacesDataResponse.data?.getSpace?.space?.forEach { spaceQuery ->
            val spaceID = spaceQuery.pageMetadata?.pageID ?: return@forEach
            val entityQueries = spaceQuery.space?.entities ?: return@forEach

            val entities =
                entityQueries.filter { it.metadata?.docID != null }.map { entityQuery ->
                    val thumbnailUri = entityQuery.spaceEntity?.thumbnail?.let {
                        saveBitmap(
                            directory = thumbnailDirectory.resolve(spaceID),
                            dispatchers = dispatchers,
                            id = entityQuery.metadata!!.docID!!,
                            bitmapString = it
                        )?.toUri()
                    }
                    entityQuery.spaceItem(spaceID, thumbnailUri)
                }
            entities.forEach { dao.upsert(it) }
            dao.getItemsFromSpace(spaceID)
                .filterNot { entities.contains(it) }
                .forEach { dao.deleteSpaceItem(it) }

            spacesToFetch
                .firstOrNull { space -> space.id == spaceID }
                ?.let { space ->
                    space.thumbnail = saveBitmap(
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

    /** Returns all the items within a given Space. */
    suspend fun contentDataForSpace(spaceID: String) =
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
            .list { file, _ ->
                file.isDirectory
            }?.filterNot { folderName ->
                folderName == MAKER_COMMUNITY_SPACE_ID || idList.contains(folderName)
            }?.forEach {
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
        val response = authenticatedApolloWrapper.performMutation(
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
            val errorString = appContext.getString(R.string.error_generic)
            snackbarModel.show(errorString)
            false
        }
    }

    suspend fun removeFromSpace(space: Space, uri: Uri): Boolean {
        val spaceID = space.id
        val response = authenticatedApolloWrapper.performMutation(
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
            val errorString = appContext.getString(R.string.error_generic)
            snackbarModel.show(errorString)
            false
        }
    }

    fun createSpace(spaceName: String) {
        coroutineScope.launch(dispatchers.io) {
            val response = authenticatedApolloWrapper.performMutation(
                CreateSpaceMutation(name = spaceName),
                userMustBeLoggedIn = true
            )

            response?.data?.createSpace?.let {
                snackbarModel.show(appContext.getString(R.string.space_create_success, spaceName))
                performRefresh()
            } ?: run {
                val errorString = appContext.getString(R.string.error_generic)
                snackbarModel.show(errorString)
            }
        }
    }

    private suspend fun saveBitmap(
        directory: File,
        dispatchers: Dispatchers,
        id: String,
        bitmapString: String?
    ) = withContext(dispatchers.io) {
        // Don't bother writing the file out if it already exists.
        val file = File(directory, id)
        try {
            if (file.exists()) return@withContext file
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to check if bitmap exists: ${file.absolutePath}", e)
            return@withContext null
        }

        val bitmap = bitmapString?.toBitmap() ?: return@withContext null
        val scaledBitmap = bitmap.scaleDownMaintainingAspectRatio(MAX_THUMBNAIL_SIZE)
        val bitmapBytes = scaledBitmap.toByteArray()
        return@withContext BitmapIO.saveBitmap(directory, file, ::FileOutputStream) {
            it.write(bitmapBytes)
        }
    }

    private fun String.toBitmap(): Bitmap? {
        val encoded = this
            .takeIf { it.startsWith(BitmapIO.DATA_URI_PREFIX) }
            ?.drop(BitmapIO.DATA_URI_PREFIX.length)
            ?: return null

        return try {
            val byteArray = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
