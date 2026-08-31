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

    /** This device's records for today, this month and all time. */
    val bests = PersonalBests(prefs)

    private val save = GameSave(prefs)

    /**
     * The all-time record as it stood when this game began.
     *
     * Frozen for the duration, because [bests] is updated on every move: read live, the BEST
     * line just shadows your score once you're ahead, and there's no target left to chase.
     */
    var bestToBeat by mutableIntStateOf(0)
        private set

    /** True once this game has overtaken the record it started against. */
    val beatenBest: Boolean get() = bestToBeat > 0 && score > bestToBeat

    /** Set for one animation as the record falls. Keyed so it plays exactly once. */
    var newBestMoment by mutableStateOf<Long?>(null)
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
        resume()
    }

    /** Picks up the saved game if there is one, otherwise starts a fresh one. */
    private fun resume() {
        val snapshot = save.read()
        if (snapshot == null) {
            newGame()
            return
        }

        bests.refresh()
        board = snapshot.board
        tray = snapshot.tray
        score = snapshot.score
        streak = snapshot.streak
        bestToBeat = snapshot.bestToBeat
        clearing = emptySet()
        gain = null
        newBestMoment = null
        gameOver = tray.filterNotNull().none { board.hasAnyPlacement(it) }

        // Finished games aren't saved, so this shouldn't happen — but a dead board with no way
        // to play on would be a nasty thing to hand someone on launch.
        if (gameOver) newGame()
    }

    fun newGame() {
        bests.refresh()
        board = Board.empty()
        tray = Pieces.dealTray(board)
        score = 0
        streak = 0
        bestToBeat = bests.allTime
        newBestMoment = null
        gameOver = false
        clearing = emptySet()
        gain = null
        save.write(board, tray, score, streak, bestToBeat)
    }

    /** True if the piece in [index] can legally land with its top-left corner at ([row], [col]). */
    fun canPlace(index: Int, row: Int, col: Int): Boolean {
        if (gameOver || clearing.isNotEmpty()) return false
        val piece = tray.getOrNull(index) ?: return false
        return board.canPlace(piece, row, col)
    }

    /** The cells a drop of tray piece [index] would clear. Empty unless the move both fits and scores. */
    fun previewClears(index: Int, row: Int, col: Int): Set<Int> {
        if (!canPlace(index, row, col)) return emptySet()
        val piece = tray.getOrNull(index) ?: return emptySet()
        return board.clearsFrom(piece, row, col)
    }

    /**
     * True if this piece has nowhere left to go, so the UI can grey it out.
     *
     * Still answered once the game is over, which is when every piece left in the tray is
     * dead: the game-over card should show why it ended, not a tray that looks playable.
     */
    fun isDead(piece: Piece): Boolean = !board.hasAnyPlacement(piece)

    fun place(index: Int, row: Int, col: Int) {
        if (!canPlace(index, row, col)) return
        val piece = tray[index] ?: return

        val placement = board.place(piece, row, col)
        streak = if (placement.clearedUnits > 0) streak + 1 else 0

        val points = Scoring.points(piece.size, placement.clearedUnits, streak)
        val wasBehind = !beatenBest
        score += points
        bests.record(score)
        gain = Gain(gainCounter++, points, placement.clearedUnits, streak)
        if (wasBehind && beatenBest) newBestMoment = gainCounter++

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

    /**
     * Refills the tray once all three are spent, works out whether anything still fits, and
     * commits the result to disk.
     *
     * The save happens here rather than in [place] so that a move which clears lines is stored
     * settled: the ~200ms flash before the cells vanish is the one window where a save would
     * capture a board that's about to change.
     */
    private fun advanceTurn() {
        if (tray.all { it == null }) {
            tray = Pieces.dealTray(board)
        }
        gameOver = tray.filterNotNull().none { board.hasAnyPlacement(it) }

        // A finished game has already been counted towards the records, so there is nothing
        // left to resume — dropping it means the next launch deals a fresh board.
        if (gameOver) save.clear() else save.write(board, tray, score, streak, bestToBeat)
    }

    private companion object {
        const val PREFS = "block_nine"
        const val CLEAR_FLASH_MS = 190L
    }
}
