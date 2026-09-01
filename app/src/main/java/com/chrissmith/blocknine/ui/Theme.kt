package com.chrissmith.blocknine.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.chrissmith.blocknine.game.Pieces

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
    /**
     * The fills the multicolour option cycles through, indexed by a piece's colour slot.
     * Tuned per theme so the set still belongs on that sheet; [tile] is always the first.
     */
    val tilePalette: List<Color>,
    /**
     * Blocks the Rising Tide pushes in. Deliberately drab next to every piece colour, so a
     * glance at the board tells you what you put there and what the water did.
     */
    val tideTile: Color,
    val tideEdge: Color,
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

/** Whether every block shares the theme's colour or each shape brings its own. Cosmetic only. */
enum class TileColour(val label: String, val blurb: String) {
    VARIED("Multicolour", "Every shape has its own colour, like the original."),
    SINGLE("One colour", "Every block is painted in the theme's single block colour."),
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
    tilePalette = listOf(
        Color(0xFF2F7DF6), // blue
        Color(0xFFEF5A5A), // coral
        Color(0xFF23A55A), // green
        Color(0xFFF2A31B), // amber
        Color(0xFF9B5DE5), // violet
        Color(0xFF10B4C4), // teal
    ),
    tideTile = Color(0xFF8FA6BC),
    tideEdge = Color(0xFF5A7089),
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
    tilePalette = listOf(
        Color(0xFFE8833A), // orange
        Color(0xFFC94F3D), // brick
        Color(0xFF6E9A45), // olive
        Color(0xFFD9A62E), // mustard
        Color(0xFF8A6BAE), // mauve
        Color(0xFF3F8C8C), // slate teal
    ),
    tideTile = Color(0xFFB3A491),
    tideEdge = Color(0xFF80705C),
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
    tilePalette = listOf(
        Color(0xFF3B82F6), // blue
        Color(0xFFF87171), // rose
        Color(0xFF34D399), // mint
        Color(0xFFFBBF24), // amber
        Color(0xFFA78BFA), // lilac
        Color(0xFF22D3EE), // cyan
    ),
    tideTile = Color(0xFF52657F),
    tideEdge = Color(0xFF33445C),
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

val LocalTileColour = staticCompositionLocalOf { TileColour.VARIED }

/**
 * Turns a piece's colour slot into a fill and an outline.
 *
 * Exists so that every place which draws a tile can ask the same question and stay ignorant
 * of whether the multicolour option is on. Build one per canvas with [rememberTilePainter].
 */
@Immutable
class TilePainter(private val colors: BoardColors, private val varied: Boolean) {

    fun fill(slot: Int): Color = when {
        // Slots past the piece palette belong to the tide, which stays drab whatever the
        // colour setting says — it isn't the player's block and shouldn't look like one.
        slot >= Pieces.COLOUR_SLOTS -> colors.tideTile
        varied -> colors.tilePalette[slot.mod(colors.tilePalette.size)]
        else -> colors.tile
    }

    /** The drop preview's colour: the shape's own when multicoloured, else the theme's ghost. */
    fun ghost(slot: Int): Color = if (varied) fill(slot) else colors.ghost

    /**
     * A darker relative of the fill, so a multicoloured tile is outlined in its own colour
     * rather than the one edge tone that only suits [BoardColors.tile].
     */
    fun edge(slot: Int): Color = when {
        slot >= Pieces.COLOUR_SLOTS -> colors.tideEdge
        !varied -> colors.tileEdge
        else -> {
            val fill = fill(slot)
            Color(fill.red * EDGE_SHADE, fill.green * EDGE_SHADE, fill.blue * EDGE_SHADE, fill.alpha)
        }
    }

    private companion object {
        const val EDGE_SHADE = 0.58f
    }
}

@Composable
fun rememberTilePainter(colors: BoardColors = LocalBoardColors.current): TilePainter {
    val varied = LocalTileColour.current == TileColour.VARIED
    return remember(colors, varied) { TilePainter(colors, varied) }
}

@Composable
fun BlockNineTheme(
    theme: BoardTheme = BoardTheme.SYSTEM,
    tileStyle: TileStyle = TileStyle.ROUNDED,
    tileColour: TileColour = TileColour.VARIED,
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
        LocalTileColour provides tileColour,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
