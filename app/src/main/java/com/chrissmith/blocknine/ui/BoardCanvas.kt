package com.chrissmith.blocknine.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.chrissmith.blocknine.game.Board
import com.chrissmith.blocknine.game.Cell

/**
 * Draws one tile within the cell whose top-left is ([left], [top]).
 *
 * [style] decides whether the tile is inset with rounded corners or fills the cell as a
 * square, in which case neighbouring tiles touch and their outlines merge into one seam.
 * [shrink] pulls the tile in towards its own centre (used by the clear animation) and
 * [alpha] scales the whole thing's opacity (used by both the clear animation and ghosts).
 */
fun DrawScope.drawTile(
    left: Float,
    top: Float,
    cell: Float,
    fill: Color,
    edge: Color?,
    style: TileStyle = TileStyle.ROUNDED,
    alpha: Float = 1f,
    shrink: Float = 0f,
) {
    val solid = style == TileStyle.SOLID
    val inset = if (solid) 0f else cell * 0.05f
    val pull = cell * 0.5f * shrink
    val side = cell - inset * 2f - pull * 2f
    if (side <= 0f || alpha <= 0f) return

    val topLeft = Offset(left + inset + pull, top + inset + pull)
    val boxSize = Size(side, side)
    val radius = if (solid) CornerRadius.Zero else CornerRadius(cell * 0.15f)

    drawRoundRect(fill.copy(alpha = fill.alpha * alpha), topLeft, boxSize, radius)
    if (edge != null) {
        drawRoundRect(
            color = edge.copy(alpha = edge.alpha * alpha),
            topLeft = topLeft,
            size = boxSize,
            cornerRadius = radius,
            style = Stroke(width = cell * 0.05f),
        )
    }
}

/**
 * The 9x9 grid: alternating box shading, hairline cell lines, heavy box borders, the
 * placed tiles, the drop preview, and the flash-and-shrink animation for cleared cells.
 *
 * [completing] holds the cells the pending drop would clear, highlighted so the player can
 * see a scoring move coming rather than discovering it after the fact.
 */
@Composable
fun BoardCanvas(
    board: Board,
    clearing: Set<Int>,
    ghostCells: List<Cell>?,
    ghostValid: Boolean,
    completing: Set<Int>,
    colors: BoardColors,
    modifier: Modifier = Modifier,
) {
    // Runs 0 -> 1 whenever a new set of cells starts clearing.
    val flash = remember { Animatable(0f) }
    LaunchedEffect(clearing) {
        if (clearing.isEmpty()) {
            flash.snapTo(0f)
        } else {
            flash.snapTo(0f)
            flash.animateTo(1f, tween(durationMillis = 190, easing = LinearEasing))
        }
    }

    val tileStyle = LocalTileStyle.current

    Canvas(modifier) {
        val cell = size.width / Board.SIZE
        val hairline = (cell * 0.02f).coerceAtLeast(1f)
        val boxLine = (cell * 0.045f).coerceAtLeast(2f)

        // Alternating 3x3 box backgrounds, matching the sudoku-style checker in the reference.
        for (box in 0 until Board.SIZE) {
            val boxRow = box / Board.BOX
            val boxCol = box % Board.BOX
            val shaded = (boxRow + boxCol) % 2 == 1
            drawRect(
                color = if (shaded) colors.boxShaded else colors.boxPlain,
                topLeft = Offset(boxCol * Board.BOX * cell, boxRow * Board.BOX * cell),
                size = Size(Board.BOX * cell, Board.BOX * cell),
            )
        }

        // A wash over everything the pending drop would clear, sitting under the grid lines so
        // the board still reads as a board.
        for (index in completing) {
            drawRect(
                color = colors.complete.copy(alpha = 0.18f),
                topLeft = Offset((index % Board.SIZE) * cell, (index / Board.SIZE) * cell),
                size = Size(cell, cell),
            )
        }

        // Hairline grid between individual cells.
        for (i in 1 until Board.SIZE) {
            drawLine(colors.gridLine, Offset(i * cell, 0f), Offset(i * cell, size.height), hairline)
            drawLine(colors.gridLine, Offset(0f, i * cell), Offset(size.width, i * cell), hairline)
        }

        // Heavier lines around each 3x3 box, inset at the edges so they aren't half-clipped.
        for (i in 0..Board.BOX) {
            val pos = (i * Board.BOX * cell).coerceIn(boxLine / 2f, size.width - boxLine / 2f)
            drawLine(colors.boxBorder, Offset(pos, 0f), Offset(pos, size.height), boxLine)
            drawLine(colors.boxBorder, Offset(0f, pos), Offset(size.width, pos), boxLine)
        }

        // Drop preview underneath the real tiles.
        if (ghostCells != null) {
            val ghostColor = if (ghostValid) colors.ghost else colors.invalid
            for (c in ghostCells) {
                if (c.row !in 0 until Board.SIZE || c.col !in 0 until Board.SIZE) continue
                drawTile(
                    left = c.col * cell,
                    top = c.row * cell,
                    cell = cell,
                    fill = ghostColor,
                    edge = null,
                    style = tileStyle,
                    alpha = if (ghostValid) 0.38f else 0.28f,
                )
            }
        }

        // Placed tiles. Cells that are mid-clear flash white and shrink away.
        for (row in 0 until Board.SIZE) {
            for (col in 0 until Board.SIZE) {
                if (!board.isFilled(row, col)) continue
                val isClearing = (row * Board.SIZE + col) in clearing
                val progress = if (isClearing) flash.value else 0f
                drawTile(
                    left = col * cell,
                    top = row * cell,
                    cell = cell,
                    fill = if (progress > 0f) lerp(colors.tile, Color.White, progress) else colors.tile,
                    edge = if (progress > 0f) null else colors.tileEdge,
                    style = tileStyle,
                    alpha = 1f - progress,
                    shrink = progress * 0.45f,
                )
            }
        }

        // Outline around the completing region, on top of the tiles — the wash alone is
        // invisible under a nearly full row, which is exactly when it matters most.
        if (completing.isNotEmpty()) {
            val edge = (cell * 0.06f).coerceAtLeast(2f)
            // Sides that lie on the board's own border would be half-clipped, so nudge them in.
            fun clampX(v: Float) = v.coerceIn(edge / 2f, size.width - edge / 2f)
            fun clampY(v: Float) = v.coerceIn(edge / 2f, size.height - edge / 2f)
            fun outside(r: Int, c: Int) = r !in 0 until Board.SIZE || c !in 0 until Board.SIZE ||
                (r * Board.SIZE + c) !in completing

            for (index in completing) {
                val row = index / Board.SIZE
                val col = index % Board.SIZE
                val left = clampX(col * cell)
                val right = clampX((col + 1) * cell)
                val top = clampY(row * cell)
                val bottom = clampY((row + 1) * cell)

                if (outside(row - 1, col)) {
                    drawLine(colors.complete, Offset(left, top), Offset(right, top), edge)
                }
                if (outside(row + 1, col)) {
                    drawLine(colors.complete, Offset(left, bottom), Offset(right, bottom), edge)
                }
                if (outside(row, col - 1)) {
                    drawLine(colors.complete, Offset(left, top), Offset(left, bottom), edge)
                }
                if (outside(row, col + 1)) {
                    drawLine(colors.complete, Offset(right, top), Offset(right, bottom), edge)
                }
            }
        }
    }
}
