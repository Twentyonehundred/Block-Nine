package com.chrissmith.blocknine.game

import androidx.compose.runtime.Immutable

/**
 * The result of dropping a piece onto the board.
 *
 * [beforeClear] still contains the completed rows/columns/boxes so the UI can flash them
 * before they disappear; [afterClear] is the settled board the next move plays against.
 */
@Immutable
data class Placement(
    val beforeClear: Board,
    val afterClear: Board,
    /** Flat indices (row * SIZE + col) of every cell removed by this placement. */
    val clearedCells: Set<Int>,
    /** How many distinct rows, columns and boxes completed at once. Drives the bonus. */
    val clearedUnits: Int,
)

/**
 * The 9x9 playing field, subdivided into nine 3x3 boxes.
 *
 * Immutable by design: every mutation returns a fresh instance, so holding one in a
 * Compose [androidx.compose.runtime.MutableState] is enough to drive recomposition
 * without needing a snapshot-aware collection.
 */
@Immutable
class Board private constructor(private val grid: List<Int>) {

    operator fun get(row: Int, col: Int): Int = grid[row * SIZE + col]

    fun isFilled(row: Int, col: Int): Boolean = get(row, col) != EMPTY

    /** True if [piece]'s bounding box lands in-bounds at ([row], [col]) over only empty cells. */
    fun canPlace(piece: Piece, row: Int, col: Int): Boolean {
        if (row < 0 || col < 0) return false
        if (row + piece.height > SIZE || col + piece.width > SIZE) return false
        return piece.cells.none { isFilled(row + it.row, col + it.col) }
    }

    /** True if [piece] fits anywhere on the board. Game over is when this fails for every tray piece. */
    fun hasAnyPlacement(piece: Piece): Boolean {
        for (row in 0..SIZE - piece.height) {
            for (col in 0..SIZE - piece.width) {
                if (canPlace(piece, row, col)) return true
            }
        }
        return false
    }

    /**
     * Drops [piece] at ([row], [col]) and resolves any completed rows, columns and boxes.
     *
     * All completions are detected against the same post-placement board and cleared
     * together, so a move that finishes a row and a box at once scores both.
     */
    fun place(piece: Piece, row: Int, col: Int): Placement {
        require(canPlace(piece, row, col)) { "${piece.id} does not fit at $row,$col" }

        val filled = grid.toMutableList()
        for (cell in piece.cells) {
            filled[(row + cell.row) * SIZE + (col + cell.col)] = FILLED
        }
        val beforeClear = Board(filled)

        val fullRows = (0 until SIZE).filter { r -> (0 until SIZE).all { beforeClear.isFilled(r, it) } }
        val fullCols = (0 until SIZE).filter { c -> (0 until SIZE).all { beforeClear.isFilled(it, c) } }
        val fullBoxes = (0 until SIZE).filter { b ->
            boxCells(b).all { (r, c) -> beforeClear.isFilled(r, c) }
        }

        val cleared = buildSet {
            fullRows.forEach { r -> for (c in 0 until SIZE) add(r * SIZE + c) }
            fullCols.forEach { c -> for (r in 0 until SIZE) add(r * SIZE + c) }
            fullBoxes.forEach { b -> boxCells(b).forEach { (r, c) -> add(r * SIZE + c) } }
        }

        val afterClear = if (cleared.isEmpty()) {
            beforeClear
        } else {
            Board(filled.mapIndexed { i, v -> if (i in cleared) EMPTY else v })
        }

        return Placement(
            beforeClear = beforeClear,
            afterClear = afterClear,
            clearedCells = cleared,
            clearedUnits = fullRows.size + fullCols.size + fullBoxes.size,
        )
    }

    companion object {
        const val SIZE = 9
        const val BOX = 3
        const val EMPTY = 0
        const val FILLED = 1

        fun empty(): Board = Board(List(SIZE * SIZE) { EMPTY })

        /** Builds a board from row strings, where any non-'.' character is a filled cell. Test helper. */
        fun of(vararg rows: String): Board {
            require(rows.size == SIZE) { "expected $SIZE rows, got ${rows.size}" }
            return Board(
                rows.flatMap { line ->
                    require(line.length == SIZE) { "expected $SIZE columns, got '$line'" }
                    line.map { if (it == '.') EMPTY else FILLED }
                }
            )
        }

        /** The nine cells of 3x3 box [index], numbered left-to-right then top-to-bottom. */
        fun boxCells(index: Int): List<Pair<Int, Int>> {
            val firstRow = (index / BOX) * BOX
            val firstCol = (index % BOX) * BOX
            return buildList {
                for (r in firstRow until firstRow + BOX) {
                    for (c in firstCol until firstCol + BOX) add(r to c)
                }
            }
        }
    }
}
