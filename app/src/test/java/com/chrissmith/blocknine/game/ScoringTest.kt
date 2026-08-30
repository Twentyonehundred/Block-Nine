package com.chrissmith.blocknine.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

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
    fun `streak only pays out while clearing`() {
        assertEquals(3, Scoring.points(cellsPlaced = 3, clearedUnits = 0, streak = 5))
        assertEquals(
            1 + 18 + 40,
            Scoring.points(cellsPlaced = 1, clearedUnits = 1, streak = 5),
        )
    }
}
