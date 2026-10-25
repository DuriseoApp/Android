package com.duriseo.android.core.common.navigation

import kotlinx.serialization.Serializable

sealed interface RouteModel {

}

sealed interface MainRouteModel : RouteModel {
    @Serializable
    data object Home : MainRouteModel

    @Serializable
    data object Menu : MainRouteModel
}