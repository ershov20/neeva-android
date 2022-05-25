package com.neeva.app.cookiecutter

import com.neeva.app.Dispatchers
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.entities.HostInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * An interface for reading and writing which hostnames to allow trackers on.
 */
abstract class TrackersAllowList {
    /** Lambda to invoke when allowing a host to use trackers. */
    protected var onAddHostExclusion: ((String) -> Unit)? = null

    /** Lambda to invoke when disallowing a host from using trackers. */
    protected var onRemoveHostExclusion: ((String) -> Unit)? = null

    fun setUpTrackingProtection(
        onAddHostExclusion: ((String) -> Unit)?,
        onRemoveHostExclusion: ((String) -> Unit)?
    ) {
        this.onAddHostExclusion = onAddHostExclusion
        this.onRemoveHostExclusion = onRemoveHostExclusion
    }

    /** Returns all of the hosts where tracking is explicitly allowed by the user. */
    abstract suspend fun getAllHostsInList(): Collection<HostInfo>

    /** Returns a flow describing if trackers are allowed on the given [host]. */
    abstract fun getHostAllowsTrackersFlow(host: String): Flow<Boolean>

    /**
     * Toggles whether or not the given [host] is allowed to use trackers.
     *
     * [onSuccess] is only called if the change was successful and no other operation was in flight.
     *
     * @return Whether or not the job was started successfully.
     */
    abstract fun toggleHostInAllowList(host: String, onSuccess: () -> Unit): Boolean
}

/*
 * Manages the allowlist of hosts where Tracking Protection has been disabled for the regular
 * browser profile.
 *
 * This class initially tries to persist the changes to the allowlist to the database before telling
 * WebLayer about the new exception.
 *
 * Only one operation is allowed to run at any given time to avoid putting the user in a bad state.
 */
class RegularTrackersAllowList(
    private val hostInfoDao: HostInfoDao,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers
) : TrackersAllowList() {
    private var currentJob: Job? = null

    override suspend fun getAllHostsInList() = hostInfoDao.getAllTrackingAllowedHosts()

    override fun getHostAllowsTrackersFlow(host: String): Flow<Boolean> {
        return hostInfoDao.getHostInfoByNameFlow(host)
            .map {
                // If the host is not currently in the database, we disallow tracking by default.
                it?.isTrackingAllowed ?: false
            }
            .distinctUntilChanged()
    }

    override fun toggleHostInAllowList(host: String, onSuccess: () -> Unit): Boolean {
        val isJobRunning = currentJob?.isActive ?: false
        if (isJobRunning) return false

        currentJob = coroutineScope.launch {
            val shouldRemoveHostExclusion = withContext(dispatchers.io) {
                hostInfoDao.toggleTrackingAllowedForHost(host)
            }

            withContext(dispatchers.main) {
                if (shouldRemoveHostExclusion) {
                    onRemoveHostExclusion?.invoke(host)
                } else {
                    onAddHostExclusion?.invoke(host)
                }

                onSuccess()
            }
        }

        return true
    }
}

/**
 * Maintains an in-memory collection of hosts that the user is allowing trackers to be run on
 * for the Incognito profile.  This collection is lost when the Incognito session ends, either
 * because the app dies or because the Incognito profile was destroyed.
 */
class IncognitoTrackersAllowList : TrackersAllowList() {
    private val allowedHosts = MutableStateFlow<Set<HostInfo>>(emptySet())

    override suspend fun getAllHostsInList() = allowedHosts.value

    override fun getHostAllowsTrackersFlow(host: String): Flow<Boolean> {
        return allowedHosts.map { currentSet ->
            currentSet.any { it.host == host }
        }
    }

    private fun addToAllowList(host: String, onSuccess: () -> Unit): Boolean {
        return onAddHostExclusion
            ?.let {
                allowedHosts.value = allowedHosts.value.plus(
                    HostInfo(host = host, isTrackingAllowed = true)
                )

                onAddHostExclusion?.invoke(host)
                onSuccess()
                true
            }
            ?: false
    }

    private fun removeFromAllowList(host: String, onSuccess: () -> Unit): Boolean {
        return onRemoveHostExclusion
            ?.let {
                val currentSet = allowedHosts.value.toMutableSet()
                currentSet.removeAll { it.host == host }
                allowedHosts.value = currentSet

                onRemoveHostExclusion?.invoke(host)
                onSuccess()
                true
            }
            ?: false
    }

    override fun toggleHostInAllowList(host: String, onSuccess: () -> Unit): Boolean {
        val isTrackingCurrentlyAllowed = allowedHosts.value.any { it.host == host }
        return if (isTrackingCurrentlyAllowed) {
            removeFromAllowList(host, onSuccess = onSuccess)
        } else {
            addToAllowList(host, onSuccess = onSuccess)
        }
    }
}

class PreviewTrackersAllowList : TrackersAllowList() {
    override suspend fun getAllHostsInList(): List<HostInfo> = emptyList()
    override fun getHostAllowsTrackersFlow(host: String) = MutableStateFlow(false)
    override fun toggleHostInAllowList(host: String, onSuccess: () -> Unit) = true
}
