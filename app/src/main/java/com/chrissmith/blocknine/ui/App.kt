package com.chrissmith.blocknine.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrissmith.blocknine.game.GameMode
import com.chrissmith.blocknine.game.GameViewModel
import com.chrissmith.blocknine.leaderboard.LeaderboardViewModel

/**
 * The whole app: a menu, a list of challenges, and a game.
 *
 * Deliberately three states and a couple of booleans rather than a navigation library. There
 * are no arguments to pass, no deep links and no back stack worth the name — the only thing
 * that has to survive is which screen you're on, and the game itself lives in a view model
 * that outlives all of this.
 *
 * Each mode gets its own [GameViewModel] keyed by name, so stepping out to the menu and back
 * puts you in the game you left rather than a fresh board. Nothing in any mode runs on a
 * clock, so there is nothing here that has to be paused.
 */
@Composable
fun BlockNineApp(settings: SettingsViewModel) {
    val leaderboard: LeaderboardViewModel = viewModel()

    // Built up front rather than on first play: the menu needs its score to offer Continue,
    // and building it is what reads the saved game back off disk.
    val classic: GameViewModel = viewModel(
        key = GameMode.CLASSIC.name,
        factory = GameViewModel.factory(GameMode.CLASSIC),
    )

    var playing by rememberSaveable { mutableStateOf<GameMode?>(null) }
    var onChallenges by rememberSaveable { mutableStateOf(false) }

    val mode = playing
    when {
        mode != null -> {
            val vm: GameViewModel = if (mode == GameMode.CLASSIC) {
                classic
            } else {
                viewModel(key = mode.name, factory = GameViewModel.factory(mode))
            }
            GameScreen(
                vm = vm,
                leaderboard = leaderboard,
                settings = settings,
                onExit = { playing = null },
            )
        }

        onChallenges -> ChallengesScreen(
            onPlay = { playing = it },
            onBack = { onChallenges = false },
        )

        else -> MainMenuScreen(
            // A finished game is nothing to carry on with, so the button reads Play again.
            gameInProgress = if (classic.gameOver) 0 else classic.score,
            bests = classic.bests,
            leaderboard = leaderboard,
            settings = settings,
            onPlay = { playing = GameMode.CLASSIC },
            onChallenges = { onChallenges = true },
        )
    }
}
