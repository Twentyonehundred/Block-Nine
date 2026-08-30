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
    val textPrimary: Color,
    val textMuted: Color,
    val accent: Color,
)

private val LightBoard = BoardColors(
    screen = Color(0xFFFFFFFF),
    boxShaded = Color(0xFFE4EAF2),
    boxPlain = Color(0xFFFFFFFF),
    gridLine = Color(0xFFC9D6E5),
    boxBorder = Color(0xFF4A5568),
    tile = Color(0xFF3B82F6),
    tileEdge = Color(0xFF2563EB),
    ghost = Color(0xFF3B82F6),
    invalid = Color(0xFFEF4444),
    textPrimary = Color(0xFF1A202C),
    textMuted = Color(0xFF718096),
    accent = Color(0xFF2F6FEB),
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
    textPrimary = Color(0xFFE8EEF7),
    textMuted = Color(0xFF93A4BC),
    accent = Color(0xFF60A5FA),
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

@Composable
fun BlockNineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val board = if (darkTheme) DarkBoard else LightBoard
    val scheme = if (darkTheme) {
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

    CompositionLocalProvider(LocalBoardColors provides board) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
