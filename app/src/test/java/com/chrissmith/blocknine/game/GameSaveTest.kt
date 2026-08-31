package com.chrissmith.blocknine.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the encoding either side of SharedPreferences. The prefs themselves are Android's
 * and can't run here, but they only ever see the strings these functions produce.
 */
class GameSaveTest {

    private val dot = Pieces.byId("dot")!!
    private val sq2 = Pieces.byId("sq2")!!

    @Test
    fun `a board survives the round trip with its colours`() {
        val board = Board.empty()
            .place(sq2, 0, 0).afterClear
            .place(dot, 5, 5).afterClear

        val restored = Board.decode(board.encode())
        assertNotNull(restored)
        for (row in 0 until Board.SIZE) {
            for (col in 0 until Board.SIZE) {
                assertEquals("filled at $row,$col", board.isFilled(row, col), restored!!.isFilled(row, col))
                assertEquals("colour at $row,$col", board.colorSlotAtOrZero(row, col), restored.colorSlotAtOrZero(row, col))
            }
        }
    }

    private fun Board.colorSlotAtOrZero(row: Int, col: Int) =
        if (isFilled(row, col)) colorSlotAt(row, col) else 0

    @Test
    fun `an empty board encodes to eighty-one dots`() {
        assertEquals(".".repeat(81), Board.empty().encode())
    }

    @Test
    fun `text that isn't a board is refused rather than half-read`() {
        assertNull("too short", Board.decode("..."))
        assertNull("too long", Board.decode(".".repeat(82)))
    }

    @Test
    fun `a save written before shapes had colours still loads`() {
        // Older builds wrote nothing at all, but a hand-edited or migrated file could contain
        // the '#' the test helpers use. It should read as a filled cell, not a crash.
        val text = "#".repeat(9) + ".".repeat(72)
        val board = Board.decode(text)
        assertNotNull(board)
        assertTrue(board!!.isFilled(0, 0))
        assertEquals(0, board.colorSlotAt(0, 0))
    }

    @Test
    fun `a tray round trips including spent slots`() {
        val tray = listOf(dot, null, sq2)
        val restored = GameSave.decodeTray(GameSave.encodeTray(tray))
        assertEquals(tray, restored)
    }

    @Test
    fun `a tray that can't be rebuilt is refused`() {
        assertNull("no save at all", GameSave.decodeTray(null))
        assertNull("wrong slot count", GameSave.decodeTray("dot,sq2"))
        assertNull("unknown shape", GameSave.decodeTray("dot,not-a-shape,sq2"))
        assertNull("nothing left to play", GameSave.decodeTray(",,"))
    }
}
