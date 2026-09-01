package com.chrissmith.blocknine.game

import kotlin.random.Random

/**
 * The Rising Tide surge.
 *
 * The whole point of the mode is that the water does *not* lift the board evenly. A flat push
 * of one row across all nine columns would cost you space and change nothing else: every gap
 * would still be exactly where it was, relative to every other gap, and a row you were three
 * cells from finishing would still be three cells from finishing.
 *
 * So instead each surge drops a wave profile somewhere along the board and pushes each column
 * by a different amount. Anything spanning those columns shears — a straight run becomes a
 * staircase, gaps that lined up stop lining up, and a nearly-finished 3x3 box gets its
 * contents dragged across two rows. That shear is the mechanic; the lost space is incidental.
 */
object Tide {

    /**
     * Pieces played between surges.
     *
     * Counted in moves rather than seconds on purpose. A clock would turn a puzzle into a
     * reaction test and punish you for thinking, and it would keep running while you looked
     * away. Three is one full tray, so the tide comes in exactly as your next three arrive —
     * which means the emptying tray doubles as the countdown.
     */
    const val PIECES_PER_SURGE = 3

    /**
     * The push profiles, in the order a run works through them.
     *
     * Every profile has at least two distinct values, because a run of equal numbers is the
     * flat push this mode exists to avoid. They widen rather than steepen for most of the run,
     * so the board deforms in more places before it starts deforming more violently.
     */
    private val PROFILES = listOf(
        intArrayOf(1, 2, 1),
        intArrayOf(1, 2, 2, 1),
        intArrayOf(1, 2, 2, 2, 1),
        intArrayOf(1, 2, 3, 2, 1),
    )

    /** Surges at one profile before the next one takes over. */
    private const val SURGES_PER_LEVEL = 3

    /** The most rows any single column can be shoved, used to scale the warning bar. */
    val MAX_PUSH: Int = PROFILES.maxOf { profile -> profile.max() }

    /**
     * The wave for surge number [surgeIndex], as a push amount per column.
     *
     * The profile is dropped at a random offset and allowed to hang off either edge, so the
     * sides of the board are no safer to build against than the middle.
     */
    fun wave(surgeIndex: Int, random: Random = Random.Default): IntArray {
        val profile = PROFILES[(surgeIndex / SURGES_PER_LEVEL).coerceIn(PROFILES.indices)]
        val overhang = profile.size / 2
        val start = random.nextInt(-overhang, Board.SIZE - overhang)
        return IntArray(Board.SIZE) { col ->
            val index = col - start
            if (index in profile.indices) profile[index] else 0
        }
    }
}
