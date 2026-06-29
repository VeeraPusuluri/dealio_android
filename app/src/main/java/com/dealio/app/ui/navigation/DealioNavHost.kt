package com.dealio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dealio.app.data.BuilderStore
import com.dealio.app.data.TokenStore
import com.dealio.app.ui.ServerStatusViewModel
import com.dealio.app.ui.builder.BuilderRoot
import com.dealio.app.ui.cp.CpRoot
import com.dealio.app.ui.customer.CustomerRoot
import com.dealio.app.ui.screens.HomeScreen
import com.dealio.app.ui.screens.LoginScreen
import com.dealio.app.ui.screens.ServerDownScreen
import com.dealio.app.ui.screens.SignupScreen
import com.dealio.app.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
}

@Composable
fun DealioNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }

    val serverStatus: ServerStatusViewModel = viewModel()
    val isDown by serverStatus.isDown.collectAsState()
    val checking by serverStatus.checking.collectAsState()

    if (isDown) {
        ServerDownScreen(checking = checking, onRetry = { serverStatus.retry() })
        return
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    val next = if (tokenStore.isLoggedIn) Routes.HOME else Routes.LOGIN
                    navController.navigate(next) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onGoToSignup = {
                    navController.navigate(Routes.SIGNUP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                onSignedUp = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onGoToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            val logout: () -> Unit = {
                tokenStore.clear()
                BuilderStore(context).clear()
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
            val role = tokenStore.user()?.role
            when {
                role.equals("BUILDER", ignoreCase = true) -> BuilderRoot(onLogout = logout)
                role.equals("CUSTOMER", ignoreCase = true) -> CustomerRoot(onLogout = logout)
                role.equals("CP", ignoreCase = true) -> CpRoot(onLogout = logout)
                else -> HomeScreen(onLogout = logout)
            }
        }
    }
}
