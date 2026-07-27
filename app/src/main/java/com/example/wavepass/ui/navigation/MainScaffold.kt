package com.example.wavepass.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.wavepass.ui.music.MusicScreen
import com.example.wavepass.ui.profile.ProfileScreen
import com.example.wavepass.ui.wavepass.WavePassScreen

// List of tabs shown in the bottom bar, in display order.
private val bottomBarScreens = listOf(Screen.Music, Screen.WavePass, Screen.Profile)

@Composable
fun MainScaffold() {
    val navController: NavHostController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                // Observes the current route so we can highlight the active tab.
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                bottomBarScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Avoids piling up multiple copies of the same
                                // destination on the back stack when tapping tabs repeatedly.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = screen.icon(), contentDescription = screen.label) },
                        label = { androidx.compose.material3.Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Music.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Music.route) { MusicScreen() }
            composable(Screen.WavePass.route) { WavePassScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }
}

// Maps each screen to its bottom bar icon.
private fun Screen.icon() = when (this) {
    Screen.Music -> Icons.Filled.PlayArrow
    Screen.WavePass -> Icons.Filled.Sensors
    Screen.Profile -> Icons.Filled.Person
}