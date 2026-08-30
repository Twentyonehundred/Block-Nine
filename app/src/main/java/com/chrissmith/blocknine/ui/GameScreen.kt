package com.chrissmith.blocknine.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrissmith.blocknine.game.Board
import com.chrissmith.blocknine.game.Cell
import com.chrissmith.blocknine.game.GameViewModel
import com.chrissmith.blocknine.game.Piece
import com.chrissmith.blocknine.game.Scoring
import com.chrissmith.blocknine.leaderboard.LeaderboardViewModel
import kotlin.math.min
import kotlin.math.roundToInt

/** A piece currently under the finger. [pointer] is in root coordinates. */
private data class DragState(val index: Int, val piece: Piece, val pointer: Offset)

/** Where the piece's top-left bounding-box corner sits, given the finger position. */
private data class Target(val row: Int, val col: Int)

/**
 * Lifts the dragged piece above the finger so the thumb doesn't cover the drop zone —
 * without this the game is nearly unplayable one-handed.
 */
private fun draggedTopLeft(piece: Piece, pointer: Offset, cell: Float) = Offset(
    x = pointer.x - piece.width * cell / 2f,
    y = pointer.y - piece.height * cell / 2f - cell * 1.25f,
)

@Composable
fun GameScreen(
    vm: GameViewModel = viewModel(),
    leaderboard: LeaderboardViewModel = viewModel(),
    settings: SettingsViewModel = viewModel(),
) {
    val colors = LocalBoardColors.current
    val haptics = LocalHapticFeedback.current

    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardCell by remember { mutableFloatStateOf(0f) }
    var drag by remember { mutableStateOf<DragState?>(null) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmNewGame by remember { mutableStateOf(false) }

    // A finished game is the only thing worth ranking, so submit exactly once per game over.
    LaunchedEffect(vm.gameOver) {
        if (vm.gameOver) leaderboard.onGameFinished(vm.score)
    }

    // Abandoning a game part-way still counts — otherwise a good run thrown away by tapping
    // NEW would silently never reach the board.
    fun startNewGame() {
        if (!vm.gameOver) leaderboard.onGameFinished(vm.score)
        vm.newGame()
    }

    // Only worth interrupting for if there's a game in progress to lose.
    fun requestNewGame() {
        if (vm.score > 0 && !vm.gameOver) confirmNewGame = true else startNewGame()
    }

    // Derived on demand rather than captured in a value: pointerInput creates its gesture
    // callbacks once, so anything they close over by value (like a precomputed target) is
    // frozen at the composition that started the gesture. Reading the remembered state
    // objects through this function keeps the drop honest.
    fun targetOf(state: DragState): Target? {
        if (boardCell <= 0f) return null
        val topLeft = draggedTopLeft(state.piece, state.pointer, boardCell)
        return Target(
            row = ((topLeft.y - boardOrigin.y) / boardCell).roundToInt(),
            col = ((topLeft.x - boardOrigin.x) / boardCell).roundToInt(),
        )
    }

    val dragging = drag
    val target = dragging?.let { targetOf(it) }

    val ghostValid = dragging != null && target != null &&
        vm.canPlace(dragging.index, target.row, target.col)

    // Rows, columns and boxes this drop would finish off, so the board can call it out.
    val completing = if (ghostValid && dragging != null && target != null) {
        vm.previewClears(dragging.index, target.row, target.col)
    } else {
        emptySet()
    }

    // Only preview once the piece actually overlaps the grid.
    val ghostCells = if (
        dragging != null && target != null &&
        target.row + dragging.piece.height > 0 && target.col + dragging.piece.width > 0 &&
        target.row < Board.SIZE && target.col < Board.SIZE
    ) {
        dragging.piece.cells.map { Cell(target.row + it.row, target.col + it.col) }
    } else {
        null
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.screen)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Header(
                score = vm.score,
                best = vm.best,
                streak = vm.streak,
                colors = colors,
                photoUrl = leaderboard.player?.photoUrl,
                onNewGame = ::requestNewGame,
                onLeaderboard = { showLeaderboard = true },
                onSettings = { showSettings = true },
            )

            Spacer(Modifier.height(20.dp))

            BoardCanvas(
                board = vm.board,
                clearing = vm.clearing,
                ghostCells = ghostCells,
                ghostValid = ghostValid,
                completing = completing,
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onGloballyPositioned {
                        boardOrigin = it.positionInRoot()
                        boardCell = it.size.width / Board.SIZE.toFloat()
                    },
            )

            // Leftover height is split rather than dumped below the tray, which floats the
            // tray a little above the bottom edge the way the reference layout does.
            Spacer(Modifier.weight(1.7f))

            Tray(
                tray = vm.tray,
                boardCell = boardCell,
                hiddenIndex = dragging?.index,
                colors = colors,
                isDead = vm::isDead,
                onDragStart = { index, piece, pointer -> drag = DragState(index, piece, pointer) },
                onDrag = { delta -> drag = drag?.let { it.copy(pointer = it.pointer + delta) } },
                onDragEnd = {
                    val current = drag
                    val where = current?.let { targetOf(it) }
                    if (current != null && where != null && vm.canPlace(current.index, where.row, where.col)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.place(current.index, where.row, where.col)
                    }
                    drag = null
                },
                onDragCancel = { drag = null },
            )

            Spacer(Modifier.weight(1f))
        }

        // The piece being dragged, drawn at full board scale so it reads as "picked up".
        if (dragging != null && boardCell > 0f) {
            val topLeft = draggedTopLeft(dragging.piece, dragging.pointer, boardCell)
            PieceCanvas(
                piece = dragging.piece,
                cell = boardCell,
                colors = colors,
                alpha = 0.92f,
                modifier = Modifier.offset {
                    IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt())
                },
            )
        }

        FloatingGain(vm = vm, boardOrigin = boardOrigin, boardCell = boardCell, colors = colors)

        if (vm.gameOver && !showLeaderboard && !showSettings) {
            GameOverOverlay(
                score = vm.score,
                best = vm.best,
                colors = colors,
                onPlayAgain = vm::newGame,
                onLeaderboard = { showLeaderboard = true },
            )
        }

        if (confirmNewGame) {
            ConfirmNewGameOverlay(
                score = vm.score,
                colors = colors,
                onConfirm = {
                    confirmNewGame = false
                    startNewGame()
                },
                onCancel = { confirmNewGame = false },
            )
        }

        if (showLeaderboard) {
            LeaderboardOverlay(
                vm = leaderboard,
                bests = vm.bests,
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
}

@Composable
private fun Header(
    score: Int,
    best: Int,
    streak: Int,
    colors: BoardColors,
    photoUrl: String?,
    onNewGame: () -> Unit,
    onLeaderboard: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Both left-hand buttons open an overlay; NEW acts on the game, so it stays apart.
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountButton(photoUrl = photoUrl, colors = colors, onClick = onLeaderboard)
            SettingsButton(colors = colors, onClick = onSettings)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = score.toString(),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "BEST $best",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted,
                )
                if (streak > 1) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "COMBO ×$streak",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                    )
                }
            }
        }

        TextButton(onClick = onNewGame, modifier = Modifier.align(Alignment.CenterEnd)) {
            Text("NEW", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
        }
    }
}

