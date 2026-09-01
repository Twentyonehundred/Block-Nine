package com.chrissmith.blocknine.leaderboard

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.chrissmith.blocknine.game.GameMode
import kotlinx.coroutines.launch

/** What the leaderboard sheet is currently showing for one board. */
sealed interface BoardState {
    data object Loading : BoardState

    /** Reads require auth, so signed-out is a state to show rather than a request to attempt. */
    data object SignedOut : BoardState
    data class Loaded(val entries: List<Entry>) : BoardState
    data class Failed(val message: String) : BoardState
}

class LeaderboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LeaderboardRepository(app)

    val configured: Boolean get() = repo.configured

    var player by mutableStateOf(repo.currentPlayer())
        private set

    /** Which mode's board is showing. Opening the sheet from a game preselects that game's mode. */
    var mode by mutableStateOf(GameMode.CLASSIC)
        private set

    var period by mutableStateOf(Period.DAY)
        private set

    var board by mutableStateOf<BoardState>(BoardState.Loading)
        private set

    var signingIn by mutableStateOf(false)
        private set

    /** Set when sign-in fails for a reason worth telling the player about. */
    var error by mutableStateOf<String?>(null)
        private set

    /** Best finished score per mode still waiting on a sign-in before it can be submitted. */
    private val unsubmitted = mutableMapOf<GameMode, Int>()

    fun select(next: Period) {
        if (next == period) return
        period = next
        refresh()
    }

    fun select(next: GameMode) {
        if (next == mode) return
        mode = next
        refresh()
    }

    fun refresh() {
        if (!configured) return
        if (player == null) {
            board = BoardState.SignedOut
            return
        }
        val target = period
        val targetMode = mode
        board = BoardState.Loading
        viewModelScope.launch {
            board = runCatching { repo.top(targetMode, target) }.fold(
                onSuccess = { BoardState.Loaded(it) },
                // Almost always a missing composite index or closed rules; both are setup
                // problems, so surface the real message rather than a generic failure.
                onFailure = { BoardState.Failed(it.message ?: "Couldn't load the leaderboard") },
            )
        }
    }

    /**
     * Called when a game ends. Submits immediately if signed in, otherwise remembers the score
     * so it still counts if the player signs in from the game-over screen.
     */
    fun onGameFinished(mode: GameMode, score: Int) {
        if (!configured || score <= 0) return
        if (player == null) {
            unsubmitted[mode] = maxOf(unsubmitted[mode] ?: 0, score)
            return
        }
        viewModelScope.launch {
            runCatching { repo.submit(mode, score) }
            if (mode == this@LeaderboardViewModel.mode &&
                (period == Period.DAY || period == Period.ALL)
            ) {
                refresh()
            }
        }
    }

    fun signIn(activityContext: Context) {
        if (!configured || signingIn) return
        signingIn = true
        error = null
        viewModelScope.launch {
            runCatching { repo.signIn(activityContext) }
                .onSuccess { signedIn ->
                    player = signedIn
                    // Everything played before signing in still counts, on whichever board it
                    // belongs to.
                    unsubmitted.forEach { (playedMode, score) ->
                        runCatching { repo.submit(playedMode, score) }
                    }
                    unsubmitted.clear()
                    refresh()
                }
                .onFailure { cause ->
                    // Backing out of the Google sheet is a choice, not a failure.
                    if (cause !is GetCredentialCancellationException) {
                        error = cause.message ?: "Sign-in failed"
                    }
                }
            signingIn = false
        }
    }

    fun signOut() {
        repo.signOut()
        player = null
        refresh()
    }
}
