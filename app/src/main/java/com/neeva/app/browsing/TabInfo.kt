package com.neeva.app.browsing

import android.net.Uri

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
    val data: PersistedData = PersistedData(null, TabOpenType.DEFAULT)
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
        val openType: TabOpenType = TabOpenType.DEFAULT
    ) {
        companion object {
            const val KEY_PARENT_TAB_ID = "PARENT_TAB_ID"
            const val KEY_OPEN_TYPE = "OPEN_TYPE"
        }

        constructor(map: Map<String, String>) : this(
            parentTabId = map[KEY_PARENT_TAB_ID],
            openType = TabOpenType.values()
                .firstOrNull { it.name == map[KEY_OPEN_TYPE] }
                ?: TabOpenType.DEFAULT
        )

        fun toMap(): Map<String, String> {
            return mutableMapOf<String, String>().apply {
                put(KEY_OPEN_TYPE, openType.name)

                if (parentTabId != null) {
                    put(KEY_PARENT_TAB_ID, parentTabId)
                }
            }
        }
    }
}
