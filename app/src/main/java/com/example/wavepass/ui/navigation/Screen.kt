package com.example.wavepass.ui.navigation

// Represents each top-level destination reachable from the bottom bar.
// Using a sealed class keeps routes type-safe instead of relying on raw strings everywhere.
sealed class Screen(val route: String, val label: String) {
    object Music : Screen(route = "music", label = "Music")
    object WavePass : Screen(route = "wavepass", label = "WavePass")
    object Profile : Screen(route = "profile", label = "Profile")
}