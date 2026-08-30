package com.chrissmith.blocknine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chrissmith.blocknine.ui.BlockNineTheme
import com.chrissmith.blocknine.ui.GameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockNineTheme {
                GameScreen()
            }
        }
    }
}
