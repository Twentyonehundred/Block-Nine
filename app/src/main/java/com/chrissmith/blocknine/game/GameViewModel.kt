package com.chrissmith.blocknine.game

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A transient "+N" award, shown floating over the board. [id] makes repeats re-animate. */
data class Gain(val id: Long, val points: Int, val clearedUnits: Int, val streak: Int)

class GameViewModel(app: Application, val mode: GameMode = GameMode.CLASSIC) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var board by mutableStateOf(Board.empty())
        private set

    /** Three slots; a slot becomes null once its piece is placed. All three must be used to refill. */
    var tray by mutableStateOf<List<Piece?>>(emptyList())
        private set

    var score by mutableIntStateOf(0)
        private set

    /** This device's records for today, this month and all time, scoped to [mode]. */
    val bests = PersonalBests(prefs, mode.prefsPrefix)

    /** Only modes worth resuming get a save file; a timed run isn't one. */
    private val save = if (mode.isResumable) GameSave(prefs) else null

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

    // ---- Rising Tide ----------------------------------------------------------------------

    /** What each column will be pushed by at the next surge. Decided as soon as the last one lands. */
    var pendingWave by mutableStateOf(IntArray(0))
        private set

    /**
     * How far each cell of the board rose in the surge that just fired, so it can animate the
     * shove. Per cell, not per column: the water only carries what it touches.
     */
    var lastLift by mutableStateOf(IntArray(0))
        private set

    /** Pieces still to play before the water comes in. */
    var piecesUntilSurge by mutableIntStateOf(0)
        private set

    /** 0 just after a surge, 1 once the very next piece will bring it in. */
    val tideProgress: Float
        get() = 1f - (piecesUntilSurge - 1).toFloat() / (Tide.PIECES_PER_SURGE - 1)

    /** Bumped on every surge. Keys the shove animation. */
    var surgeMoment by mutableStateOf<Long?>(null)
        private set

    private var surgeCount = 0

    init {
        if (mode.isResumable) resume() else newGame()
    }

    /** Picks up the saved game if there is one, otherwise starts a fresh one. */
    private fun resume() {
        val snapshot = save?.read()
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
        save?.write(board, tray, score, streak, bestToBeat)
        if (mode == GameMode.RISING_TIDE) armTide()
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
        award(Scoring.points(piece.size, placement.clearedUnits, streak), placement.clearedUnits, streak)

        tray = tray.toMutableList().also { it[index] = null }
        resolve(placement) { endTurn() }
    }

    /** Files points against the score and fires the floating "+N", plus the record moment if it falls. */
    private fun award(points: Int, clearedUnits: Int, streak: Int) {
        val wasBehind = !beatenBest
        score += points
        bests.record(score)
        gain = Gain(gainCounter++, points, clearedUnits, streak)
        if (wasBehind && beatenBest) newBestMoment = gainCounter++
    }

    /**
     * Commits a placement to the board, holding the completed lines onscreen for a beat first
     * so they can be seen going. [then] runs once the board has settled.
     */
    private fun resolve(placement: Placement, then: () -> Unit) {
        if (placement.clearedCells.isEmpty()) {
            board = placement.afterClear
            then()
            return
        }

        board = placement.beforeClear
        clearing = placement.clearedCells
        viewModelScope.launch {
            delay(CLEAR_FLASH_MS)
            board = placement.afterClear
            clearing = emptySet()
            then()
        }
    }

    /**
     * Closes out a move: lets the tide in if this piece was the one that called it, then
     * refills and works out whether the game can go on.
     *
     * The surge happens before the refill so that the next three pieces are dealt against the
     * board you'll actually be playing on, rather than the one the water is about to rearrange.
     */
    private fun endTurn() {
        if (mode == GameMode.RISING_TIDE && --piecesUntilSurge <= 0) {
            surge { restock() }
        } else {
            restock()
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
    private fun restock() {
        if (tray.all { it == null }) {
            tray = Pieces.dealTray(board)
        }
        checkAlive()

        // A finished game has already been counted towards the records, so there is nothing
        // left to resume — dropping it means the next launch deals a fresh board.
        if (!gameOver) save?.write(board, tray, score, streak, bestToBeat)
    }

    private fun checkAlive() {
        gameOver = tray.filterNotNull().none { board.hasAnyPlacement(it) }
        if (gameOver) save?.clear()
    }

    /**
     * Sets up the first wave.
     *
     * The wave is settled a full tray before it lands so it can be shown coming: being shoved
     * is fair, being shoved by surprise is not.
     */
    private fun armTide() {
        surgeCount = 0
        surgeMoment = null
        lastLift = IntArray(0)
        pendingWave = Tide.wave(0)
        piecesUntilSurge = Tide.PIECES_PER_SURGE
    }

    /**
     * Lets the water in, then runs [then] once the board has finished moving.
     *
     * A surge can finish rows the player never touched. Those clear and pay out, because
     * refusing to score them would mean watching a completed row sit there; they don't feed
     * the combo streak though, which stays a record of the player's own consecutive clears.
     *
     * The water can also crush a block off the top edge, but that is never itself the end of a
     * run. A column only has the reach to eject anything once it has filled all nine rows, and
     * a filled column clears — so the crush is the toll for a line, not a death. What finishes
     * a Rising Tide game is the same thing that finishes a classic one: nothing fits any more.
     */
    private fun surge(then: () -> Unit) {
        val result = board.surge(pendingWave, Pieces.COLOUR_SLOTS)

        surgeCount++
        lastLift = result.lift
        pendingWave = Tide.wave(surgeCount)
        piecesUntilSurge = Tide.PIECES_PER_SURGE
        surgeMoment = gainCounter++

        val settled = result.board.settle()
        if (settled.clearedUnits > 0) {
            award(Scoring.points(0, settled.clearedUnits, 0), settled.clearedUnits, 0)
        }
        resolve(settled, then)
    }

    companion object {
        /** Shared with anything that needs to read a record without starting a game. */
        const val PREFS = "block_nine"

        private const val CLEAR_FLASH_MS = 190L

        /** Builds a view model for one [mode]; pair with a `viewModel(key = mode.name, ...)`. */
        fun factory(mode: GameMode) = viewModelFactory {
            initializer { GameViewModel(this[APPLICATION_KEY] as Application, mode) }
        }
    }
}
