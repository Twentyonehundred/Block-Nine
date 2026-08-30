package com.chrissmith.blocknine.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PiecesTest {

    @Test
    fun `every catalogue shape fits on an empty board`() {
        val empty = Board.empty()
        val tooBig = Pieces.ALL.filterNot { empty.hasAnyPlacement(it) }
        assertTrue("these shapes can never be placed: ${tooBig.map { it.id }}", tooBig.isEmpty())
    }

    @Test
    fun `shape ids are unique`() {
        assertEquals(Pieces.ALL.size, Pieces.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun `bounding boxes match the declared cells`() {
        for (piece in Pieces.ALL) {
            assertEquals("${piece.id} width", piece.cells.maxOf { it.col } + 1, piece.width)
            assertEquals("${piece.id} height", piece.cells.maxOf { it.row } + 1, piece.height)
            assertEquals("${piece.id} starts at column 0", 0, piece.cells.minOf { it.col })
            assertEquals("${piece.id} starts at row 0", 0, piece.cells.minOf { it.row })
        }
    }

    @Test
    fun `a tray always has three pieces`() {
        repeat(50) { seed ->
            val tray = Pieces.dealTray(Board.empty(), Random(seed))
            assertEquals(Pieces.TRAY_SIZE, tray.size)
        }
    }

    @Test
    fun `a tray dealt onto a playable board contains at least one placeable piece`() {
        // A board with plenty of room: only the top-left box is filled.
        val board = Board.of(
            "###......",
            "###......",
            "###......",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
        )

        repeat(100) { seed ->
            val tray = Pieces.dealTray(board, Random(seed))
            assertTrue(
                "no placeable piece in ${tray.map { it.id }}",
                tray.any { board.hasAnyPlacement(it) },
            )
        }
    }
}
