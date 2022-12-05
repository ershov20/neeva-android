// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import androidx.annotation.StringRes
import com.neeva.app.R
import com.neeva.app.ui.toZonedDateTime
import java.time.LocalDateTime
import java.time.LocalTime

enum class AgeGroup(@StringRes val resourceId: Int) {
    PINNED(R.string.pinned_tab_header),
    TODAY(R.string.archived_tabs_today),
    YESTERDAY(R.string.archived_tabs_yesterday),
    LAST_7_DAYS(R.string.archived_tabs_last_seven_days),
    LAST_30_DAYS(R.string.archived_tabs_last_thirty_days),
    OLDER(R.string.archived_tabs_older)
}

data class AgeGroupCalculator(val now: Long) {
    private val todayMidnight: LocalDateTime = LocalDateTime.of(
        now.toZonedDateTime().toLocalDate(),
        LocalTime.MIDNIGHT
    )
    private val yesterdayMidnight: LocalDateTime = todayMidnight.minusDays(1)
    private val pastSevenDays: LocalDateTime = todayMidnight.minusDays(7)
    private val pastThirtyDays: LocalDateTime = todayMidnight.minusDays(30)

    fun getAgeBucket(timestamp: Long): AgeGroup {
        val lastActiveDateTime: LocalDateTime = timestamp.toZonedDateTime().toLocalDateTime()
        return when {
            lastActiveDateTime >= todayMidnight -> AgeGroup.TODAY
            lastActiveDateTime >= yesterdayMidnight -> AgeGroup.YESTERDAY
            lastActiveDateTime >= pastSevenDays -> AgeGroup.LAST_7_DAYS
            lastActiveDateTime >= pastThirtyDays -> AgeGroup.LAST_30_DAYS
            else -> AgeGroup.OLDER
        }
    }
}
