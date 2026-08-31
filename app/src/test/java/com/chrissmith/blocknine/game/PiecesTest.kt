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

    /** Rotates a shape a quarter turn clockwise and renormalises it to the origin. */
    private fun rotated(piece: Piece): Set<Cell> {
        val turned = piece.cells.map { Cell(row = it.col, col = piece.height - 1 - it.row) }
        val top = turned.minOf { it.row }
        val left = turned.minOf { it.col }
        return turned.mapTo(HashSet()) { Cell(it.row - top, it.col - left) }
    }

    private fun cellsOf(id: String) = Pieces.ALL.first { it.id == id }.cells.toSet()

    /**
     * Asserts that [ids] name four distinct [size]-cell shapes forming a full rotation cycle:
     * turning each one clockwise lands exactly on the next, and the last comes back round.
     */
    private fun assertRotationCycle(size: Int, vararg ids: String) {
        val shapes = ids.map { cellsOf(it) }
        assertEquals("${ids.toList()} should be four different shapes", 4, shapes.toSet().size)
        for ((id, shape) in ids.zip(shapes)) assertEquals("$id cell count", size, shape.size)
        for (i in ids.indices) {
            val turned = rotated(Piece("turned", shapes[i].toList()))
            val next = (i + 1) % ids.size
            assertEquals("${ids[i]} turned clockwise should be ${ids[next]}", shapes[next], turned)
        }
    }

    @Test
    fun `the long T is dealable in all four orientations`() {
        assertRotationCycle(5, "t5-up", "t5-left", "t5-down", "t5-right")
    }

    @Test
    fun `the U is dealable in all four orientations`() {
        assertRotationCycle(5, "u-up", "u-right", "u-down", "u-left")
    }

    @Test
    fun `diagonals run both ways at both lengths`() {
        // A diagonal only has two orientations — turning a backslash a quarter turn gives a
        // forward slash — so this can't use assertRotationCycle.
        for (length in listOf(2, 3)) {
            val down = cellsOf("diag$length-down")
            val up = cellsOf("diag$length-up")
            assertEquals("diag$length-down cell count", length, down.size)
            assertEquals("diag$length-up cell count", length, up.size)
            assertEquals(
                "diag$length-down should run top-left to bottom-right",
                (0 until length).map { Cell(it, it) }.toSet(),
                down,
            )
            assertEquals(
                "diag$length-up should be its mirror",
                (0 until length).map { Cell(it, length - 1 - it) }.toSet(),
                up,
            )
        }
    }

    @Test
    fun `diagonal cells only ever touch at the corners`() {
        for (id in listOf("diag2-down", "diag2-up", "diag3-down", "diag3-up")) {
            val cells = cellsOf(id)
            val touching = cells.filter { cell ->
                Cell(cell.row + 1, cell.col) in cells || Cell(cell.row, cell.col + 1) in cells
            }
            assertTrue("$id has edge-to-edge neighbours at $touching", touching.isEmpty())
        }
    }

    @Test
    fun `colour slots use the whole palette and never repeat next door`() {
        val slots = Pieces.ALL.map { it.colorSlot }
        assertEquals("every slot should be used", Pieces.COLOUR_SLOTS, slots.toSet().size)
        assertTrue("slots must be in range, got ${slots.toSet()}", slots.all { it in 0 until Pieces.COLOUR_SLOTS })
        // Rotations of a shape are listed together, so neighbours matching would put a whole
        // family in one colour.
        assertTrue(
            "adjacent shapes share a colour",
            slots.zipWithNext().none { it.first == it.second },
        )
    }

    @Test
    fun `a placed piece keeps its own colour on the board`() {
        val piece = Pieces.ALL.first { it.colorSlot == 3 }
        val board = Board.empty().place(piece, 0, 0).afterClear
        val cell = piece.cells.first()
        assertEquals(3, board.colorSlotAt(cell.row, cell.col))
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
