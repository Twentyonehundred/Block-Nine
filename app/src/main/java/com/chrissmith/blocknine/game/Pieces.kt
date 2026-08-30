package com.chrissmith.blocknine.game

import kotlin.random.Random

/**
 * The shape catalogue and the tray dealer.
 *
 * Shapes are written as ASCII art so the catalogue stays readable: '#' is a filled cell,
 * '.' is empty.
 */
object Pieces {

    private fun piece(id: String, vararg rows: String): Piece {
        val cells = buildList {
            rows.forEachIndexed { r, line ->
                line.forEachIndexed { c, ch -> if (ch != '.') add(Cell(r, c)) }
            }
        }
        require(cells.isNotEmpty()) { "piece $id has no cells" }
        return Piece(id, cells)
    }

    /** Every shape that can be dealt. Rotations are listed explicitly — pieces don't rotate in-hand. */
    val ALL: List<Piece> = listOf(
        // Single
        piece("dot", "#"),

        // Lines
        piece("h2", "##"),
        piece("v2", "#", "#"),
        piece("h3", "###"),
        piece("v3", "#", "#", "#"),
        piece("h4", "####"),
        piece("v4", "#", "#", "#", "#"),
        piece("h5", "#####"),
        piece("v5", "#", "#", "#", "#", "#"),

        // Rectangles
        piece("sq2", "##", "##"),
        piece("rect23", "###", "###"),
        piece("rect32", "##", "##", "##"),
        piece("sq3", "###", "###", "###"),

        // Small corners (3 cells)
        piece("corner-tl", "##", "#."),
        piece("corner-tr", "##", ".#"),
        piece("corner-bl", "#.", "##"),
        piece("corner-br", ".#", "##"),

        // Big corners (5 cells)
        piece("big-tl", "###", "#..", "#.."),
        piece("big-tr", "###", "..#", "..#"),
        piece("big-bl", "#..", "#..", "###"),
        piece("big-br", "..#", "..#", "###"),

        // T shapes
        piece("t-up", "###", ".#."),
        piece("t-down", ".#.", "###"),
        piece("t-left", ".#", "##", ".#"),
        piece("t-right", "#.", "##", "#."),

        // S / Z shapes
        piece("s-h", ".##", "##."),
        piece("z-h", "##.", ".##"),
        piece("s-v", "#.", "##", ".#"),
        piece("z-v", ".#", "##", "#."),

        // L / J tetrominoes
        piece("l-a", "#.", "#.", "##"),
        piece("l-b", "###", "#.."),
        piece("l-c", "##", ".#", ".#"),
        piece("l-d", "..#", "###"),
        piece("j-a", ".#", ".#", "##"),
        piece("j-b", "#..", "###"),
        piece("j-c", "##", "#.", "#."),
        piece("j-d", "###", "..#"),
    )

    const val TRAY_SIZE = 3

    /**
     * Deals a fresh tray of [TRAY_SIZE] pieces.
     *
     * A purely random deal can hand you three pieces that cannot possibly fit, ending the
     * game through no fault of the player. To keep it fair we reroll until at least one
     * piece is placeable, giving up after [attempts] tries — at which point the board
     * genuinely is too congested and the game is legitimately over.
     */
    fun dealTray(board: Board, random: Random = Random.Default, attempts: Int = 16): List<Piece> {
        repeat(attempts) {
            val tray = List(TRAY_SIZE) { ALL.random(random) }
            if (tray.any { board.hasAnyPlacement(it) }) return tray
        }
        return List(TRAY_SIZE) { ALL.random(random) }
    }
}
