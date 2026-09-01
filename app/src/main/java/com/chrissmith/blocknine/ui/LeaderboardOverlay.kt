package com.chrissmith.blocknine.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chrissmith.blocknine.game.GameMode
import com.chrissmith.blocknine.game.PersonalBests
import com.chrissmith.blocknine.leaderboard.BoardState
import com.chrissmith.blocknine.leaderboard.Entry
import com.chrissmith.blocknine.leaderboard.LeaderboardViewModel
import com.chrissmith.blocknine.leaderboard.Period

@Composable
fun LeaderboardOverlay(
    vm: LeaderboardViewModel,
    /** Which board to open on. Opening from a game shows that game's mode first. */
    mode: GameMode,
    colors: BoardColors,
    onDismiss: () -> Unit,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current

    // Every mode's device records, read fresh each time the sheet opens. Read from prefs rather
    // than taken from the running game so the tabs can show a mode that isn't being played.
    val bests = remember { GameMode.entries.associateWith { PersonalBests.of(context, it) } }
    val shown = bests.getValue(vm.mode)

    LaunchedEffect(Unit) {
        vm.select(mode)
        bests.values.forEach { it.refresh() }
        vm.refresh()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.screen,
            // shadowElevation, not tonalElevation: the latter tints the surface with the
            // primary colour, which turns the light-theme card lilac.
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                // Swallow taps on the card so it doesn't dismiss itself.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                Text(
                    text = "Leaderboard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(Modifier.height(16.dp))
                // Modes above periods: which game you're looking at is the coarser question, and
                // the personal bests underneath belong to whichever mode is selected here.
                Tabs(
                    options = GameMode.entries,
                    selected = vm.mode,
                    label = { it.shortTitle },
                    colors = colors,
                    onSelect = vm::select,
                )

                Spacer(Modifier.height(14.dp))
                PersonalBestStrip(bests = shown, colors = colors)

                Spacer(Modifier.height(18.dp))
                Tabs(
                    options = Period.entries,
                    selected = vm.period,
                    label = { it.label },
                    colors = colors,
                    onSelect = vm::select,
                )
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 340.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        !vm.configured -> Notice(
                            "The leaderboard isn't set up yet.\nYour best score is still saved on this device.",
                            colors,
                        )

                        else -> when (val state = vm.board) {
                            is BoardState.Loading -> CircularProgressIndicator(color = colors.accent)
                            is BoardState.SignedOut -> Notice(
                                "Sign in to see how you rank against everyone else.",
                                colors,
                            )
                            is BoardState.Failed -> Notice(state.message, colors)
                            is BoardState.Loaded -> if (state.entries.isEmpty()) {
                                Notice(emptyMessage(vm.period), colors)
                            } else {
                                Standings(
                                    entries = state.entries,
                                    meUid = vm.player?.uid,
                                    colors = colors,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                AccountRow(vm = vm, colors = colors, onSignIn = { activity?.let(vm::signIn) })

                vm.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = colors.invalid,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Close", color = colors.textMuted, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * The player's own three records. Above the leaderboard rather than inside it because this
 * part works signed out — it's the whole scoreboard for anyone who never signs in.
 */
@Composable
private fun PersonalBestStrip(bests: PersonalBests, colors: BoardColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.boxShaded)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = "YOUR BEST",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            BestCell("Today", bests.day, colors, Modifier.weight(1f))
            BestCell("This month", bests.month, colors, Modifier.weight(1f))
            BestCell("All time", bests.allTime, colors, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BestCell(label: String, score: Int, colors: BoardColors, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = score.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            // A zero is a prompt to go and set one, not an achievement, so it stays quiet.
            color = if (score > 0) colors.textPrimary else colors.textMuted,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textMuted,
            maxLines = 1,
        )
    }
}

private fun emptyMessage(period: Period) = when (period) {
    Period.DAY -> "No scores today yet.\nBe the first."
    Period.MONTH -> "No scores this month yet."
    Period.ALL -> "No scores yet."
}

/** A segmented control. Used for both rows of tabs so the two read as one stacked control. */
@Composable
private fun <T> Tabs(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    colors: BoardColors,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.boxShaded)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) colors.tile else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) Color.White else colors.textMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Standings(
    entries: List<Entry>,
    meUid: String?,
    colors: BoardColors,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(entries, key = { _, entry -> entry.uid }) { index, entry ->
            StandingRow(
                rank = index + 1,
                entry = entry,
                isMe = entry.uid == meUid,
                colors = colors,
            )
        }
    }
}

/** Centred explanatory text used for the empty, unconfigured and failed states. */
@Composable
private fun Notice(message: String, colors: BoardColors) {
    Text(
        text = message,
        fontSize = 13.sp,
        color = colors.textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun StandingRow(
    rank: Int,
    entry: Entry,
    isMe: Boolean,
    colors: BoardColors,
) {
    // Rank 1 is the champion for whichever board is showing, so it gets the accent treatment.
    val champion = rank == 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    champion -> colors.tile.copy(alpha = 0.16f)
                    isMe -> colors.boxShaded
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (champion) "🏆" else "$rank",
            fontSize = if (champion) 15.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        Spacer(Modifier.width(8.dp))
        Initials(name = entry.name, champion = champion, colors = colors)
        Spacer(Modifier.width(10.dp))
        Text(
            text = entry.name,
            fontSize = 15.sp,
            fontWeight = if (champion || isMe) FontWeight.Bold else FontWeight.Medium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = entry.score.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (champion) colors.accent else colors.textPrimary,
        )
    }
}

/** Avatar stand-in. Initials rather than the Google photo so there's no image-loading dependency. */
@Composable
private fun Initials(name: String, champion: Boolean, colors: BoardColors) {
    val letters = name.trim().split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (champion) colors.tile else colors.boxShaded),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letters,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (champion) Color.White else colors.textMuted,
        )
    }
}

@Composable
private fun AccountRow(
    vm: LeaderboardViewModel,
    colors: BoardColors,
    onSignIn: () -> Unit,
) {
    if (!vm.configured) return

    val player = vm.player
    if (player == null) {
        Button(
            onClick = onSignIn,
            enabled = !vm.signingIn,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.tile,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = if (vm.signingIn) "Signing in…" else "Sign in with Google",
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sign in to put your scores on the board.",
            fontSize = 12.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Playing as ${player.name}",
                fontSize = 12.sp,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = vm::signOut) {
                Text("Sign out", fontSize = 12.sp, color = colors.textMuted)
            }
        }
    }
}
