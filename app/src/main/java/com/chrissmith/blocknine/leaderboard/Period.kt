package com.chrissmith.blocknine.leaderboard

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** The three boards. */
enum class Period(val label: String) {
    DAY("Today"),
    MONTH("This month"),
    ALL("All time"),
}

/**
 * Bucket keys for the daily and monthly boards.
 *
 * Deliberately UTC rather than device-local: the key is stored on the player's document and
 * queried with an equality filter, so two players in different timezones must agree on what
 * "today" is or they end up on separate boards and neither sees the other.
 */
object Periods {

    private val DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
    private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC)

    fun dayKey(now: Instant = Instant.now()): String = DAY_FORMAT.format(now)

    fun monthKey(now: Instant = Instant.now()): String = MONTH_FORMAT.format(now)
}
