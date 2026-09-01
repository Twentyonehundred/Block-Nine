package com.chrissmith.blocknine.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chrissmith.blocknine.game.GameMode
import com.chrissmith.blocknine.game.GameViewModel
import com.chrissmith.blocknine.game.PersonalBests

/**
 * The list of challenge modes.
 *
 * Each one's best is read straight from prefs rather than by spinning up its view model —
 * building a game just to show a number would deal a board and start a clock.
 */
@Composable
fun ChallengesScreen(
    onPlay: (GameMode) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalBoardColors.current
    val context = LocalContext.current
    val bests = remember { GameMode.challenges.associateWith { bestFor(context, it) } }

    BackHandler(onBack = onBack)

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.screen)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(colors = colors, onClick = onBack)
                Text(
                    text = "CHALLENGES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = colors.textMuted,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            GameMode.challenges.forEach { mode ->
                val best = bests[mode] ?: 0
                MenuButton(
                    label = mode.title,
                    sublabel = if (best > 0) "${mode.blurb}  ·  BEST $best" else mode.blurb,
                    primary = true,
                    colors = colors,
                    onClick = { onPlay(mode) },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = mode.rules,
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun bestFor(context: Context, mode: GameMode): Int {
    val prefs = context.getSharedPreferences(GameViewModel.PREFS, Context.MODE_PRIVATE)
    return PersonalBests(prefs, mode.prefsPrefix).allTime
}
