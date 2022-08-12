package com.neeva.app.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

fun Long.toZonedDateTime(): ZonedDateTime {
    return Date(this).toInstant().atZone(ZoneId.systemDefault())
}

fun Long.toLocalDate(): LocalDate {
    return Date(this).toLocalDate()
}

fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

fun LocalDateTime.toEpochMilli(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
