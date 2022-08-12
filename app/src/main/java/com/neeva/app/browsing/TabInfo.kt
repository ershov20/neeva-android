package com.neeva.app.browsing

import android.net.Uri
import java.util.concurrent.TimeUnit

/**
 * Normalizes URIs for fuzzy comparison.
 *
 * iOS ignores the scheme and fragment when matching.
 */
data class UriFuzzyMatchData(
    val authority: String?,
    val path: String?,
    val query: String?
) {
    companion object {
        fun create(uri: Uri): UriFuzzyMatchData? {
            // Ignore URIs that shouldn't be matched (e.g. file: or intent: URIs).
            val scheme = uri.normalizeScheme().scheme
            if (scheme != "http" && scheme != "https") return null

            // Normalize the authority so that it strips off any mobile-specific tags.
            var newAuthority = uri.authority
            newAuthority?.let {
                val starterRegex = "^(www|mobile|m)\\."
                newAuthority = Regex(starterRegex).replace(it, "")
            }
            newAuthority?.let {
                val mobileWikipediaRegex = "^(..)\\.m\\.wikipedia\\.org"
                newAuthority = Regex(mobileWikipediaRegex).replace(it, "$1.wikipedia.org")
            }

            // Remove trailing slash, if it exists.
            var newPath = uri.path
            newPath?.let {
                if (it.endsWith("/")) {
                    newPath = it.dropLast(1)
                }
            }

            return UriFuzzyMatchData(
                newAuthority,
                newPath,
                uri.query
            )
        }
    }
}

/** Information required to render a Tab in the UI. */
data class TabInfo(
    val id: String,
    val url: Uri?,
    val title: String?,
    val isSelected: Boolean,
    val isCrashed: Boolean = false,
    val isClosing: Boolean = false,
    val data: PersistedData = PersistedData()
) {
    /** Used to compare two URIs and sees if they're "close enough" to be a match. */
    val fuzzyMatchUrl: UriFuzzyMatchData? = url?.let { UriFuzzyMatchData.create(url) }

    enum class TabOpenType {
        DEFAULT,
        CHILD_TAB,
        VIA_INTENT
    }

    /** Used to save info about Tab across process restarts via WebLayer. */
    data class PersistedData(
        val parentTabId: String? = null,
        val parentSpaceId: String? = null,
        val lastActiveMs: Long = System.currentTimeMillis(),
        val openType: TabOpenType = TabOpenType.DEFAULT
    ) {
        companion object {
            const val KEY_PARENT_TAB_ID = "PARENT_TAB_ID"
            const val KEY_PARENT_SPACE_ID = "PARENT_SPACE_ID"
            const val KEY_LAST_ACTIVE_MS = "LAST_ACTIVE_MS"
            const val KEY_OPEN_TYPE = "OPEN_TYPE"
        }

        constructor(isSelected: Boolean, map: Map<String, String>) : this(
            parentTabId = map[KEY_PARENT_TAB_ID],
            parentSpaceId = map[KEY_PARENT_SPACE_ID],
            lastActiveMs = if (isSelected) {
                System.currentTimeMillis()
            } else {
                map[KEY_LAST_ACTIVE_MS]?.toLongOrNull() ?: System.currentTimeMillis()
            },
            openType = TabOpenType.values()
                .firstOrNull { it.name == map[KEY_OPEN_TYPE] }
                ?: TabOpenType.DEFAULT
        )

        fun toMap(): Map<String, String> {
            return mutableMapOf<String, String>().apply {
                put(KEY_OPEN_TYPE, openType.name)
                put(KEY_LAST_ACTIVE_MS, lastActiveMs.toString())

                if (parentSpaceId != null) {
                    put(KEY_PARENT_SPACE_ID, parentSpaceId)
                }

                if (parentTabId != null) {
                    put(KEY_PARENT_TAB_ID, parentTabId)
                }
            }
        }
    }

    /** Determines which [AgeGroup] applies to this tab, according to when it was last active. */
    fun getAgeGroup(ageGroupCalculator: AgeGroupCalculator): AgeGroup {
        return if (isSelected) {
            AgeGroup.TODAY
        } else {
            ageGroupCalculator.getAgeBucket(data.lastActiveMs)
        }
    }

    fun isArchived(archiveAfterOption: ArchiveAfterOption, now: Long): Boolean {
        if (isSelected) return false

        val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
        val foreverAgo = 0L

        val timeLimit = when (archiveAfterOption) {
            ArchiveAfterOption.AFTER_7_DAYS -> sevenDaysAgo
            ArchiveAfterOption.AFTER_30_DAYS -> thirtyDaysAgo
            ArchiveAfterOption.NEVER -> foreverAgo
        }

        return data.lastActiveMs < timeLimit
    }
}
