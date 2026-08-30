package com.chrissmith.blocknine.game

/**
 * Scoring rules, kept separate so the numbers are easy to tune.
 *
 * Three sources of points:
 *  - one point per cell placed, so progress never stalls completely;
 *  - a quadratic bonus for clears, so a double clear is worth far more than two singles;
 *  - a streak bonus for clearing on consecutive turns.
 */
object Scoring {

    private const val CLEAR_BASE = 18
    private const val STREAK_STEP = 10

    /** Points for a placement that cleared [clearedUnits] rows/columns/boxes at once. */
    fun points(cellsPlaced: Int, clearedUnits: Int, streak: Int): Int {
        var total = cellsPlaced
        if (clearedUnits > 0) {
            total += CLEAR_BASE * clearedUnits * clearedUnits
            if (streak > 1) total += STREAK_STEP * (streak - 1)
        }
        return total
    }
}
