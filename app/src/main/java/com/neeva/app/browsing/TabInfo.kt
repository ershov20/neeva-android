package com.neeva.app.browsing

import android.net.Uri

/**
 * Records information about navigations that were triggered by a Search As You Type query.
 * TODO(dan.alcantara): Persist this data out to storage when we have a good way of tracking it.
 */
data class SearchNavigationInfo(
    /** Index of the navigation where the SAYT query was initiated. */
    val navigationEntryIndex: Int,

    /** URL of the navigation that resulted from tapping on a SAYT result. */
    val navigationEntryUri: Uri,

    /** Query that was performed. */
    val searchQuery: String
)

/** Information required to render a Tab in the UI. */
data class TabInfo(
    val id: String,
    val url: Uri?,
    val title: String?,
    val isSelected: Boolean,
    val isCrashed: Boolean = false,
    val isClosing: Boolean = false,
    val searchQueryMap: Map<Int, SearchNavigationInfo> = emptyMap(),
    val data: PersistedData = PersistedData(null, TabOpenType.DEFAULT)
) {
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
