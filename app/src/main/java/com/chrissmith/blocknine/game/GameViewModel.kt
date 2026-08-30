package com.chrissmith.blocknine.game

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A transient "+N" award, shown floating over the board. [id] makes repeats re-animate. */
data class Gain(val id: Long, val points: Int, val clearedUnits: Int, val streak: Int)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var board by mutableStateOf(Board.empty())
        private set

    /** Three slots; a slot becomes null once its piece is placed. All three must be used to refill. */
    var tray by mutableStateOf<List<Piece?>>(emptyList())
        private set

    var score by mutableIntStateOf(0)
        private set

    var best by mutableIntStateOf(prefs.getInt(KEY_BEST, 0))
        private set

    /** Consecutive turns that cleared something. Resets on a turn that clears nothing. */
    var streak by mutableIntStateOf(0)
        private set

    var gameOver by mutableStateOf(false)
        private set

    /** Cells mid-clear. The board still shows them filled so they can flash before vanishing. */
    var clearing by mutableStateOf<Set<Int>>(emptySet())
        private set

    var gain by mutableStateOf<Gain?>(null)
        private set

    private var gainCounter = 0L

    init {
        newGame()
    }

    fun newGame() {
        board = Board.empty()
        tray = Pieces.dealTray(board)
        score = 0
        streak = 0
        gameOver = false
        clearing = emptySet()
        gain = null
    }

    /** True if the piece in [index] can legally land with its top-left corner at ([row], [col]). */
    fun canPlace(index: Int, row: Int, col: Int): Boolean {
        if (gameOver || clearing.isNotEmpty()) return false
        val piece = tray.getOrNull(index) ?: return false
        return board.canPlace(piece, row, col)
    }

    /** True if this piece has nowhere left to go, so the UI can grey it out. */
    fun isDead(piece: Piece): Boolean = !gameOver && !board.hasAnyPlacement(piece)

    fun place(index: Int, row: Int, col: Int) {
        if (!canPlace(index, row, col)) return
        val piece = tray[index] ?: return

        val placement = board.place(piece, row, col)
        streak = if (placement.clearedUnits > 0) streak + 1 else 0

        val points = Scoring.points(piece.size, placement.clearedUnits, streak)
        score += points
        if (score > best) {
            best = score
            prefs.edit().putInt(KEY_BEST, best).apply()
        }
        gain = Gain(gainCounter++, points, placement.clearedUnits, streak)

        tray = tray.toMutableList().also { it[index] = null }

        if (placement.clearedCells.isEmpty()) {
            board = placement.afterClear
            advanceTurn()
        } else {
            // Show the completed lines for a beat, then settle the board.
            board = placement.beforeClear
            clearing = placement.clearedCells
            viewModelScope.launch {
                delay(CLEAR_FLASH_MS)
                board = placement.afterClear
                clearing = emptySet()
                advanceTurn()
            }
        }
    }

    /** Refills the tray once all three are spent, then works out whether anything still fits. */
    private fun advanceTurn() {
        if (tray.all { it == null }) {
            tray = Pieces.dealTray(board)
        }
        gameOver = tray.filterNotNull().none { board.hasAnyPlacement(it) }
    }

    private companion object {
        const val PREFS = "block_nine"
        const val KEY_BEST = "best_score"
        const val CLEAR_FLASH_MS = 190L
    }
}
