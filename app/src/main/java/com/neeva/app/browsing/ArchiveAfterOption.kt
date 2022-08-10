package com.neeva.app.browsing

import androidx.annotation.StringRes
import com.neeva.app.R
import com.neeva.app.sharedprefs.SharedPrefFolder

enum class ArchiveAfterOption(@StringRes val resourceId: Int) {
    AFTER_7_DAYS(R.string.archived_tabs_archive_after_seven_days),
    AFTER_30_DAYS(R.string.archived_tabs_archive_after_thirty_days),
    NEVER(R.string.archived_tabs_archive_never);

    companion object {
        fun fromString(value: String): ArchiveAfterOption {
            return ArchiveAfterOption
                .values().firstOrNull { value == it.name }
                ?: SharedPrefFolder.App.AutomaticallyArchiveTabs.defaultValue
        }
    }
}
