package com.chrissmith.blocknine.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/** The look-and-feel choices, remembered on the device. */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var theme by mutableStateOf(prefs.enum(KEY_THEME, BoardTheme.SYSTEM))
        private set

    var tileStyle by mutableStateOf(prefs.enum(KEY_TILE_STYLE, TileStyle.ROUNDED))
        private set

    var tileColour by mutableStateOf(prefs.enum(KEY_TILE_COLOUR, TileColour.VARIED))
        private set

    fun choose(theme: BoardTheme) {
        this.theme = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun choose(style: TileStyle) {
        tileStyle = style
        prefs.edit().putString(KEY_TILE_STYLE, style.name).apply()
    }

    fun choose(colour: TileColour) {
        tileColour = colour
        prefs.edit().putString(KEY_TILE_COLOUR, colour.name).apply()
    }

    private companion object {
        const val PREFS = "block_nine"
        const val KEY_THEME = "board_theme"
        const val KEY_TILE_STYLE = "tile_style"
        const val KEY_TILE_COLOUR = "tile_colour"

        /**
         * Enums are stored by name so the file stays readable, but a name that no longer
         * exists (an option renamed in a later build) must not crash the app on launch.
         */
        inline fun <reified T : Enum<T>> SharedPreferences.enum(key: String, fallback: T): T {
            val stored = getString(key, null) ?: return fallback
            return enumValues<T>().firstOrNull { it.name == stored } ?: fallback
        }
    }
}
