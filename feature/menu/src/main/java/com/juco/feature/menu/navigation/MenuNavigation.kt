package com.juco.feature.menu.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.juco.core.common.navigation.MainRouteModel
import com.juco.feature.menu.MenuRoute

fun NavController.navigateToMenu(navOptions: NavOptions) {
    navigate(MainRouteModel.Menu, navOptions)
}

fun NavGraphBuilder.menuNavGraph(
    padding: PaddingValues,
) {
    composable<MainRouteModel.Menu> { backStackEntry ->
        MenuRoute(
            padding = padding
        )
    }
}