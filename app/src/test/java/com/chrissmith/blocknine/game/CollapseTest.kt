package com.chrissmith.blocknine.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapseTest {

    @Test
    fun `a settled board doesn't move`() {
        // The cascade stops on this. Collapsing a board that has nothing to fall must report
        // nothing moved, or the chain would run forever on a still board.
        val board = Board.of(
            ".........", ".........", ".........", ".........", ".........",
            ".........", ".........", "##.......", "###......",
        )

        val result = board.collapse()

        assertFalse(result.moved)
        assertEquals(board, result.board)
    }

    @Test
    fun `an empty board has nothing to drop`() {
        val result = Board.empty().collapse()

        assertFalse(result.moved)
    }

    @Test
    fun `a floating tile falls to the floor`() {
        val board = Board.of(
            "#........", ".........", ".........", ".........", ".........",
            ".........", ".........", ".........", ".........",
        )

        val result = board.collapse()

        assertTrue(result.moved)
        assertTrue(result.board.isFilled(8, 0))
        assertFalse(result.board.isFilled(0, 0))
    }

    @Test
    fun `a tile lands on top of whatever is already there`() {
        val board = Board.of(
            "#........", ".........", ".........", ".........", ".........",
            ".........", ".........", ".........", "#........",
        )

        val result = board.collapse()

        assertTrue("the resting tile should have stayed", result.board.isFilled(8, 0))
        assertTrue("the falling tile should have stacked on it", result.board.isFilled(7, 0))
        assertFalse(result.board.isFilled(6, 0))
    }

    @Test
    fun `columns fall independently`() {
        // No sideways sliding: a tile drops down its own column and nowhere else, even when
        // the column beside it has room to spare.
        val board = Board.of(
            ".........", ".........", ".........", ".........", ".........",
            ".........", "#.#......", ".........", "..#......",
        )

        val result = board.collapse()

        assertTrue("column 0's tile reached the floor", result.board.isFilled(8, 0))
        assertTrue("column 2 was already packed at the bottom", result.board.isFilled(8, 2))
        assertTrue(result.board.isFilled(7, 2))
        assertFalse("nothing should have moved between columns", result.board.isFilled(7, 0))
    }

    @Test
    fun `a stack keeps its order as it falls`() {
        // Tiles can't overtake each other, so the colours arrive in the order they set off in.
        val grid = Board.decode(
            "0........" + "........." + "1........" + "........." + "2........" +
                "........." + "........." + "........." + "........."
        )!!

        val result = grid.collapse()

        assertEquals(0, result.board.colorSlotAt(6, 0))
        assertEquals(1, result.board.colorSlotAt(7, 0))
        assertEquals(2, result.board.colorSlotAt(8, 0))
    }

    @Test
    fun `the shift says how far each tile fell`() {
        // Negative by convention — the row it came from less the row it landed in — so the
        // canvas can animate a fall and a tide surge with the same arithmetic.
        val board = Board.of(
            "#........", ".........", ".........", ".........", ".........",
            ".........", ".........", ".........", "#........",
        )

        val result = board.collapse()

        assertEquals("the tile on the floor never moved", 0, result.shift[8 * Board.SIZE])
        assertEquals("the tile from row 0 fell seven rows", -7, result.shift[7 * Board.SIZE])
    }

    @Test
    fun `clearing a row drops what was standing above it`() {
        // The rule in one move. A 3x1 sits two rows above a bottom row that is one cell short;
        // filling that cell takes the row away, and the run that was floating comes down.
        val board = Board.of(
            ".........", ".........", ".........", ".........", ".........",
            ".........", "###......", ".........", "########.",
        )

        val settled = board.place(dot, 8, 8).afterClear
        assertFalse("the bottom row should have gone", settled.isFilled(8, 0))

        val result = settled.collapse()

        assertTrue("the run should have dropped to the floor", result.board.isFilled(8, 0))
        assertTrue(result.board.isFilled(8, 1))
        assertTrue(result.board.isFilled(8, 2))
        assertEquals("it fell two rows to get there", -2, result.shift[8 * Board.SIZE])
    }

    @Test
    fun `a fall can complete a line the placement never touched`() {
        // The cascade, end to end. Eight tiles float three rows up and a ninth column has two
        // stacked beside them; filling the bottom row clears it, everything lands in the row
        // that opened up, and that row is full — a second clear the player didn't place for.
        val board = Board.of(
            ".........", ".........", ".........", ".........", ".........",
            "########.", "........#", "........#", "########.",
        )

        val cleared = board.place(dot, 8, 8).afterClear
        assertEquals("only the bottom row goes on the placement", 0, cleared.settle().clearedUnits)

        val landed = cleared.collapse().board

        for (col in 0 until Board.SIZE) {
            assertTrue("column $col should have landed on the floor", landed.isFilled(8, col))
        }
        assertEquals("the landing completes the row", 1, landed.settle().clearedUnits)
    }

    private val dot = requireNotNull(Pieces.byId("dot"))
}
