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
    fun `the 3x3 block is not dealable`() {
        assertTrue(Pieces.ALL.none { it.cells.size == 9 })
    }

    @Test
    fun `the deal favours small pieces over the 2x3 slabs`() {
        // Fixed seed, so this measures the weighting rather than luck. An empty board never
        // forces a reroll, which keeps the sample a clean draw from the catalogue.
        val random = Random(20260830)
        val counts = mutableMapOf<String, Int>()
        repeat(5_000) {
            Pieces.dealTray(Board.empty(), random).forEach {
                counts[it.id] = (counts[it.id] ?: 0) + 1
            }
        }

        val small = listOf("dot", "h2", "v2").sumOf { counts[it] ?: 0 }
        val slabs = listOf("rect23", "rect32").sumOf { counts[it] ?: 0 }

        assertTrue("small pieces were $small of 15000, expected roughly a fifth", small > 2_000)
        assertTrue("2x3 slabs were $slabs of 15000, expected roughly 2%", slabs < 500)
        assertTrue("2x3 slabs should still appear", slabs > 0)
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
