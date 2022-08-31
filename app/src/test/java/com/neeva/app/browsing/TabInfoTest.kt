package com.neeva.app.browsing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.ui.toEpochMilli
import com.neeva.app.ui.toZonedDateTime
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
class TabInfoTest : BaseTest() {
    @Test
    fun isArchived() {
        val now = TimeUnit.DAYS.toMillis(1000)
        val baseTabInfo = TabInfo(
            id = "unused",
            url = null,
            title = null,
            isSelected = false,
            data = TabInfo.PersistedData()
        )

        baseTabInfo.copy(
            data = TabInfo.PersistedData(lastActiveMs = now - TimeUnit.DAYS.toMillis(3))
        ).apply {
            expectThat(isArchivable(ArchiveAfterOption.AFTER_7_DAYS, now)).isFalse()
            expectThat(isArchivable(ArchiveAfterOption.AFTER_30_DAYS, now)).isFalse()
            expectThat(isArchivable(ArchiveAfterOption.NEVER, now)).isFalse()
        }

        baseTabInfo.copy(
            data = TabInfo.PersistedData(lastActiveMs = now - TimeUnit.DAYS.toMillis(14))
        ).apply {
            expectThat(isArchivable(ArchiveAfterOption.AFTER_7_DAYS, now)).isTrue()
            expectThat(isArchivable(ArchiveAfterOption.AFTER_30_DAYS, now)).isFalse()
            expectThat(isArchivable(ArchiveAfterOption.NEVER, now)).isFalse()
        }

        baseTabInfo.copy(
            data = TabInfo.PersistedData(lastActiveMs = now - TimeUnit.DAYS.toMillis(31))
        ).apply {
            expectThat(isArchivable(ArchiveAfterOption.AFTER_7_DAYS, now)).isTrue()
            expectThat(isArchivable(ArchiveAfterOption.AFTER_30_DAYS, now)).isTrue()
            expectThat(isArchivable(ArchiveAfterOption.NEVER, now)).isFalse()
        }

        // Selected tabs will never be archived.
        baseTabInfo.copy(
            isSelected = true,
            data = TabInfo.PersistedData(lastActiveMs = now - TimeUnit.DAYS.toMillis(31))
        ).apply {
            expectThat(isArchivable(ArchiveAfterOption.AFTER_7_DAYS, now)).isFalse()
            expectThat(isArchivable(ArchiveAfterOption.AFTER_30_DAYS, now)).isFalse()
            expectThat(isArchivable(ArchiveAfterOption.NEVER, now)).isFalse()
        }
    }
    @Test
    fun getAgeGroup() {
        val now = TimeUnit.DAYS.toMillis(1000)

        val todayNoon = LocalDateTime.of(now.toZonedDateTime().toLocalDate(), LocalTime.NOON)
        val ageGroupCalculator = AgeGroupCalculator(now)

        val baseTabInfo = TabInfo(
            id = "unused",
            url = null,
            title = null,
            isSelected = false,
            data = TabInfo.PersistedData()
        )

        baseTabInfo.copy(
            data = TabInfo.PersistedData(lastActiveMs = todayNoon.toEpochMilli())
        ).apply {
            expectThat(getAgeGroup(ageGroupCalculator)).isEqualTo(AgeGroup.TODAY)
        }

        baseTabInfo.copy(
            data = TabInfo.PersistedData(lastActiveMs = todayNoon.minusDays(1).toEpochMilli())
        ).apply {
            expectThat(getAgeGroup(ageGroupCalculator)).isEqualTo(AgeGroup.YESTERDAY)
        }

        baseTabInfo.copy(
            data = TabInfo.PersistedData(lastActiveMs = todayNoon.minusDays(3).toEpochMilli())
        ).apply {
            expectThat(getAgeGroup(ageGroupCalculator)).isEqualTo(AgeGroup.LAST_7_DAYS)
        }

        baseTabInfo.copy(
            data = TabInfo.PersistedData(lastActiveMs = todayNoon.minusDays(14).toEpochMilli())
        ).apply {
            expectThat(getAgeGroup(ageGroupCalculator)).isEqualTo(AgeGroup.LAST_30_DAYS)
        }

        baseTabInfo.copy(
            data = TabInfo.PersistedData(lastActiveMs = todayNoon.minusDays(31).toEpochMilli())
        ).apply {
            expectThat(getAgeGroup(ageGroupCalculator)).isEqualTo(AgeGroup.OLDER)
        }

        // Selected tabs will never be archived.
        baseTabInfo.copy(
            isSelected = true,
            data = TabInfo.PersistedData(lastActiveMs = todayNoon.minusDays(31).toEpochMilli())
        ).apply {
            expectThat(getAgeGroup(ageGroupCalculator)).isEqualTo(AgeGroup.TODAY)
        }
    }
}
