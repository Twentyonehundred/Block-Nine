package com.chrissmith.blocknine.game

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Scoring rules, kept separate so the numbers are easy to tune.
 *
 * Three sources of points:
 *  - one point per cell placed, so progress never stalls completely;
 *  - a quadratic bonus for clearing several units with one piece, so taking out a row and a
 *    column together is worth far more than taking them out one at a time;
 *  - a combo multiplier that grows while you clear on consecutive turns and drops straight
 *    back to nothing on a turn that clears nothing.
 *
 * The combo is a multiplier rather than a flat bonus so that keeping a run alive is worth
 * something on a big move as well as a small one. It is capped, so a long run pays well
 * without turning one lucky streak into an unbeatable score.
 */
object Scoring {

    /**
     * Points for the first unit cleared. Every extra unit in the same move pays this again
     * for each unit already going, which is what makes the clear bonus quadratic: one unit
     * is 18, two is 72, three is 162.
     */
    private const val CLEAR_BASE = 18

    /** How much each consecutive clearing turn adds to the multiplier. */
    private const val COMBO_STEP = 0.3f

    /** Ceiling on the multiplier, reached on the eighth clearing turn in a row. */
    private const val MAX_COMBO = 3f

    /** What a run of [streak] consecutive clearing turns multiplies the clear bonus by. */
    fun comboMultiplier(streak: Int): Float =
        if (streak <= 1) 1f else min(1f + COMBO_STEP * (streak - 1), MAX_COMBO)

    /** Points for a placement that cleared [clearedUnits] rows/columns/boxes at once. */
    fun points(cellsPlaced: Int, clearedUnits: Int, streak: Int): Int {
        if (clearedUnits <= 0) return cellsPlaced
        val clear = CLEAR_BASE * clearedUnits * clearedUnits
        return cellsPlaced + (clear * comboMultiplier(streak)).roundToInt()
    }

    /**
     * The shout-out for a move, or null when it was an ordinary placement.
     *
     * Both halves can fire at once: a piece that takes out a row and a column on the third
     * clearing turn running reads "DOUBLE CLEAR · COMBO ×3".
     */
    fun label(clearedUnits: Int, streak: Int): String? {
        val multi = when {
            clearedUnits >= 5 -> "MEGA CLEAR ×$clearedUnits"
            clearedUnits == 4 -> "QUAD CLEAR"
            clearedUnits == 3 -> "TRIPLE CLEAR"
            clearedUnits == 2 -> "DOUBLE CLEAR"
            else -> null
        }
        val combo = if (clearedUnits > 0 && streak > 1) "COMBO ×$streak" else null
        return listOfNotNull(multi, combo).joinToString(" · ").ifEmpty { null }
    }
}
