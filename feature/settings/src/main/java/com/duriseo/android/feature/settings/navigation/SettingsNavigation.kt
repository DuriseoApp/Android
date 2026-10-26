package com.duriseo.android.feature.settings.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.duriseo.android.core.common.navigation.MainRouteModel
import com.duriseo.android.feature.settings.SettingsRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) {
    navigate(MainRouteModel.Settings, navOptions)
}

fun NavGraphBuilder.settingsNavGraph(
    padding: PaddingValues,
) {
    composable<MainRouteModel.Settings> {
        SettingsRoute(
            padding = padding,
        )
    }
}