@Composable
private fun Tray(
    tray: List<Piece?>,
    boardCell: Float,
    hiddenIndex: Int?,
    colors: BoardColors,
    isDead: (Piece) -> Boolean,
    onDragStart: (Int, Piece, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val slotHeight = with(LocalDensity.current) { (boardCell * 3.3f).toDp() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (boardCell > 0f) slotHeight else 110.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        tray.forEachIndexed { index, piece ->
            key(index) {
                var slotOrigin by remember { mutableStateOf(Offset.Zero) }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { slotOrigin = it.positionInRoot() }
                        .then(
                            if (piece != null) {
                                Modifier.pointerInput(piece, index) {
                                    detectDragGestures(
                                        onDragStart = { local -> onDragStart(index, piece, slotOrigin + local) },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            onDrag(amount)
                                        },
                                        onDragEnd = { onDragEnd() },
                                        onDragCancel = { onDragCancel() },
                                    )
                                }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (piece != null && index != hiddenIndex) {
                        // Shrink to fit the slot, but never bigger than a fixed fraction of a
                        // board cell so a 1x1 and a 5x1 stay visually consistent.
                        val cell = min(
                            boardCell * 0.56f,
                            min(
                                constraints.maxWidth.toFloat() / piece.width,
                                constraints.maxHeight.toFloat() / piece.height,
                            ),
                        )
                        PieceCanvas(
                            piece = piece,
                            cell = cell,
                            colors = colors,
                            alpha = if (isDead(piece)) 0.3f else 1f,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceCanvas(
    piece: Piece,
    cell: Float,
    colors: BoardColors,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    if (cell <= 0f) return
    val density = LocalDensity.current
    val width = with(density) { (piece.width * cell).toDp() }
    val height = with(density) { (piece.height * cell).toDp() }
    val tileStyle = LocalTileStyle.current

    Canvas(modifier.size(width, height)) {
        for (c in piece.cells) {
            drawTile(
                left = c.col * cell,
                top = c.row * cell,
                cell = cell,
                fill = colors.tile,
                edge = colors.tileEdge,
                style = tileStyle,
                alpha = alpha,
            )
        }
    }
}

/** The "+N" that floats up off the board after a scoring move. */
@Composable
private fun FloatingGain(
    vm: GameViewModel,
    boardOrigin: Offset,
    boardCell: Float,
    colors: BoardColors,
) {
    val gain = vm.gain ?: return
    if (boardCell <= 0f) return

    key(gain.id) {
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 900)) }

        // Names the bonus rather than leaving the player to work out why the number jumped.
        val shout = Scoring.label(gain.clearedUnits, gain.streak)
        val boardHeight = boardCell * Board.SIZE

        Box(Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (boardOrigin.y + boardHeight * 0.36f - 70f * progress.value).roundToInt(),
                        )
                    }
                    .alpha(1f - progress.value),
            ) {
                Text(
                    text = "+${gain.points}",
                    fontSize = if (gain.clearedUnits > 0) 34.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (gain.clearedUnits > 0) colors.accent else colors.textMuted,
                )
                if (shout != null) {
                    Text(
                        text = shout,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = colors.complete,
                    )
                }
            }
        }
    }
}

/** Guards against throwing away a game in progress with a stray tap on NEW. */
@Composable
private fun ConfirmNewGameOverlay(
    score: Int,
    colors: BoardColors,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .selectable(
                selected = false,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            // Capped so the longer body copy wraps into a card instead of stretching edge to edge.
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.screen,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Start a new game?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "This game ends now. Your score of $score still counts on the leaderboard.",
                    fontSize = 13.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.tile,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("New game", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onCancel) {
                    Text("Keep playing", fontSize = 13.sp, color = colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    best: Int,
    colors: BoardColors,
    onPlayAgain: () -> Unit,
    onLeaderboard: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            // Swallow taps so nothing underneath reacts while the dialog is up.
            .selectable(
                selected = false,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.screen,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No moves left",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = score.toString(),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                )
                Text(
                    text = if (score >= best) "New best!" else "Best $best",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.tile,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Play again", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onLeaderboard) {
                    Text("Leaderboard", fontSize = 13.sp, color = colors.textMuted)
                }
            }
        }
    }
}
