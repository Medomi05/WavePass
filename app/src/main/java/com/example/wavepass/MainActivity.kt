package com.example.wavepass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.wavepass.ui.navigation.MainScaffold
import com.example.wavepass.ui.theme.WavePassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WavePassTheme {
                MainScaffold()
            }
        }
    }
}