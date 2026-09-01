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
 * The board after a tide surge.
 *
 * [overflowed] means a filled cell was shoved off the top edge and destroyed. It takes a column
 * packed to all nine rows to reach that far, and a packed column clears, so this is the toll for
 * a line rather than a losing condition — it's reported because the physics knows it, not
 * because anything ends on it.
 *
 * [lift] says, for every cell of the new board, how many rows it travelled to get there, so the
 * animation can slide exactly what moved and leave everything else alone.
 */
@Immutable
data class Surge(val board: Board, val overflowed: Boolean, val lift: IntArray) {

    /** Rows the cell now at ([row], [col]) rose by, 0 if it was already there. */
    fun liftAt(row: Int, col: Int): Int = lift[row * Board.SIZE + col]

    // IntArray is identity-compared by default, which would make two equal surges unequal and
    // quietly defeat Compose's skipping. Deep-compare it instead.
    override fun equals(other: Any?): Boolean = this === other ||
        (other is Surge && board == other.board && overflowed == other.overflowed &&
            lift.contentEquals(other.lift))

    override fun hashCode(): Int =
        (board.hashCode() * 31 + overflowed.hashCode()) * 31 + lift.contentHashCode()
}

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

    /**
     * The colour slot of the piece that filled this cell, or 0 if it's empty.
     *
     * A cell holds its slot plus one so that zero can go on meaning empty, which keeps the
     * grid a plain list of ints and [isFilled] a comparison against [EMPTY].
     */
    fun colorSlotAt(row: Int, col: Int): Int = (get(row, col) - 1).coerceAtLeast(0)

    /**
     * The board as [SIZE] * [SIZE] characters: '.' for an empty cell, otherwise the colour
     * slot as a digit. Round-trips through [decode] for the saved game.
     */
    fun encode(): String = buildString(SIZE * SIZE) {
        grid.forEach { append(if (it == EMPTY) EMPTY_CHAR else '0' + (it - 1).coerceIn(0, 9)) }
    }

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
     * The cells a drop of [piece] at ([row], [col]) would clear, without placing anything.
     *
     * Lets the drop preview show which rows, columns and boxes a move would complete. Kept
     * separate from [place] rather than built on it because this runs on every pointer move
     * of a drag, and there is no need to allocate two boards to answer it.
     */
    fun clearsFrom(piece: Piece, row: Int, col: Int): Set<Int> {
        if (!canPlace(piece, row, col)) return emptySet()

        val added = piece.cells.mapTo(HashSet()) { (row + it.row) * SIZE + (col + it.col) }
        fun filledAfter(r: Int, c: Int) = isFilled(r, c) || (r * SIZE + c) in added

        return buildSet {
            for (r in 0 until SIZE) {
                if ((0 until SIZE).all { filledAfter(r, it) }) {
                    for (c in 0 until SIZE) add(r * SIZE + c)
                }
            }
            for (c in 0 until SIZE) {
                if ((0 until SIZE).all { filledAfter(it, c) }) {
                    for (r in 0 until SIZE) add(r * SIZE + c)
                }
            }
            for (b in 0 until SIZE) {
                if (boxCells(b).all { (r, c) -> filledAfter(r, c) }) {
                    boxCells(b).forEach { (r, c) -> add(r * SIZE + c) }
                }
            }
        }
    }

    /**
     * Resolves every completed row, column and box on the board as it currently stands.
     *
     * All completions are detected against the same board and cleared together, so a move
     * that finishes a row and a box at once scores both. Separate from [place] because the
     * tide can finish a line the player didn't, and that has to clear the same way.
     */
    fun settle(): Placement {
        val fullRows = (0 until SIZE).filter { r -> (0 until SIZE).all { isFilled(r, it) } }
        val fullCols = (0 until SIZE).filter { c -> (0 until SIZE).all { isFilled(it, c) } }
        val fullBoxes = (0 until SIZE).filter { b -> boxCells(b).all { (r, c) -> isFilled(r, c) } }

        val cleared = buildSet {
            fullRows.forEach { r -> for (c in 0 until SIZE) add(r * SIZE + c) }
            fullCols.forEach { c -> for (r in 0 until SIZE) add(r * SIZE + c) }
            fullBoxes.forEach { b -> boxCells(b).forEach { (r, c) -> add(r * SIZE + c) } }
        }

        val afterClear = if (cleared.isEmpty()) {
            this
        } else {
            Board(grid.mapIndexed { i, v -> if (i in cleared) EMPTY else v })
        }

        return Placement(
            beforeClear = this,
            afterClear = afterClear,
            clearedCells = cleared,
            clearedUnits = fullRows.size + fullCols.size + fullBoxes.size,
        )
    }

    /** Drops [piece] at ([row], [col]) and resolves whatever that completes. */
    fun place(piece: Piece, row: Int, col: Int): Placement {
        require(canPlace(piece, row, col)) { "${piece.id} does not fit at $row,$col" }

        val filled = grid.toMutableList()
        for (cell in piece.cells) {
            filled[(row + cell.row) * SIZE + (col + cell.col)] = piece.colorSlot + 1
        }
        return Board(filled).settle()
    }

    /**
     * Lets the water in, [pushes] rows deep per column, as blocks in colour slot [fillSlot].
     *
     * The water is a piston, one per column, and it only moves what is genuinely in its way.
     * Walk a column up from the floor: a block shifts only when something has come up into its
     * square, and then only as far as it is shoved. A block with air beneath it is in nobody's
     * way, so it stays exactly where it is and the water rises past underneath it.
     *
     * The consequence worth knowing is that slack in a column absorbs the push. Two blocks with
     * a gap between them close up before either of them travels, which is what stops a surge
     * ejecting a block off the top of a board that plainly still has room in it. Only a column
     * with no slack left at all can push anything over the edge, and by then it is nine rows
     * full and about to clear — see [Surge].
     *
     * Columns move by different amounts on purpose, so a run lying across an uneven wave shears
     * rather than lifting flat — see [Tide]. Contact is column-local: a block hanging off the
     * side of a rising stack isn't being pushed by it, and gets left behind as it slides past.
     *
     * Nothing is cleared here; the caller decides whether a surge that completes a line should
     * pay out, and [settle] does the actual clearing.
     */
    fun surge(pushes: IntArray, fillSlot: Int): Surge {
        require(pushes.size == SIZE) { "expected $SIZE pushes, got ${pushes.size}" }

        val next = MutableList(SIZE * SIZE) { EMPTY }
        val lift = IntArray(SIZE * SIZE)
        var overflowed = false

        for (col in 0 until SIZE) {
            val push = pushes[col].coerceIn(0, SIZE)

            // The lowest row still free above everything already placed in this column. Starts
            // at the top of the water, and walks up as the column is rebuilt from the floor.
            var free = SIZE - 1 - push

            for (row in SIZE - 1 downTo 0) {
                val value = get(row, col)
                if (value == EMPTY) continue

                // Stay put unless the space has been taken, then ride only as high as it takes
                // to get out of the way. This is where a gap lower down soaks up the push.
                val to = minOf(row, free)
                free = to - 1

                if (to < 0) {
                    overflowed = true
                } else {
                    next[to * SIZE + col] = value
                    lift[to * SIZE + col] = row - to
                }
            }

            // The water itself, which slid up from off the bottom edge.
            for (row in SIZE - push until SIZE) {
                next[row * SIZE + col] = fillSlot + 1
                lift[row * SIZE + col] = push
            }
        }

        return Surge(Board(next), overflowed, lift)
    }

    companion object {
        const val SIZE = 9
        const val BOX = 3
        const val EMPTY = 0
        const val FILLED = 1

        private const val EMPTY_CHAR = '.'

        fun empty(): Board = Board(List(SIZE * SIZE) { EMPTY })

        /**
         * Rebuilds a board from [encode], or null if the text isn't a board.
         *
         * Anything filled but unrecognised is read as colour slot 0, so a save written before
         * shapes had colours still loads rather than throwing away a game in progress.
         */
        fun decode(text: String): Board? {
            if (text.length != SIZE * SIZE) return null
            return Board(
                text.map { ch ->
                    when {
                        ch == EMPTY_CHAR -> EMPTY
                        ch.isDigit() -> (ch - '0') + 1
                        else -> FILLED
                    }
                }
            )
        }

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
