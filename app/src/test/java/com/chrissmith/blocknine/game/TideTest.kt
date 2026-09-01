package com.chrissmith.blocknine.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TideTest {

    // Fixed seeds throughout: the wave placement is random by design, and a test that only
    // usually passes is worse than no test.
    private fun waves(surgeIndex: Int, count: Int = 200) =
        (0 until count).map { Tide.wave(surgeIndex, Random(it)) }

    @Test
    fun `a wave is one push per column`() {
        for (wave in waves(0)) assertEquals(Board.SIZE, wave.size)
    }

    @Test
    fun `every wave shoves some columns harder than others`() {
        // This is the whole mode. A wave with one distinct non-zero value across the whole
        // board would be a flat lift, which costs a row and changes nothing else.
        for (surge in 0 until 20) {
            for (wave in waves(surge, count = 60)) {
                assertTrue(
                    "surge $surge produced a flat wave ${wave.toList()}",
                    wave.distinct().size > 1,
                )
            }
        }
    }

    @Test
    fun `a wave always pushes something`() {
        for (surge in 0 until 20) {
            for (wave in waves(surge, count = 60)) {
                assertTrue("surge $surge did nothing at all", wave.any { it > 0 })
            }
        }
    }

    @Test
    fun `pushes never exceed the advertised maximum`() {
        for (surge in 0 until 40) {
            for (wave in waves(surge, count = 40)) {
                assertTrue(wave.all { it in 0..Tide.MAX_PUSH })
            }
        }
    }

    @Test
    fun `the wave reaches both edges of the board`() {
        // Building tight against the wall shouldn't be a way to sit out the tide.
        val seen = (0 until 400).map { Tide.wave(0, Random(it)) }
        assertTrue("column 0 was never pushed", seen.any { it[0] > 0 })
        assertTrue("the last column was never pushed", seen.any { it[Board.SIZE - 1] > 0 })
    }

    @Test
    fun `waves get harsher as the run goes on`() {
        // Compared by total water shifted rather than peak height, because the early profiles
        // widen before they steepen.
        fun worst(surge: Int) = (0 until 200).maxOf { Tide.wave(surge, Random(it)).sum() }
        assertTrue("the last profile is no worse than the first", worst(30) > worst(0))
    }

    @Test
    fun `a surge shears a straight run into a staircase`() {
        val board = Board.of(
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            "###......",
        )

        val result = board.surge(intArrayOf(1, 2, 1, 0, 0, 0, 0, 0, 0), Pieces.COLOUR_SLOTS)

        assertFalse(result.overflowed)
        // The row that was flat now sits at three different heights.
        assertTrue(result.board.isFilled(7, 0))
        assertTrue(result.board.isFilled(6, 1))
        assertTrue(result.board.isFilled(7, 2))
    }

    @Test
    fun `the water fills in behind whatever it pushed`() {
        val board = Board.empty()

        val result = board.surge(intArrayOf(2, 1, 0, 0, 0, 0, 0, 0, 0), Pieces.COLOUR_SLOTS)

        assertTrue(result.board.isFilled(8, 0))
        assertTrue(result.board.isFilled(7, 0))
        assertTrue(result.board.isFilled(8, 1))
        assertFalse("column 1 was only pushed one", result.board.isFilled(7, 1))
        assertFalse("an unpushed column takes no water", result.board.isFilled(8, 2))

        // Tide blocks sit in their own colour slot so the board can draw them apart from
        // anything the player put down.
        assertEquals(Pieces.COLOUR_SLOTS, result.board.colorSlotAt(8, 0))
    }

    @Test
    fun `pushing a block off the top is a drowning`() {
        val board = Board.of(
            "#........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
        )

        val result = board.surge(intArrayOf(1, 2, 1, 0, 0, 0, 0, 0, 0), Pieces.COLOUR_SLOTS)

        assertTrue(result.overflowed)
    }

    @Test
    fun `a column with room to spare survives the same push`() {
        val board = Board.of(
            ".........",
            "#........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
        )

        val result = board.surge(intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0), Pieces.COLOUR_SLOTS)

        assertFalse(result.overflowed)
        assertTrue(result.board.isFilled(0, 0))
    }

    @Test
    fun `a surge that finishes a row leaves it there for settle to clear`() {
        // The board only clears on demand — the view model decides whether tide-made lines pay
        // out, so surge itself must hand back the completed row intact.
        val board = Board.of(
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            "........#",
        )

        val result = board.surge(IntArray(Board.SIZE) { if (it == 8) 0 else 1 }, Pieces.COLOUR_SLOTS)

        for (col in 0 until Board.SIZE) assertTrue(result.board.isFilled(8, col))

        val settled = result.board.settle()
        assertEquals(1, settled.clearedUnits)
    }
}
