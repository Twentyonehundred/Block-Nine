package com.chrissmith.blocknine.game

import androidx.compose.runtime.Immutable

/** A single cell offset from a piece's top-left bounding-box corner. */
@Immutable
data class Cell(val row: Int, val col: Int)

/**
 * A placeable block shape. [cells] are normalised so that the minimum row and column are
 * both zero, which makes [width] and [height] the piece's bounding box.
 */
@Immutable
data class Piece(val id: String, val cells: List<Cell>) {
    val height: Int = cells.maxOf { it.row } + 1
    val width: Int = cells.maxOf { it.col } + 1
    val size: Int get() = cells.size
}
