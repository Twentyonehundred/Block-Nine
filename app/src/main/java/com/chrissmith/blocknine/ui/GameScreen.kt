package com.chrissmith.blocknine.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import com.chrissmith.blocknine.game.GameMode
import com.chrissmith.blocknine.game.GameViewModel
import com.chrissmith.blocknine.game.Piece
import com.chrissmith.blocknine.game.Scoring
import com.chrissmith.blocknine.leaderboard.LeaderboardViewModel
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

/** Beyond three extra pulses the buzz stops reading as a count and starts reading as a rattle. */
private const val MAX_EXTRA_PULSES = 3

/** Short enough that a quadruple clear finishes buzzing well inside the score animation. */
private const val PULSE_GAP_MS = 55L

/** How big a tray piece is relative to a board cell, before the slot's own limits apply. */
private const val TRAY_PIECE_SCALE = 0.56f

/** Long enough to follow with the eye, short enough not to delay the next move. */
private const val SNAP_BACK_MS = 200

/** Gap between tray slots springing in, so a fresh deal arrives left to right. */
private const val DEAL_STAGGER_MS = 45L

/**
 * How long the board takes to settle after it has been moved for you.
 *
 * Long enough to see which tiles went and how far, short enough that you can't get a piece
 * down during it — a surge or a landslide should read as one event, not a window to play in.
 * Shared by both so a Collapse chain keeps a steady beat as it runs.
 */
private const val BOARD_SLIDE_MS = 260

/** The waterline widget's height, as a fraction of a board cell. */
private const val TIDE_STRIP_CELLS = 0.62f

/** How long the game-over panel takes to get out of the way of the final board. */
private const val PEEK_MS = 160

/** A piece currently under the finger. [pointer] is in root coordinates. */
private data class DragState(val index: Int, val piece: Piece, val pointer: Offset)

/**
 * A piece on its way back to the tray after a drop that didn't take.
 *
 * Carries where it was let go of and where it belongs, so the piece can be seen returning
 * rather than blinking out from under the finger and back into its slot.
 */
private data class SnapBack(
    val id: Long,
    val index: Int,
    val piece: Piece,
    val from: Offset,
    val to: Offset,
    /** The tile size it settles at, which is smaller than the board cell it was dragged at. */
    val cell: Float,
)

/** Where the piece's top-left bounding-box corner sits, given the finger position. */
private data class Target(val row: Int, val col: Int)

