package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.ProblemRepository
import com.example.ui.LeetCodeViewModel
import com.example.ui.LeetCodeViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize local Room database
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ProblemRepository(database.problemDao())

        // 2. Initialize ViewModel via factory
        val viewModel = ViewModelProvider(
            this,
            LeetCodeViewModelFactory(repository)
        )[LeetCodeViewModel::class.java]

        setContent {
            LeetCodeCompanionTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                // Hide bottom bar on AI hint screen for an expansive coach workspace.
                val showBottomBar = currentRoute != "ai_hint"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = CardBackground,
                                contentColor = TextPrimary
                            ) {
                                val navTabs = listOf(
                                    NavTab("Home", "home", Icons.Default.Home),
                                    NavTab("Log", "log", Icons.Default.AddCircleOutline),
                                    NavTab("History", "history", Icons.Default.Assignment),
                                    NavTab("Stats", "stats", Icons.Default.BarChart)
                                )

                                navTabs.forEach { tab ->
                                    val isSelected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != tab.route) {
                                                navController.navigate(tab.route) {
                                                    popUpTo("home") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.label,
                                                tint = if (isSelected) PrimaryPurple else TextSecondary
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                color = if (isSelected) PrimaryPurple else TextSecondary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = PrimaryPurple.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                        }
                    },
                    containerColor = BackgroundDark
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToLog = { navController.navigate("log") },
                                onNavigateToAIHint = { navController.navigate("ai_hint") }
                            )
                        }

                        composable("log") {
                            LogProblemScreen(
                                viewModel = viewModel,
                                onNavigateBackOrHome = {
                                    // Always pop or navigate to home safely
                                    if (!navController.popBackStack()) {
                                        navController.navigate("home")
                                    }
                                }
                            )
                        }

                        composable("history") {
                            HistoryScreen(
                                viewModel = viewModel
                            )
                        }

                        composable("stats") {
                            StatsScreen(
                                viewModel = viewModel
                            )
                        }

                        composable("ai_hint") {
                            AIHintScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate("home")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavTab(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
