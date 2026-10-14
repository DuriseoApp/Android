package com.juco.duriseo_android.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.juco.duriseo_android.core.common.navigation.MainRouteModel
import com.juco.duriseo_android.feature.home.navigation.navigateToHome
import com.juco.duriseo_android.core.common.navigation.RouteModel
import com.juco.duriseo_android.feature.main.component.MainMenu
import com.juco.duriseo_android.feature.menu.navigation.navigateToMenu

class MainNavigator(
    val navController: NavHostController
) {
    val startDestination = MainRouteModel.Home
    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination
    val currentMenu: MainMenu?
        @Composable get() = MainMenu.find { menu ->
            currentDestination?.hasRoute(menu::class) == true
        }

    private val singleTopOptions = navOptions {
        launchSingleTop = true
        restoreState = true
    }

    fun navigate(menu: MainMenu) {
        val navOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        when (menu) {
            MainMenu.HOME -> navController.navigateToHome(navOptions)
            MainMenu.MENU -> navController.navigateToMenu(navOptions)
        }
        fun popBackStack() = navController.popBackStack()
        fun popAllBackStack(destination: RouteModel) =
            navController.popBackStack(destination, false)
    }

    @Composable
    fun showBottomBar(): Boolean {
        val isCurrentMenu = MainMenu.contains {
            currentDestination?.hasRoute(it::class) == true
        }
        return isCurrentMenu

    }
}

@Composable
fun rememberMainNavigator(
    navController: NavHostController = rememberNavController(),
): MainNavigator = remember(navController) {
    MainNavigator(navController)
}