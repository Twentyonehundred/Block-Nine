package com.chrissmith.blocknine.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {

    private fun shape(id: String) = Pieces.ALL.first { it.id == id }
    private val dot = shape("dot")

    @Test
    fun `empty board has no filled cells`() {
        val board = Board.empty()
        for (r in 0 until Board.SIZE) {
            for (c in 0 until Board.SIZE) assertFalse(board.isFilled(r, c))
        }
    }

    @Test
    fun `completing a row clears exactly that row`() {
        val board = Board.of(
            "########.",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
        )

        val result = board.place(dot, 0, 8)

        assertEquals(1, result.clearedUnits)
        assertEquals(9, result.clearedCells.size)
        for (c in 0 until Board.SIZE) assertFalse(result.afterClear.isFilled(0, c))
    }

    @Test
    fun `completing a column clears exactly that column`() {
        val board = Board.of(
            "#........",
            "#........",
            "#........",
            "#........",
            "#........",
            "#........",
            "#........",
            "#........",
            ".........",
        )

        val result = board.place(dot, 8, 0)

        assertEquals(1, result.clearedUnits)
        for (r in 0 until Board.SIZE) assertFalse(result.afterClear.isFilled(r, 0))
    }

    @Test
    fun `completing a 3x3 box clears the box`() {
        val board = Board.of(
            "###......",
            "###......",
            "##.......",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
        )

        val result = board.place(dot, 2, 2)

        assertEquals(1, result.clearedUnits)
        assertEquals(9, result.clearedCells.size)
        Board.boxCells(0).forEach { (r, c) -> assertFalse(result.afterClear.isFilled(r, c)) }
    }

    @Test
    fun `a row and a box completed by the same move both clear`() {
        val board = Board.of(
            "###......",
            "###......",
            "##.######",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
        )

        val result = board.place(dot, 2, 2)

        assertEquals(2, result.clearedUnits)
        // Row 2 (9 cells) plus box 0 (9 cells), overlapping in the three cells of row 2.
        assertEquals(15, result.clearedCells.size)
        for (c in 0 until Board.SIZE) assertFalse(result.afterClear.isFilled(2, c))
        Board.boxCells(0).forEach { (r, c) -> assertFalse(result.afterClear.isFilled(r, c)) }
    }

    @Test
    fun `cells outside a cleared line survive`() {
        val board = Board.of(
            "########.",
            "#........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
        )

        val result = board.place(dot, 0, 8)

        assertTrue("the cell below the cleared row should remain", result.afterClear.isFilled(1, 0))
    }

    @Test
    fun `placement is rejected when it would overlap or leave the board`() {
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

        assertFalse("overlaps an occupied cell", board.canPlace(dot, 0, 0))
        assertFalse("off the right edge", board.canPlace(shape("h5"), 0, 5))
        assertFalse("off the bottom edge", board.canPlace(shape("v5"), 5, 0))
        assertFalse("negative coordinates", board.canPlace(dot, -1, 0))
        assertTrue(board.canPlace(shape("h5"), 0, 4))
    }

    @Test
    fun `a full board has no placement for any piece`() {
        val full = Board.of(
            "#########",
            "#########",
            "#########",
            "#########",
            "#########",
            "#########",
            "#########",
            "#########",
            "#########",
        )

        assertTrue(Pieces.ALL.none { full.hasAnyPlacement(it) })
    }

    @Test
    fun `a 2x3 piece needs two clear rows`() {
        // Striped: no two adjacent rows are ever both open, so nothing two cells tall fits,
        // however much horizontal room is left.
        val board = Board.of(
            "#########",
            ".........",
            "#########",
            ".........",
            "#########",
            ".........",
            "#########",
            ".........",
            "#########",
        )

        assertFalse(board.hasAnyPlacement(shape("rect23")))
        assertFalse(board.hasAnyPlacement(shape("rect32")))
        assertTrue(board.hasAnyPlacement(shape("h3")))
    }
}
