package com.chrissmith.blocknine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrissmith.blocknine.ui.BlockNineApp
import com.chrissmith.blocknine.ui.BlockNineTheme
import com.chrissmith.blocknine.ui.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Hoisted above the theme rather than left inside the screens: the palette and the
            // tile style are what BlockNineTheme provides, so it has to be told them here.
            val settings: SettingsViewModel = viewModel()
            BlockNineTheme(
                theme = settings.theme,
                tileStyle = settings.tileStyle,
                tileColour = settings.tileColour,
            ) {
                BlockNineApp(settings = settings)
            }
        }
    }
}
