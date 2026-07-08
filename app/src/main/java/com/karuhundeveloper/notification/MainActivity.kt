package com.karuhundeveloper.notification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.karuhundeveloper.notification.ui.screen.AddRuleScreen
import com.karuhundeveloper.notification.ui.screen.MainScreen
import com.karuhundeveloper.notification.ui.theme.NotificationInterceptorTheme

// ── Navigation destinations ───────────────────────────────────────────────────

private object Routes {
    const val MAIN     = "main"
    const val ADD_RULE = "add_rule"
}

// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotificationInterceptorTheme {
                AppNavGraph()
            }
        }
    }
}

@Composable
private fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onAddRuleClick = { navController.navigate(Routes.ADD_RULE) }
            )
        }
        composable(Routes.ADD_RULE) {
            AddRuleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
