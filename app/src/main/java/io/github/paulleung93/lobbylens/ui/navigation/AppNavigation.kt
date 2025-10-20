package io.github.paulleung93.lobbylens.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.paulleung93.lobbylens.ui.details.DetailsScreen
import io.github.paulleung93.lobbylens.ui.editor.EditorScreen
import io.github.paulleung93.lobbylens.ui.home.HomeScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable(
            route = "editor?imageUri={imageUri}",
            arguments = listOf(navArgument("imageUri") {
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            EditorScreen(
                navController = navController,
                imageUri = backStackEntry.arguments?.getString("imageUri")
            )
        }
        composable(
            route = "details/{cid}",
            arguments = listOf(navArgument("cid") { type = NavType.StringType })
        ) { backStackEntry ->
            DetailsScreen(
                navController = navController,
                cid = backStackEntry.arguments?.getString("cid")
            )
        }
    }
}