/** The tile size a tray piece is drawn at: shrunk to fit its slot, capped so shapes stay even. */
private fun trayCell(piece: Piece, boardCell: Float, slot: Size): Float = min(
    boardCell * TRAY_PIECE_SCALE,
    min(slot.width / piece.width, slot.height / piece.height),
)

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
    onExit: () -> Unit = {},
) {
    val colors = LocalBoardColors.current
    val haptics = LocalHapticFeedback.current
    val isTide = vm.mode == GameMode.RISING_TIDE
    val isCollapse = vm.mode == GameMode.COLLAPSE

    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardCell by remember { mutableFloatStateOf(0f) }
    var drag by remember { mutableStateOf<DragState?>(null) }
    var snapBack by remember { mutableStateOf<SnapBack?>(null) }
    var snapBackCounter by remember { mutableLongStateOf(0L) }
    // Where each tray slot sits on screen, so a returning piece knows what it's aiming at.
    var slotBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmNewGame by remember { mutableStateOf(false) }

    BackHandler(onBack = onExit)

    // A finished game is the only thing worth ranking, so submit exactly once per game over.
    // Every mode has its own board, so every mode submits — to its own.
    LaunchedEffect(vm.gameOver) {
        if (vm.gameOver) leaderboard.onGameFinished(vm.mode, vm.score)
    }

    // Runs the tiles back to where they came from and lets them travel in, so the board reads
    // as having been moved rather than replaced. Serves both the tide's shove and Collapse's
    // landslide; a landslide falls with gathering speed, a shove decelerates into place.
    val boardSlide = remember { Animatable(0f) }
    LaunchedEffect(vm.shiftMoment) {
        if (vm.shiftMoment == null) return@LaunchedEffect
        boardSlide.snapTo(1f)
        boardSlide.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = BOARD_SLIDE_MS,
                easing = if (isCollapse) FastOutLinearInEasing else FastOutSlowInEasing,
            ),
        )
    }

    // One knock as the board lands. Anything it clears buzzes again off the gain below, which
    // is the right order: you feel it move, then you feel what the movement finished.
    LaunchedEffect(vm.shiftMoment) {
        if (vm.shiftMoment != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Every placement gets a tap; a clear gets one extra per unit it took out, so a triple
    // lands differently in the hand from a single without needing to look up at the score.
    LaunchedEffect(vm.gain?.id) {
        val gain = vm.gain ?: return@LaunchedEffect
        repeat(1 + min(gain.clearedUnits, MAX_EXTRA_PULSES)) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(PULSE_GAP_MS)
        }
    }

    // Abandoning a game part-way still counts — otherwise a good run thrown away by tapping
    // NEW would silently never reach the board.
    fun startNewGame() {
        if (!vm.gameOver) leaderboard.onGameFinished(vm.mode, vm.score)
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

    /**
     * The flight home for a piece that has just been dropped somewhere it can't go.
     *
     * Null when the tray hasn't been measured yet, in which case the piece simply disappears
     * as it used to — there's nowhere meaningful to send it.
     */
    fun snapBackFor(state: DragState): SnapBack? {
        if (boardCell <= 0f) return null
        val slot = slotBounds[state.index] ?: return null
        val cell = trayCell(state.piece, boardCell, slot.size)
        return SnapBack(
            id = snapBackCounter++,
            index = state.index,
            piece = state.piece,
            from = draggedTopLeft(state.piece, state.pointer, boardCell),
            to = Offset(
                x = slot.left + (slot.width - state.piece.width * cell) / 2f,
                y = slot.top + (slot.height - state.piece.height * cell) / 2f,
            ),
            cell = cell,
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
                mode = vm.mode,
                score = vm.score,
                best = vm.bestToBeat,
                beatenBest = vm.beatenBest,
                streak = vm.streak,
                colors = colors,
                photoUrl = leaderboard.player?.photoUrl,
                onBack = onExit,
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
                ghostSlot = dragging?.piece?.colorSlot ?: 0,
                completing = completing,
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onGloballyPositioned {
                        boardOrigin = it.positionInRoot()
                        boardCell = it.size.width / Board.SIZE.toFloat()
                    },
                shift = vm.lastShift,
                shiftProgress = { boardSlide.value },
            )

            if (isTide) {
                Spacer(Modifier.height(6.dp))
                TideStrip(
                    wave = vm.pendingWave,
                    progress = { vm.tideProgress },
                    colors = colors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            with(LocalDensity.current) {
                                (boardCell * TIDE_STRIP_CELLS).coerceAtLeast(1f).toDp()
                            }
                        ),
                )
            }

            // Leftover height is split rather than dumped below the tray, which floats the
            // tray a little above the bottom edge the way the reference layout does.
            Spacer(Modifier.weight(1.7f))

            Tray(
                tray = vm.tray,
                boardCell = boardCell,
                // A piece in flight is drawn by the overlay, so its slot must stay empty until
                // it lands — otherwise it appears in two places at once.
                hiddenIndex = dragging?.index ?: snapBack?.index,
                colors = colors,
                isDead = vm::isDead,
                onSlotBounds = { index, bounds ->
                    if (slotBounds[index] != bounds) slotBounds = slotBounds + (index to bounds)
                },
                onDragStart = { index, piece, pointer ->
                    // Picking the piece up again cancels any flight it was already on.
                    if (snapBack?.index == index) snapBack = null
                    drag = DragState(index, piece, pointer)
                },
                onDrag = { delta -> drag = drag?.let { it.copy(pointer = it.pointer + delta) } },
                onDragEnd = {
                    val current = drag
                    val where = current?.let { targetOf(it) }
                    if (current != null && where != null && vm.canPlace(current.index, where.row, where.col)) {
                        // The buzz is fired off the resulting gain, so it can say how big the move was.
                        vm.place(current.index, where.row, where.col)
                    } else if (current != null) {
                        snapBack = snapBackFor(current)
                    }
                    drag = null
                },
                onDragCancel = {
                    drag?.let { snapBack = snapBackFor(it) }
                    drag = null
                },
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

        snapBack?.let { flight ->
            SnapBackPiece(
                flight = flight,
                boardCell = boardCell,
                colors = colors,
                onLanded = { if (snapBack?.id == flight.id) snapBack = null },
            )
        }

        FloatingGain(vm = vm, boardOrigin = boardOrigin, boardCell = boardCell, colors = colors)

        NewBestBanner(
            moment = vm.newBestMoment,
            boardOrigin = boardOrigin,
            boardCell = boardCell,
            colors = colors,
        )

        if (vm.gameOver && !showLeaderboard && !showSettings) {
            GameOverOverlay(
                headline = "No moves left",
                score = vm.score,
                best = vm.bestToBeat,
                colors = colors,
                onPlayAgain = vm::newGame,
                onLeaderboard = { showLeaderboard = true },
                onExit = onExit,
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
                mode = vm.mode,
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
    mode: GameMode,
    score: Int,
    /** The record as it stood when this game started, so there's a fixed target to chase. */
    best: Int,
    beatenBest: Boolean,
    streak: Int,
    colors: BoardColors,
    photoUrl: String?,
    onBack: () -> Unit,
    onNewGame: () -> Unit,
    onLeaderboard: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Back leaves the game; the other two open an overlay. NEW acts on the game itself, so
        // it stays over on the far side, well away from anything that only looks at it.
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(colors = colors, onClick = onBack)
            AccountButton(photoUrl = photoUrl, colors = colors, onClick = onLeaderboard)
            SettingsButton(colors = colors, onClick = onSettings)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Classic goes unlabelled — it's the game, and the header is crowded enough.
            if (mode.isChallenge) {
                Text(
                    text = mode.title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = colors.accent,
                )
            }
            // Counted up rather than snapped, so a big clear registers as an event. A new game
            // snaps instead: watching the score wind back down to zero looks like a bug.
            val shown by animateIntAsState(
                targetValue = score,
                animationSpec = if (score == 0) snap() else tween(durationMillis = 450),
                label = "score",
            )
            Text(
                text = shown.toString(),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (beatenBest) "NEW BEST" else "BEST $best",
                    fontSize = 13.sp,
                    fontWeight = if (beatenBest) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = if (beatenBest) 1.sp else 0.sp,
                    color = if (beatenBest) colors.complete else colors.textMuted,
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

/**
 * Carries a rejected piece back to its slot, shrinking from board scale down to tray scale
 * on the way, so a fumbled drop reads as the piece being put back rather than vanishing.
 */
@Composable
private fun SnapBackPiece(
    flight: SnapBack,
    boardCell: Float,
    colors: BoardColors,
    onLanded: () -> Unit,
) {
    if (boardCell <= 0f) return

    key(flight.id) {
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            progress.animateTo(1f, tween(durationMillis = SNAP_BACK_MS, easing = FastOutSlowInEasing))
            onLanded()
        }

        val t = progress.value
        val x = flight.from.x + (flight.to.x - flight.from.x) * t
        val y = flight.from.y + (flight.to.y - flight.from.y) * t

        PieceCanvas(
            piece = flight.piece,
            cell = boardCell + (flight.cell - boardCell) * t,
            colors = colors,
            alpha = 0.92f,
            modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) },
        )
    }
}

@Composable
private fun Tray(
    tray: List<Piece?>,
    boardCell: Float,
    hiddenIndex: Int?,
    colors: BoardColors,
    isDead: (Piece) -> Boolean,
    onSlotBounds: (Int, Rect) -> Unit,
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
                        .onGloballyPositioned {
                            slotOrigin = it.positionInRoot()
                            onSlotBounds(
                                index,
                                Rect(slotOrigin, Size(it.size.width.toFloat(), it.size.height.toFloat())),
                            )
                        }
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
                    // A slot filling up means a fresh deal, which is worth a beat of animation
                    // so three new shapes don't just materialise. Staggered by slot to read as
                    // a deal rather than a flicker.
                    val entrance = remember { Animatable(0f) }
                    LaunchedEffect(piece) {
                        if (piece == null) {
                            entrance.snapTo(0f)
                        } else {
                            delay(index * DEAL_STAGGER_MS)
                            entrance.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMedium),
                            )
                        }
                    }

                    if (piece != null && index != hiddenIndex) {
                        val cell = trayCell(
                            piece = piece,
                            boardCell = boardCell,
                            slot = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
                        )
                        // The spring overshoots past 1, which is the bounce; alpha reaches full
                        // well before then so the piece isn't still fading while it settles.
                        val grown = entrance.value
                        PieceCanvas(
                            piece = piece,
                            cell = cell,
                            colors = colors,
                            alpha = (if (isDead(piece)) 0.3f else 1f) * (grown / 0.6f).coerceIn(0f, 1f),
                            modifier = Modifier.scale(0.72f + 0.28f * grown),
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
    val painter = rememberTilePainter(colors)
    val fill = painter.fill(piece.colorSlot)
    val edge = painter.edge(piece.colorSlot)

    Canvas(modifier.size(width, height)) {
        for (c in piece.cells) {
            drawTile(
                left = c.col * cell,
                top = c.row * cell,
                cell = cell,
                fill = fill,
                edge = edge,
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
        val shout = Scoring.label(gain.clearedUnits, gain.streak, gain.link)
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

/**
 * A one-off banner for the move that overtakes your record.
 *
 * Sits high on the board, clear of the "+N" that floats up from the middle — the two fire on
 * the same move, and the record falling is the bigger news of the two.
 */
@Composable
private fun NewBestBanner(
    moment: Long?,
    boardOrigin: Offset,
    boardCell: Float,
    colors: BoardColors,
) {
    if (moment == null || boardCell <= 0f) return

    key(moment) {
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 1600)) }

        // Swells in over the first fifth, holds, then fades out over the last third.
        val appear = (progress.value / 0.2f).coerceAtMost(1f)
        val fade = ((progress.value - 0.66f) / 0.34f).coerceIn(0f, 1f)
        val scale = 0.7f + 0.3f * appear

        Box(Modifier.fillMaxSize()) {
            Text(
                text = "NEW BEST!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = colors.complete,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(0, (boardOrigin.y + boardCell * Board.SIZE * 0.12f).roundToInt())
                    }
                    .scale(scale)
                    .alpha(appear * (1f - fade)),
            )
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
                    text = "This game ends now. Your score of $score still counts on the " +
                        "leaderboard.",
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
    /** Why the game ended. */
    headline: String,
    score: Int,
    best: Int,
    colors: BoardColors,
    onPlayAgain: () -> Unit,
    onLeaderboard: () -> Unit,
    onExit: () -> Unit,
) {
    // Tapping the scrim slides the panel out of the way so the board that killed you can
    // actually be read. The tap-catcher stays put either way — the game is over, so nothing
    // underneath should respond to a touch — and tapping again brings the panel back.
    var peeking by remember { mutableStateOf(false) }
    val cover by animateFloatAsState(
        targetValue = if (peeking) 0f else 1f,
        animationSpec = tween(PEEK_MS),
        label = "gameOverCover",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * cover))
            .selectable(
                selected = peeking,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { peeking = !peeking },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // A reminder of where the score went, and of the way back, while the panel is hidden.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .alpha(1f - cover),
            shape = RoundedCornerShape(20.dp),
            color = colors.screen,
            shadowElevation = 8.dp,
        ) {
            Text(
                text = "$headline · $score  —  tap to show",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textMuted,
            )
        }

        if (cover <= 0f) return@Box

        Surface(
            modifier = Modifier.alpha(cover),
            shape = RoundedCornerShape(24.dp),
            color = colors.screen,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = headline,
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
                // best is the record this game set out to beat, so it stays a real target even
                // after the score has passed it.
                val beaten = score > best
                Text(
                    text = if (beaten) "New best!" else "Best $best",
                    fontSize = 14.sp,
                    fontWeight = if (beaten) FontWeight.Bold else FontWeight.Medium,
                    color = if (beaten) colors.complete else colors.textMuted,
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
                TextButton(onClick = onExit) {
                    Text("Menu", fontSize = 13.sp, color = colors.textMuted)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tap outside to see the board",
                    fontSize = 11.sp,
                    color = colors.textMuted.copy(alpha = 0.7f),
                )
            }
        }
    }
}
