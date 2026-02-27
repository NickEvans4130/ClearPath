package com.clearpath.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearpath.ui.dashboard.StatsScreen
import com.clearpath.ui.download.RegionDownloadScreen
import com.clearpath.ui.education.CameraEducationScreen
import com.clearpath.map.MapScreen

sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object RegionDownload : Screen("region_download")
    data object Stats : Screen("stats")
    data object Education : Screen("education")
}

@Composable
fun ClearPathNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier,
    ) {
        composable(Screen.Map.route) {
            MapScreen(
                onOpenDownload = { navController.navigate(Screen.RegionDownload.route) },
                onOpenStats = { navController.navigate(Screen.Stats.route) },
                onOpenEducation = { navController.navigate(Screen.Education.route) },
            )
        }
        composable(Screen.RegionDownload.route) {
            RegionDownloadScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Education.route) {
            CameraEducationScreen(onBack = { navController.popBackStack() })
        }
    }
}
