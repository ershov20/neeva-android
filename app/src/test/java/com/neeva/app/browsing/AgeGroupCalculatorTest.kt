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

@RunWith(AndroidJUnit4::class)
class AgeGroupCalculatorTest : BaseTest() {
    @Test
    fun getAgeGroup() {
        val now = TimeUnit.DAYS.toMillis(1000)
        val ageGroupCalculator = AgeGroupCalculator(now)

        val todayNoon = LocalDateTime.of(now.toZonedDateTime().toLocalDate(), LocalTime.NOON)
        expectThat(
            ageGroupCalculator.getAgeBucket(todayNoon.toEpochMilli())
        ).isEqualTo(AgeGroup.TODAY)

        val yesterdayNoon = todayNoon.minusDays(1)
        expectThat(
            ageGroupCalculator.getAgeBucket(yesterdayNoon.toEpochMilli())
        ).isEqualTo(AgeGroup.YESTERDAY)

        val threeDaysAgoNoon = todayNoon.minusDays(3)
        expectThat(
            ageGroupCalculator.getAgeBucket(threeDaysAgoNoon.toEpochMilli())
        ).isEqualTo(AgeGroup.LAST_7_DAYS)

        val tenDaysAgoNoon = todayNoon.minusDays(10)
        expectThat(
            ageGroupCalculator.getAgeBucket(tenDaysAgoNoon.toEpochMilli())
        ).isEqualTo(AgeGroup.LAST_30_DAYS)

        val overOneMonthAgoNoon = todayNoon.minusMonths(1).minusDays(1)
        expectThat(
            ageGroupCalculator.getAgeBucket(overOneMonthAgoNoon.toEpochMilli())
        ).isEqualTo(AgeGroup.OLDER)
    }
}
