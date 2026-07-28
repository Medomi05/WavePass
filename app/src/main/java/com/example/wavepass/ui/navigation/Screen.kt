package com.example.wavepass.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Music : Screen(route = "music", label = "Library")
    object NowPlaying : Screen(route = "now_playing", label = "Now Playing")
    object WavePass : Screen(route = "wavepass", label = "WavePass")
    object Profile : Screen(route = "profile", label = "Profile")
}