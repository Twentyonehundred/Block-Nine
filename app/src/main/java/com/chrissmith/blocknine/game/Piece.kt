package com.chrissmith.blocknine.game

import androidx.compose.runtime.Immutable

/** A single cell offset from a piece's top-left bounding-box corner. */
@Immutable
data class Cell(val row: Int, val col: Int)

/**
 * A placeable block shape. [cells] are normalised so that the minimum row and column are
 * both zero, which makes [width] and [height] the piece's bounding box.
 *
 * [colorSlot] is which of the multicolour option's fills this shape is painted in. It lives
 * on the piece rather than being worked out when drawing so that a shape keeps the same
 * colour in the tray, under the finger, and once it's part of the board.
 */
@Immutable
data class Piece(val id: String, val cells: List<Cell>, val colorSlot: Int = 0) {
    val height: Int = cells.maxOf { it.row } + 1
    val width: Int = cells.maxOf { it.col } + 1
    val size: Int get() = cells.size
}
