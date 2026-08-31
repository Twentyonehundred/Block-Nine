package com.chrissmith.blocknine.game

import kotlin.random.Random

/**
 * The shape catalogue and the tray dealer.
 *
 * Shapes are written as ASCII art so the catalogue stays readable: '#' is a filled cell,
 * '.' is empty.
 */
object Pieces {

    /** A shape together with how often it should come up relative to every other shape. */
    private class Entry(val piece: Piece, val weight: Int)

    private fun piece(id: String, vararg rows: String): Piece {
        val cells = buildList {
            rows.forEachIndexed { r, line ->
                line.forEachIndexed { c, ch -> if (ch != '.') add(Cell(r, c)) }
            }
        }
        require(cells.isNotEmpty()) { "piece $id has no cells" }
        return Piece(id, cells)
    }

    private fun entry(id: String, weight: Int, vararg rows: String) =
        Entry(piece(id, *rows), weight)

    /**
     * Every shape that can be dealt, with its relative weight. Rotations are listed explicitly
     * — pieces don't rotate in-hand.
     *
     * The deal is weighted rather than uniform. With this many shapes a flat draw makes each one
     * turn up about 3% of the time, which buries the small pieces that let you dig out of a tight
     * board and hands out the awkward 2x3 slabs as often as anything else. Weights are relative
     * only: a 8 is dealt eight times as often as a 1, and the scale has no other meaning.
     */
    private val SHAPES: List<Entry> = listOf(
        // Small change. These are the release valve on a congested board, so they are the most
        // common things in the bag.
        entry("dot", 8, "#"),
        entry("h2", 8, "##"),
        entry("v2", 8, "#", "#"),

        // Lines. Longer means rarer — a 5 needs a whole clear row to land.
        entry("h3", 5, "###"),
        entry("v3", 5, "#", "#", "#"),
        entry("h4", 3, "####"),
        entry("v4", 3, "#", "#", "#", "#"),
        entry("h5", 2, "#####"),
        entry("v5", 2, "#", "#", "#", "#", "#"),

        // Rectangles. No 3x3: it demands an entirely empty box and ends too many games on its
        // own. The 2x3 slabs are kept but made scarce for the same reason, milder.
        entry("sq2", 6, "##", "##"),
        entry("rect23", 1, "###", "###"),
        entry("rect32", 1, "##", "##", "##"),

        // Small corners (3 cells). Forgiving, so common.
        entry("corner-tl", 4, "##", "#."),
        entry("corner-tr", 4, "##", ".#"),
        entry("corner-bl", 4, "#.", "##"),
        entry("corner-br", 4, ".#", "##"),

        // Big corners (5 cells).
        entry("big-tl", 2, "###", "#..", "#.."),
        entry("big-tr", 2, "###", "..#", "..#"),
        entry("big-bl", 2, "#..", "#..", "###"),
        entry("big-br", 2, "..#", "..#", "###"),

        // T shapes.
        entry("t-up", 3, "###", ".#."),
        entry("t-down", 3, ".#.", "###"),
        entry("t-left", 3, ".#", "##", ".#"),
        entry("t-right", 3, "#.", "##", "#."),

        // Long T shapes: five cells on a full 3x3 footprint, so as scarce as the big corners.
        // Each is the small T of the same name with its stem stretched by one.
        entry("t5-up", 2, "###", ".#.", ".#."),
        entry("t5-down", 2, ".#.", ".#.", "###"),
        entry("t5-left", 2, "..#", "###", "..#"),
        entry("t5-right", 2, "#..", "###", "#.."),

        // U shapes: a run of three with a cell stood up at each end. Named for the way the
        // mouth faces. Only 3x2, so a shade less demanding than the long Ts.
        entry("u-up", 2, "#.#", "###"),
        entry("u-down", 2, "###", "#.#"),
        entry("u-left", 2, "##", ".#", "##"),
        entry("u-right", 2, "##", "#.", "##"),

        // S / Z shapes. The hardest four-cell shapes to place, so the rarest of them.
        entry("s-h", 2, ".##", "##."),
        entry("z-h", 2, "##.", ".##"),
        entry("s-v", 2, "#.", "##", ".#"),
        entry("z-v", 2, ".#", "##", "#."),

        // L / J tetrominoes.
        entry("l-a", 3, "#.", "#.", "##"),
        entry("l-b", 3, "###", "#.."),
        entry("l-c", 3, "##", ".#", ".#"),
        entry("l-d", 3, "..#", "###"),
        entry("j-a", 3, ".#", ".#", "##"),
        entry("j-b", 3, "#..", "###"),
        entry("j-c", 3, "##", "#.", "#."),
        entry("j-d", 3, "###", "..#"),
    )

    /** How many fills the multicolour option cycles through. */
    const val COLOUR_SLOTS = 6

    /**
     * The catalogue proper, with each shape given a colour slot.
     *
     * Slots are handed out by position, so neighbouring entries — which is to say the four
     * rotations of the same shape — never share a colour, and a family of four spreads across
     * the palette instead of clumping.
     */
    private val CATALOGUE: List<Entry> = SHAPES.mapIndexed { index, entry ->
        Entry(entry.piece.copy(colorSlot = index % COLOUR_SLOTS), entry.weight)
    }

    /** Every dealable shape, weights discarded. */
    val ALL: List<Piece> = CATALOGUE.map { it.piece }

    /** Finds a shape by id, for rebuilding a tray from a saved game. Null if the id is unknown. */
    fun byId(id: String): Piece? = ALL.firstOrNull { it.id == id }

    private val totalWeight: Int = CATALOGUE.sumOf { it.weight }

    const val TRAY_SIZE = 3

    /** Draws one shape, respecting the catalogue weights. */
    private fun randomPiece(random: Random): Piece {
        var roll = random.nextInt(totalWeight)
        for (entry in CATALOGUE) {
            roll -= entry.weight
            if (roll < 0) return entry.piece
        }
        // Unreachable: the weights sum to totalWeight, so the roll always lands.
        return CATALOGUE.last().piece
    }

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
            val tray = List(TRAY_SIZE) { randomPiece(random) }
            if (tray.any { board.hasAnyPlacement(it) }) return tray
        }
        return List(TRAY_SIZE) { randomPiece(random) }
    }
}
