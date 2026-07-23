package com.juco.feature.main

import android.content.Context
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.juco.core.designsystem.theme.JucoTheme
import com.juco.feature.main.component.BottomNavigationBar
import com.juco.feature.main.navigation.MainNavHost
import com.juco.feature.main.navigation.MainNavigator
import com.juco.feature.main.navigation.rememberMainNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    navigator: MainNavigator = rememberMainNavigator(),
    context: Context = LocalContext.current
) {
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    val snackBarHostState = remember { SnackbarHostState() }

    var snackBarJob by remember { mutableStateOf<Job?>(null) }
    val onShowSnackBar: (String) -> Unit = { msg ->
        snackBarJob?.cancel()
        snackBarJob = lifecycleScope.launch {
            snackBarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = JucoTheme.colors.primaryBackground,
        snackbarHost = { SnackbarHost(snackBarHostState) },
        content = { paddingValues ->
            MainNavHost(
                navigator = navigator,
                onShowSnackBar = onShowSnackBar,
                padding = paddingValues,
                context = context,
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentMenu = navigator.currentMenu,
                onMenuSelected = { navigator.navigate(it) },
                isVisible = navigator.showBottomBar()
            )
        }

    )

}