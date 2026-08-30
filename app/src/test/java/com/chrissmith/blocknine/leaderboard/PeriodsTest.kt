package com.chrissmith.blocknine.leaderboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PeriodsTest {

    @Test
    fun `keys use the shape the security rules expect`() {
        val instant = Instant.parse("2026-08-30T17:45:00Z")
        assertEquals("2026-08-30", Periods.dayKey(instant))
        assertEquals("2026-08", Periods.monthKey(instant))
    }

    @Test
    fun `keys are UTC, so a late-evening UK game still lands on the same day`() {
        // 23:30 UTC and 00:30 UTC are deliberately different days: every client must agree
        // on the bucket or players end up on boards that can't see each other.
        assertEquals("2026-08-30", Periods.dayKey(Instant.parse("2026-08-30T23:30:00Z")))
        assertEquals("2026-08-31", Periods.dayKey(Instant.parse("2026-08-31T00:30:00Z")))
    }

    @Test
    fun `month rolls over independently of the day`() {
        assertEquals("2026-08", Periods.monthKey(Instant.parse("2026-08-31T23:59:59Z")))
        assertEquals("2026-09", Periods.monthKey(Instant.parse("2026-09-01T00:00:00Z")))
    }

    @Test
    fun `single-digit months and days are zero padded`() {
        val instant = Instant.parse("2027-01-05T09:00:00Z")
        assertEquals("2027-01-05", Periods.dayKey(instant))
        assertEquals("2027-01", Periods.monthKey(instant))
    }
}
