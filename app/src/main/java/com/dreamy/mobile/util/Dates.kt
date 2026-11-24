package com.dreamy.mobile.util

import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val longFrench = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
private val shortFrench = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH)

/** "24 novembre 2026", like `Intl.DateTimeFormat("fr-FR", { day, month: "long", year })`. */
fun LocalDate.formatFr(): String = format(longFrench)

fun Instant.formatFr(): String = atZone(ZoneId.systemDefault()).toLocalDate().formatFr()

fun Instant.formatShortFr(): String = atZone(ZoneId.systemDefault()).toLocalDate().format(shortFrench)

fun LocalDate.age(today: LocalDate = LocalDate.now()): Int = Period.between(this, today).years

/** Next occurrence of this birthday, today included. 29 February falls back to 28 February. */
fun LocalDate.nextBirthday(today: LocalDate = LocalDate.now()): LocalDate {
    val thisYear = withYear(today.year)
    return if (thisYear.isBefore(today)) withYear(today.year + 1) else thisYear
}
