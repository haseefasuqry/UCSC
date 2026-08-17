package com.ucsc.codescribe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ucsc.codescribe.ui.AppViewModelFactory
import com.ucsc.codescribe.ui.editor.EditorScreen
import com.ucsc.codescribe.ui.home.HomeScreen
import com.ucsc.codescribe.ui.versions.DiffScreen
import com.ucsc.codescribe.ui.versions.VersionHistoryScreen

private const val ROLLED_BACK_KEY = "codescribe_rolled_back"

@Composable
fun CodeScribeNavHost(
    viewModelFactory: AppViewModelFactory,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModelFactory = viewModelFactory,
                onOpenEditor = { fileId -> navController.navigate(Routes.editor(fileId)) }
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("fileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getLong("fileId") ?: return@composable
            val rolledBack = backStackEntry.savedStateHandle
                .getStateFlow(ROLLED_BACK_KEY, false)
                .collectAsState()
            EditorScreen(
                fileId = fileId,
                viewModelFactory = viewModelFactory,
                rolledBack = rolledBack,
                onConsumeRollback = { backStackEntry.savedStateHandle[ROLLED_BACK_KEY] = false },
                onBack = { navController.popBackStack() },
                onOpenVersions = { id -> navController.navigate(Routes.versions(id)) }
            )
        }
        composable(
            route = Routes.VERSIONS,
            arguments = listOf(navArgument("fileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getLong("fileId") ?: return@composable
            VersionHistoryScreen(
                fileId = fileId,
                viewModelFactory = viewModelFactory,
                onBack = { navController.popBackStack() },
                onViewDiff = { id, old, new -> navController.navigate(Routes.diff(id, old, new)) },
                onRollbackComplete = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(ROLLED_BACK_KEY, true)
                }
            )
        }
        composable(
            route = Routes.DIFF,
            arguments = listOf(
                navArgument("fileId") { type = NavType.LongType },
                navArgument("oldVersion") { type = NavType.IntType },
                navArgument("newVersion") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments ?: return@composable
            DiffScreen(
                fileId = args.getLong("fileId"),
                oldVersion = args.getInt("oldVersion"),
                newVersion = args.getInt("newVersion"),
                viewModelFactory = viewModelFactory,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
