package com.juco.feature.home.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.juco.core.common.navigation.MainRouteModel
import com.juco.feature.home.HomeRoute

fun NavController.navigateToHome(navOptions: NavOptions) {
    navigate(MainRouteModel.Home, navOptions)
}

fun NavGraphBuilder.homeNavGraph(
    padding: PaddingValues,
) {
    composable<MainRouteModel.Home> { backStackEntry ->
        HomeRoute(
            padding = padding
        )
    }
}