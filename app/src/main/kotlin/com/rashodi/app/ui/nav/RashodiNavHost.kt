package com.rashodi.app.ui.nav

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rashodi.app.ui.analytics.AnalyticsScreen
import com.rashodi.app.ui.calculator.DistributionScreen
import com.rashodi.app.ui.categories.CategoriesScreen
import com.rashodi.app.ui.dashboard.DashboardScreen
import com.rashodi.app.ui.funds.FundsScreen
import com.rashodi.app.ui.importexport.ImportExportScreen
import com.rashodi.app.ui.more.MoreScreen
import com.rashodi.app.ui.operations.AddEditOperationScreen
import com.rashodi.app.ui.operations.OperationsScreen
import com.rashodi.app.ui.planfact.PlanFactScreen
import com.rashodi.app.ui.settings.SettingsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val OPERATIONS = "operations"
    const val ANALYTICS = "analytics"
    const val MORE = "more"
    const val CATEGORIES = "categories"
    const val PLAN_FACT = "planfact"
    const val FUNDS = "funds"
    const val CALCULATOR = "calculator"
    const val IMPORT_EXPORT = "importexport"
    const val SETTINGS = "settings"
    const val OPERATION_EDIT = "operation"

    fun edit(kind: String, id: Long) = "$OPERATION_EDIT?kind=$kind&id=$id"
    fun add() = "$OPERATION_EDIT?kind=expense&id=-1"
}

private data class TopDest(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDest(Routes.DASHBOARD, "Дашборд", Icons.Rounded.GridView),
    TopDest(Routes.OPERATIONS, "Операции", Icons.AutoMirrored.Rounded.ReceiptLong),
    TopDest(Routes.ANALYTICS, "Аналитика", Icons.Rounded.BarChart),
    TopDest(Routes.MORE, "Ещё", Icons.Rounded.Menu),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RashodiNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isTopLevel = topDestinations.any { it.route == currentRoute }
    val showFab = currentRoute == Routes.DASHBOARD || currentRoute == Routes.OPERATIONS ||
        currentRoute == Routes.ANALYTICS

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    topDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                if (currentRoute != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo(Routes.DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = { navController.navigate(Routes.add()) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Добавить операцию")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = androidx.compose.ui.Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            appGraph(navController)
        }
    }
}

private fun NavGraphBuilder.appGraph(navController: NavHostController) {
    composable(Routes.DASHBOARD) {
        DashboardScreen(
            onOpenLeaks = { navController.navigate(Routes.ANALYTICS) },
            onAdd = { navController.navigate(Routes.add()) },
        )
    }
    composable(Routes.OPERATIONS) {
        OperationsScreen(
            onEdit = { kind, id -> navController.navigate(Routes.edit(kind, id)) },
        )
    }
    composable(Routes.ANALYTICS) { AnalyticsScreen() }
    composable(Routes.MORE) {
        MoreScreen(onNavigate = { route -> navController.navigate(route) })
    }
    composable(Routes.CATEGORIES) { CategoriesScreen(onBack = { navController.popBackStack() }) }
    composable(Routes.PLAN_FACT) { PlanFactScreen(onBack = { navController.popBackStack() }) }
    composable(Routes.FUNDS) { FundsScreen(onBack = { navController.popBackStack() }) }
    composable(Routes.CALCULATOR) { DistributionScreen(onBack = { navController.popBackStack() }) }
    composable(Routes.IMPORT_EXPORT) { ImportExportScreen(onBack = { navController.popBackStack() }) }
    composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }

    composable(
        route = "${Routes.OPERATION_EDIT}?kind={kind}&id={id}",
        arguments = listOf(
            navArgument("kind") { type = NavType.StringType; defaultValue = "expense" },
            navArgument("id") { type = NavType.LongType; defaultValue = -1L },
        ),
    ) { entry ->
        AddEditOperationScreen(
            initialKind = entry.arguments?.getString("kind") ?: "expense",
            id = entry.arguments?.getLong("id") ?: -1L,
            onDone = { navController.popBackStack() },
        )
    }
}
