package com.chromachaos.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chromachaos.game.ui.screens.GameScreen
import com.chromachaos.game.ui.screens.LeaderboardScreen
import com.chromachaos.game.ui.screens.MainMenuScreen
import com.chromachaos.game.ui.screens.OnboardingScreen
import com.chromachaos.game.ui.screens.SettingsScreen
import com.chromachaos.game.ui.theme.ChromaChaosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChromaChaosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChromaChaosApp()
                }
            }
        }
    }
}

@Composable
fun ChromaChaosApp() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(navController = navController)
        }
        composable("onboarding") {
            OnboardingScreen(navController = navController)
        }
        composable("game") {
            GameScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("leaderboard") {
            LeaderboardScreen(navController = navController)
        }
    }
} 