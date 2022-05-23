package com.neeva.app.cookiecutter

import com.neeva.app.Dispatchers
import com.neeva.app.cookiecutter.ui.popover.CookieCutterPopoverSwitch
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.entities.HostInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.weblayer.ContentFilterManager

/**
 * An interface for reading and writing which website hostnames to allow trackers on.
 */
interface TrackersAllowList {
    /**
     * Allow trackers on the given [host]. Internally creates a [Job].
     * [onSuccess] is only called if the change was successful.
     *
     * Returns false if the job didn't run because another job was already running.
     */
    fun addToAllowList(host: String, onSuccess: () -> Unit): Boolean

    /**
     * Disable trackers on the given [host]. Internally creates a [Job].
     * [onSuccess] is only called if the change was successful.
     *
     * Returns false if the job didn't run because another job was already running.
     */
    fun removeFromAllowList(host: String, onSuccess: () -> Unit): Boolean

    /**
     * Provides the setter to allowing or disallowing trackers on a given [host].
     * [onSuccess] is only called if the change was successful.
     *
     * Returns false if the job didn't run because another job was already running.
     */
    fun getAllowListSetter(
        host: String,
        onSuccess: () -> Unit
    ): (newValue: Boolean) -> Boolean

    /** Returns a flow describing if trackers are allowed on the given [host]. */
    fun getAllowsTrackersFlow(host: String): Flow<Boolean?>

    /** Disallows trackers on every host. */
    fun removeAllHostFromAllowList()
}

/**
 * Warning: Not thread safe.
 *
 * [CookieCutterPopoverSwitch] mitigates this issue by disabling after clicking so only 1 job gets
 * fired at a time.
 */
class TrackersAllowListImpl(
    private val hostInfoDao: HostInfoDao?,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val contentFilterManager: ContentFilterManager
) : TrackersAllowList {
    private var currentJob: Job? = null

    init {
        // initialize the ContentFilterManager with HostInfoDao values
        coroutineScope.launch {
            val allTrackingAllowedHosts = withContext(dispatchers.io) {
                hostInfoDao?.getAllTrackingAllowedHosts()
            }

            withContext(dispatchers.main) {
                allTrackingAllowedHosts?.forEach {
                    contentFilterManager.addHostExclusion(it.host)
                }
            }
        }
    }

    override fun addToAllowList(host: String, onSuccess: () -> Unit): Boolean {
        if (!isJobRunning()) {
            currentJob = coroutineScope.launch {
                withContext(dispatchers.io) {
                    hostInfoDao?.upsert(HostInfo(host = host, isTrackingAllowed = true))
                }
                withContext(dispatchers.main) {
                    contentFilterManager.addHostExclusion(host)
                    onSuccess()
                }
            }
            return true
        } else {
            return false
        }
    }

    override fun removeFromAllowList(host: String, onSuccess: () -> Unit): Boolean {
        if (!isJobRunning()) {
            currentJob = coroutineScope.launch {
                withContext(dispatchers.io) {
                    hostInfoDao?.deleteFromHostInfo(host)
                }
                withContext(dispatchers.main) {
                    contentFilterManager.removeHostExclusion(host)
                    onSuccess()
                }
            }
            return true
        } else {
            return false
        }
    }

    override fun getAllowListSetter(
        host: String,
        onSuccess: () -> Unit,
    ): (Boolean) -> Boolean {
        return { cookieCutterEnabled ->
            if (cookieCutterEnabled) {
                removeFromAllowList(host, onSuccess = onSuccess)
            } else {
                addToAllowList(host, onSuccess = onSuccess)
            }
        }
    }

    /**
     * Based on the [HostInfoDao] database, returns if the given [host] allows trackers.
     * For a [host] not in the database, defaults to not allowing trackers.
     */
    override fun getAllowsTrackersFlow(host: String): Flow<Boolean?> {
        val result = MutableStateFlow<Boolean?>(null)
        coroutineScope.launch(dispatchers.io) {
            // If host does not exist in HostInfoDao, assume tracking is not allowed by default
            result.value = hostInfoDao?.getHostInfoByName(host)?.isTrackingAllowed ?: false
        }
        return result
    }

    override fun removeAllHostFromAllowList() {
        currentJob = coroutineScope.launch {
            withContext(dispatchers.io) {
                hostInfoDao?.deleteTrackingAllowedHosts()
            }
            withContext(dispatchers.main) {
                contentFilterManager.clearAllHostExclusions()
            }
        }
    }

    private fun isJobRunning(): Boolean {
        return currentJob?.isActive ?: false
    }
}

class PreviewTrackersAllowList : TrackersAllowList {
    override fun addToAllowList(host: String, onSuccess: () -> Unit): Boolean { return true }
    override fun removeFromAllowList(host: String, onSuccess: () -> Unit): Boolean { return true }
    override fun getAllowListSetter(
        host: String,
        onSuccess: () -> Unit
    ): (newValue: Boolean) -> Boolean = { true }
    override fun getAllowsTrackersFlow(host: String): Flow<Boolean?> {
        return MutableStateFlow(false)
    }
    override fun removeAllHostFromAllowList() { }
}
