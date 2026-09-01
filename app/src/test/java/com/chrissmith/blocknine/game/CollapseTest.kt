package com.chrissmith.blocknine.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Collapse's landslide. Every board here is written as it is *after* the clear, since that is
 * what [Board.collapseInto] is handed — the hole is described by the cells passed to it, not by
 * what the board still has in it.
 */
class CollapseTest {

    private val dot = requireNotNull(Pieces.byId("dot"))

    /** The flat indices of a whole row, the commonest hole to close. */
    private fun row(r: Int) = (0 until Board.SIZE).map { r * Board.SIZE + it }.toSet()

    /** The flat indices of a whole column. */
    private fun col(c: Int) = (0 until Board.SIZE).map { it * Board.SIZE + c }.toSet()

    /** The flat indices of 3x3 box [index]. */
    private fun box(index: Int) =
        Board.boxCells(index).mapTo(HashSet()) { (r, c) -> r * Board.SIZE + c }

    @Test
    fun `nothing cleared means nothing falls`() {
        // The cascade stops on this. A link that closes no hole has to report no movement, or a
        // chain would run forever on a board that has finished moving.
        val board = Board.of(
            "#........", ".........", ".........", ".........", ".........",
            ".........", ".........", ".........", "..#......",
        )

        val result = board.collapseInto(emptySet())

        assertFalse(result.moved)
        assertEquals(board, result.board)
    }

    @Test
    fun `a cleared row brings everything above it down one`() {
        val board = Board.of(
            "#.......#", ".........", "..#......", ".........", ".........",
            ".........", ".........", ".........", ".........",
        )

        val result = board.collapseInto(row(8))

        assertTrue(result.moved)
        assertTrue(result.board.isFilled(1, 0))
        assertTrue(result.board.isFilled(1, 8))
        assertTrue(result.board.isFilled(3, 2))
        assertFalse("the tiles left the rows they were in", result.board.isFilled(0, 0))
    }

    @Test
    fun `a cleared box only moves the columns it stood in`() {
        // The local rule doing what whole-board compaction couldn't: three columns come down
        // three rows, and the tile alongside them doesn't so much as twitch.
        val board = Board.of(
            "####.....", ".........", ".........", ".........", ".........",
            ".........", ".........", ".........", ".........",
        )

        val result = board.collapseInto(box(3))

        for (c in 0 until 3) {
            assertTrue("column $c should have fallen three", result.board.isFilled(3, c))
            assertFalse(result.board.isFilled(0, c))
        }
        assertTrue("the tile outside the box shouldn't have moved", result.board.isFilled(0, 3))
    }

    @Test
    fun `a cleared column moves nothing`() {
        // Everything standing over that hole went into the clear with it, so there is nobody
        // left to fall.
        val board = Board.of(
            "..#......", ".........", ".........", ".........", ".........",
            ".........", ".........", ".........", "..#......",
        )

        val result = board.collapseInto(col(0))

        assertFalse(result.moved)
        assertTrue("the neighbouring column is untouched", result.board.isFilled(0, 2))
        assertTrue(result.board.isFilled(8, 2))
    }

    @Test
    fun `a gap the clear didn't make is not fallen into`() {
        // Free placement is the whole point of the mode. A tile left floating stays floating;
        // it only ever comes down by as much as the clear actually took out from under it.
        val board = Board.of(
            "#........", ".........", ".........", ".........", ".........",
            "#........", ".........", ".........", ".........",
        )

        val result = board.collapseInto(row(8))

        assertTrue("the high tile came down exactly one", result.board.isFilled(1, 0))
        assertTrue("so did the low one", result.board.isFilled(6, 0))
        assertFalse("neither went hunting for the floor", result.board.isFilled(8, 0))
        for (r in 2 until 6) assertFalse("the gap between them survives", result.board.isFilled(r, 0))
    }

    @Test
    fun `a stack keeps its order as it falls`() {
        // Tiles can't overtake one another, so the colours arrive in the order they set off in.
        val board = Board.decode(
            "0........" + "1........" + "2........" + "........." + "........." +
                "........." + "........." + "........." + "........."
        )!!

        val result = board.collapseInto(row(8))

        assertEquals(0, result.board.colorSlotAt(1, 0))
        assertEquals(1, result.board.colorSlotAt(2, 0))
        assertEquals(2, result.board.colorSlotAt(3, 0))
    }

    @Test
    fun `two holes under the same tile drop it twice`() {
        val board = Board.of(
            "#........", ".........", ".........", ".........", ".........",
            ".........", ".........", ".........", ".........",
        )

        val result = board.collapseInto(row(7) + row(8))

        assertTrue(result.board.isFilled(2, 0))
        assertEquals("two cleared rows beneath it, so two rows down", -2, result.shift[2 * Board.SIZE])
    }

    @Test
    fun `the shift is negative and says how far each tile fell`() {
        // Negative by convention — the row it came from less the row it landed in — so the
        // canvas animates a landslide and a tide surge with the same arithmetic.
        val board = Board.of(
            "#........", ".........", ".........", ".........", ".........",
            ".........", ".........", "#........", ".........",
        )

        val result = board.collapseInto(row(8))

        assertEquals("the high tile fell one", -1, result.shift[1 * Board.SIZE])
        assertEquals("so did the low one", -1, result.shift[8 * Board.SIZE])
        assertEquals("and the cells it left behind say nothing", 0, result.shift[0])
    }

    @Test
    fun `clearing a row drops what was standing above it`() {
        // The rule as it's actually reached in play: a 3x1 floats two rows above a bottom row
        // one cell short, and filling that cell takes the floor out from under it.
        val board = Board.of(
            ".........", ".........", ".........", ".........", ".........",
            ".........", "###......", ".........", "########.",
        )

        val placement = board.place(dot, 8, 8)
        assertEquals("the bottom row should have gone", 1, placement.clearedUnits)

        val result = placement.afterClear.collapseInto(placement.clearedCells)

        for (c in 0 until 3) assertTrue(result.board.isFilled(7, c))
        assertEquals("one cleared row below it, so one row down", -1, result.shift[7 * Board.SIZE])
    }

    @Test
    fun `a fall can complete a line the placement never touched`() {
        // The cascade, end to end, and the reason a box clear is the interesting one: only its
        // three columns move, so the fall changes which tiles line up. Here the bottom-right
        // box is one cell short. Filling it drops columns 6 to 8 by three, which lands three
        // tiles alongside a six-wide row that has been sitting there incomplete all game.
        val board = Board.of(
            "......###", ".........", ".........", "######...", ".........",
            ".........", "......###", "......###", "......##.",
        )

        val placement = board.place(dot, 8, 8)
        assertEquals("only the box goes on the placement", 1, placement.clearedUnits)
        assertFalse("row 3 is still short", placement.afterClear.isFilled(3, 8))

        val fall = placement.afterClear.collapseInto(placement.clearedCells)

        for (c in 0 until Board.SIZE) {
            assertTrue("column $c should be on row 3 now", fall.board.isFilled(3, c))
        }
        assertTrue("the untouched half didn't move", fall.shift[3 * Board.SIZE] == 0)
        assertEquals("the fallen half came down three", -3, fall.shift[3 * Board.SIZE + 6])
        assertEquals("and the landing completes the row", 1, fall.board.settle().clearedUnits)
    }
}
