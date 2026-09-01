package com.chrissmith.blocknine.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chrissmith.blocknine.game.Cell
import com.chrissmith.blocknine.game.GameMode
import com.chrissmith.blocknine.game.PersonalBests
import com.chrissmith.blocknine.leaderboard.LeaderboardViewModel

/** The shape in the wordmark: a 3x3 with enough gaps to read as a puzzle rather than a block. */
private val LOGO_CELLS = listOf(
    Cell(0, 0), Cell(0, 1),
    Cell(1, 1), Cell(1, 2),
    Cell(2, 0), Cell(2, 2),
)

@Composable
fun MainMenuScreen(
    /** The classic game in progress, if there is one, so Play can offer to carry on with it. */
    gameInProgress: Int,
    bests: PersonalBests,
    leaderboard: LeaderboardViewModel,
    settings: SettingsViewModel,
    onPlay: () -> Unit,
    onChallenges: () -> Unit,
) {
    val colors = LocalBoardColors.current
    var showLeaderboard by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.screen)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(start = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountButton(
                photoUrl = leaderboard.player?.photoUrl,
                colors = colors,
                onClick = { showLeaderboard = true },
            )
            SettingsButton(colors = colors, onClick = { showSettings = true })
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoGlyph(colors = colors)
            Spacer(Modifier.height(18.dp))
            Text(
                text = "BLOCK NINE",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (bests.allTime > 0) "BEST ${bests.allTime}" else "NO SCORE YET",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = colors.textMuted,
            )

            Spacer(Modifier.height(44.dp))

            MenuButton(
                label = if (gameInProgress > 0) "Continue" else "Play",
                sublabel = if (gameInProgress > 0) "Classic · $gameInProgress" else GameMode.CLASSIC.rules,
                primary = true,
                colors = colors,
                onClick = onPlay,
            )
            Spacer(Modifier.height(12.dp))
            MenuButton(
                label = "Challenges",
                sublabel = "Different rules, separate records",
                primary = false,
                colors = colors,
                onClick = onChallenges,
            )
            Spacer(Modifier.height(12.dp))
            MenuButton(
                label = "Leaderboard",
                sublabel = "Daily, monthly and all time",
                primary = false,
                colors = colors,
                onClick = { showLeaderboard = true },
            )
        }
    }

    if (showLeaderboard) {
        LeaderboardOverlay(
            vm = leaderboard,
            bests = bests,
            colors = colors,
            onDismiss = { showLeaderboard = false },
        )
    }

    if (showSettings) {
        SettingsOverlay(
            settings = settings,
            colors = colors,
            onDismiss = { showSettings = false },
        )
    }
}

/** A few tiles in the theme's own colours, so the menu shows what the game looks like. */
@Composable
private fun LogoGlyph(colors: BoardColors, modifier: Modifier = Modifier) {
    val painter = rememberTilePainter(colors)
    val tileStyle = LocalTileStyle.current

    Canvas(modifier.size(84.dp)) {
        val cell = size.width / 3f
        LOGO_CELLS.forEachIndexed { index, c ->
            drawTile(
                left = c.col * cell,
                top = c.row * cell,
                cell = cell,
                fill = painter.fill(index),
                edge = painter.edge(index),
                style = tileStyle,
            )
        }
    }
}

/**
 * One row on a menu. [primary] marks the thing you almost always came here to do, which gets
 * the filled treatment; everything else is a bordered card so the screen stays calm.
 */
@Composable
internal fun MenuButton(
    label: String,
    sublabel: String,
    primary: Boolean,
    colors: BoardColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (primary) colors.tile else colors.boxShaded,
        shadowElevation = if (primary) 6.dp else 0.dp,
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (primary) Color.White else colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sublabel,
                fontSize = 12.sp,
                color = if (primary) Color.White.copy(alpha = 0.82f) else colors.textMuted,
            )
        }
    }
}

/** A back chevron sized to match the round header buttons. */
@Composable
internal fun BackButton(colors: BoardColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.boxShaded)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "‹",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMuted,
        )
    }
}
