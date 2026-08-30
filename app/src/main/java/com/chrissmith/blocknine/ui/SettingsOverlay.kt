package com.chrissmith.blocknine.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chrissmith.blocknine.game.Cell

/**
 * The header's settings control. Drawn rather than taken from an icon pack, matching the
 * hand-drawn person glyph next to it.
 */
@Composable
fun SettingsButton(
    colors: BoardColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.boxShaded)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GearGlyph(colors.textMuted)
    }
}

@Composable
private fun GearGlyph(tint: Color) {
    Canvas(Modifier.size(20.dp)) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        val toothWidth = outer * 0.30f
        val toothLength = outer * 0.60f

        repeat(8) { i ->
            rotate(degrees = i * 45f, pivot = centre) {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(centre.x - toothWidth / 2f, centre.y - outer),
                    size = Size(toothWidth, toothLength),
                    cornerRadius = CornerRadius(toothWidth * 0.35f),
                )
            }
        }

        // A thick stroked circle rather than a filled one, which leaves the hub hole for free.
        val ring = outer * 0.48f
        drawCircle(tint, radius = ring, center = centre, style = Stroke(width = outer * 0.42f))
    }
}

/** Look-and-feel choices: which palette to paint with, and how tiles sit in their cells. */
@Composable
fun SettingsOverlay(
    settings: SettingsViewModel,
    colors: BoardColors,
    onDismiss: () -> Unit,
) {
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
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 360.dp)
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
                    text = "Look & feel",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("THEME", colors)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BoardTheme.entries.forEach { theme ->
                        ThemeSwatch(
                            theme = theme,
                            selected = theme == settings.theme,
                            colors = colors,
                            onClick = { settings.choose(theme) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("BLOCKS", colors)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TileStyle.entries.forEach { style ->
                        TileStyleCard(
                            style = style,
                            selected = style == settings.tileStyle,
                            colors = colors,
                            onClick = { settings.choose(style) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = settings.tileStyle.blurb,
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Done", color = colors.textMuted, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, colors: BoardColors) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = colors.textMuted,
    )
}

/** A theme option, previewed as a four-cell corner of a board painted in its own palette. */
@Composable
private fun ThemeSwatch(
    theme: BoardTheme,
    selected: Boolean,
    colors: BoardColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = theme.palette(isSystemInDarkTheme())
    val tileStyle = LocalTileStyle.current

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(preview.screen)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) colors.accent else colors.gridLine,
                    shape = RoundedCornerShape(10.dp),
                )
        ) {
            val cell = size.height / 2f
            val originX = (size.width - cell * 2f) / 2f

            for (row in 0..1) {
                for (col in 0..1) {
                    drawRect(
                        color = if ((row + col) % 2 == 1) preview.boxShaded else preview.boxPlain,
                        topLeft = Offset(originX + col * cell, row * cell),
                        size = Size(cell, cell),
                    )
                }
            }
            drawTile(originX, 0f, cell, preview.tile, preview.tileEdge, tileStyle)
            drawTile(originX + cell, cell, cell, preview.tile, preview.tileEdge, tileStyle)
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = theme.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.textPrimary else colors.textMuted,
            maxLines = 1,
        )
    }
}

/** A tile-style option, previewed as a small L of tiles drawn the way that style draws them. */
@Composable
private fun TileStyleCard(
    style: TileStyle,
    selected: Boolean,
    colors: BoardColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.tile.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.accent else colors.gridLine,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val shape = listOf(Cell(0, 0), Cell(0, 1), Cell(1, 1), Cell(1, 2))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            val cell = size.height / 2f
            val originX = (size.width - cell * 3f) / 2f
            for (c in shape) {
                drawTile(
                    left = originX + c.col * cell,
                    top = c.row * cell,
                    cell = cell,
                    fill = colors.tile,
                    edge = colors.tileEdge,
                    style = style,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = style.label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.textPrimary else colors.textMuted,
        )
    }
}
