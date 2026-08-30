package com.chrissmith.blocknine.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Board palette. Held separately from the Material scheme because the grid needs a few
 * specific roles (shaded vs plain 3x3 boxes, hairline grid vs heavy box borders) that
 * don't map onto Material's slots.
 */
@Immutable
data class BoardColors(
    val screen: Color,
    val boxShaded: Color,
    val boxPlain: Color,
    val gridLine: Color,
    val boxBorder: Color,
    val tile: Color,
    val tileEdge: Color,
    val ghost: Color,
    val invalid: Color,
    /** Marks a row, column or box the pending drop would complete. Reads against [invalid]. */
    val complete: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val accent: Color,
)

/** How a tile is drawn in its cell. Purely cosmetic — the grid and the rules don't change. */
enum class TileStyle(val label: String, val blurb: String) {
    ROUNDED("Rounded", "Tiles sit inside their cell with a gap between them."),
    SOLID("Touching", "Tiles fill the cell edge to edge, outlined like the original."),
}

/** The palettes offered in settings. [SYSTEM] follows the device's light/dark setting. */
enum class BoardTheme(val label: String) {
    SYSTEM("System"),
    CLASSIC("Classic"),
    WARM("Warm"),
    DARK("Dark"),
}

/**
 * Modelled on the reference screenshot: a white sheet, cool grey-blue shading on alternate
 * boxes, dark slate rules, and a plain blue tile outlined in near-charcoal so that touching
 * tiles read as separate squares.
 */
private val ClassicBoard = BoardColors(
    screen = Color(0xFFFFFFFF),
    boxShaded = Color(0xFFE8EDF4),
    boxPlain = Color(0xFFFFFFFF),
    gridLine = Color(0xFFD7DEE8),
    boxBorder = Color(0xFF39414D),
    tile = Color(0xFF2F7DF6),
    tileEdge = Color(0xFF2A3A4F),
    ghost = Color(0xFF2F7DF6),
    invalid = Color(0xFFEF4444),
    complete = Color(0xFF16A34A),
    textPrimary = Color(0xFF1A202C),
    textMuted = Color(0xFF6B7280),
    accent = Color(0xFF2F7DF6),
)

/** A softer light mode: paper rather than white, amber tiles instead of blue. */
private val WarmBoard = BoardColors(
    screen = Color(0xFFFBF7F0),
    boxShaded = Color(0xFFF0E7D8),
    boxPlain = Color(0xFFFDFBF6),
    gridLine = Color(0xFFE2D6C2),
    boxBorder = Color(0xFF8A7A62),
    tile = Color(0xFFE8833A),
    tileEdge = Color(0xFFB85F1C),
    ghost = Color(0xFFE8833A),
    invalid = Color(0xFFD94F4F),
    complete = Color(0xFF4F8F3A),
    textPrimary = Color(0xFF3A322A),
    textMuted = Color(0xFF8A7F70),
    accent = Color(0xFFD9702A),
)

private val DarkBoard = BoardColors(
    screen = Color(0xFF0B1220),
    boxShaded = Color(0xFF16233A),
    boxPlain = Color(0xFF101A2C),
    gridLine = Color(0xFF22304A),
    boxBorder = Color(0xFF5A6E8C),
    tile = Color(0xFF3B82F6),
    tileEdge = Color(0xFF1D4ED8),
    ghost = Color(0xFF60A5FA),
    invalid = Color(0xFFEF4444),
    complete = Color(0xFF22C55E),
    textPrimary = Color(0xFFE8EEF7),
    textMuted = Color(0xFF93A4BC),
    accent = Color(0xFF60A5FA),
)

/** The palette this theme paints with. [systemDark] only matters for [BoardTheme.SYSTEM]. */
fun BoardTheme.palette(systemDark: Boolean): BoardColors = when (this) {
    BoardTheme.SYSTEM -> if (systemDark) DarkBoard else ClassicBoard
    BoardTheme.CLASSIC -> ClassicBoard
    BoardTheme.WARM -> WarmBoard
    BoardTheme.DARK -> DarkBoard
}

/** True when this theme paints a dark sheet, so Material can be told which scheme to build. */
fun BoardTheme.isDark(systemDark: Boolean): Boolean = when (this) {
    BoardTheme.SYSTEM -> systemDark
    BoardTheme.DARK -> true
    else -> false
}

val LocalBoardColors = staticCompositionLocalOf { ClassicBoard }

/** Read by the two canvases that draw tiles, so the choice doesn't have to be threaded by hand. */
val LocalTileStyle = staticCompositionLocalOf { TileStyle.ROUNDED }

@Composable
fun BlockNineTheme(
    theme: BoardTheme = BoardTheme.SYSTEM,
    tileStyle: TileStyle = TileStyle.ROUNDED,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val board = theme.palette(systemDark)
    val scheme = if (theme.isDark(systemDark)) {
        darkColorScheme(
            primary = board.accent,
            background = board.screen,
            surface = board.screen,
            onBackground = board.textPrimary,
            onSurface = board.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = board.accent,
            background = board.screen,
            surface = board.screen,
            onBackground = board.textPrimary,
            onSurface = board.textPrimary,
        )
    }

    CompositionLocalProvider(
        LocalBoardColors provides board,
        LocalTileStyle provides tileStyle,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
