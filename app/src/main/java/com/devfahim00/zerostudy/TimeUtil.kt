package com.devfahim00.zerostudy

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val zone: ZoneId get() = ZoneId.systemDefault()

/** Local date of an epoch-millis timestamp. */
fun dkey(t: Long): LocalDate = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()

/** Start of the local day containing t, in epoch millis. */
fun sod(t: Long): Long = dkey(t).atStartOfDay(zone).toInstant().toEpochMilli()

/** Start of the local Monday-start week containing t, in epoch millis. */
fun wk(t: Long): Long =
    dkey(t).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant().toEpochMilli()

/** Whole days from b to a (local calendar days). */
fun dayDiff(a: Long, b: Long): Long = ChronoUnit.DAYS.between(dkey(b), dkey(a))

/** Add n calendar days to the local day of t, result at midnight. */
fun addDays(t: Long, n: Long): Long =
    dkey(t).plusDays(n).atStartOfDay(zone).toInstant().toEpochMilli()

fun p2(n: Long): String = n.toString().padStart(2, '0')

/** Compact duration, e.g. 1h 30m / 45m / 20s (seconds are rounded). */
fun hm(sec: Long): String {
    val s = Math.round(sec.toDouble())
    val h = s / 3600
    val m = (s % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m" else "${s}s"
}

/** "MMM d" short date. */
fun fmtDate(t: Long): String =
    dkey(t).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

/** Short weekday name, e.g. Mon. */
fun fmtWeekday(t: Long): String =
    dkey(t).format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))

/** "HH:mm" clock time. */
fun fmtClock(t: Long): String {
    val zdt = Instant.ofEpochMilli(t).atZone(zone)
    return p2(zdt.hour.toLong()) + ":" + p2(zdt.minute.toLong())
}

/** Formats a double without a trailing .0, e.g. 3.0 -> "3", 3.5 -> "3.5". */
fun fmtNum(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
