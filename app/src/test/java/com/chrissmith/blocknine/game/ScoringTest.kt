package com.chrissmith.blocknine.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

    private companion object {
        /** Multipliers are single-decimal by design, so anything tighter is float noise. */
        const val TOLERANCE = 0.0001f
    }

    @Test
    fun `a placement that clears nothing scores one point per cell`() {
        assertEquals(4, Scoring.points(cellsPlaced = 4, clearedUnits = 0, streak = 0))
    }

    @Test
    fun `a single clear adds the base bonus`() {
        assertEquals(1 + 18, Scoring.points(cellsPlaced = 1, clearedUnits = 1, streak = 1))
    }

    @Test
    fun `two clears at once beat two separate single clears`() {
        val together = Scoring.points(cellsPlaced = 1, clearedUnits = 2, streak = 1)
        val apart = 2 * Scoring.points(cellsPlaced = 1, clearedUnits = 1, streak = 1)
        assertTrue("$together should beat $apart", together > apart)
    }

    @Test
    fun `each extra unit in one move is worth more than the last`() {
        // A row plus a column plus a box in a single drop should feel like an event.
        val steps = (1..4).map { Scoring.points(cellsPlaced = 0, clearedUnits = it, streak = 1) }
        val gaps = steps.zipWithNext { a, b -> b - a }
        assertTrue("bonus should accelerate, got $steps", gaps.zipWithNext().all { it.second > it.first })
    }

    @Test
    fun `the combo only pays out while clearing`() {
        assertEquals(3, Scoring.points(cellsPlaced = 3, clearedUnits = 0, streak = 5))
        assertEquals(1f, Scoring.comboMultiplier(1), TOLERANCE)
        assertEquals(1f, Scoring.comboMultiplier(0), TOLERANCE)
    }

    @Test
    fun `the combo multiplier climbs with the run and then stops`() {
        val run = (1..12).map { Scoring.comboMultiplier(it) }
        assertTrue("should be non-decreasing, got $run", run.zipWithNext().all { it.second >= it.first })
        assertEquals(1.6f, Scoring.comboMultiplier(3), TOLERANCE)
        assertEquals(3f, Scoring.comboMultiplier(8), TOLERANCE)
        assertEquals("a long run must not run away", 3f, Scoring.comboMultiplier(50), TOLERANCE)
    }

    @Test
    fun `a clear on a run scores more than the same clear cold`() {
        val cold = Scoring.points(cellsPlaced = 1, clearedUnits = 1, streak = 1)
        val hot = Scoring.points(cellsPlaced = 1, clearedUnits = 1, streak = 4)
        assertTrue("$hot should beat $cold", hot > cold)
        // 18 * 1.9, rounded, plus the one cell placed.
        assertEquals(1 + 34, hot)
    }

    @Test
    fun `the label names what earned the bonus`() {
        assertNull("an ordinary placement says nothing", Scoring.label(clearedUnits = 0, streak = 0))
        assertNull("a lone single clear is unremarkable", Scoring.label(clearedUnits = 1, streak = 1))
        assertEquals("COMBO ×3", Scoring.label(clearedUnits = 1, streak = 3))
        assertEquals("DOUBLE CLEAR", Scoring.label(clearedUnits = 2, streak = 1))
        assertEquals("TRIPLE CLEAR · COMBO ×2", Scoring.label(clearedUnits = 3, streak = 2))
        assertEquals("QUAD CLEAR", Scoring.label(clearedUnits = 4, streak = 1))
        assertEquals("MEGA CLEAR ×6", Scoring.label(clearedUnits = 6, streak = 1))
    }

    @Test
    fun `a broken run says nothing even at a high streak count`() {
        // streak is reset to zero by a turn that clears nothing, so this pairing can't happen
        // in play; the label should still refuse to claim a combo for a move that cleared none.
        assertNull(Scoring.label(clearedUnits = 0, streak = 7))
    }
}
