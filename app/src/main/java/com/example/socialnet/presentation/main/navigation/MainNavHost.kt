package com.example.socialnet.presentation.main.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.socialnet.presentation.auth.LoginRoute
import com.example.socialnet.presentation.auth.RegisterRoute
import com.example.socialnet.presentation.auth.ResetPasswordRoute
import com.example.socialnet.presentation.feed.FeedRoute
import com.example.socialnet.presentation.main.components.MainBottomBar
import com.example.socialnet.presentation.post.SavePostRoute
import com.example.socialnet.presentation.profile.OtherProfileRoute
import com.example.socialnet.presentation.profile.ProfileRoute

@Composable
fun MainNavHost(role: Boolean) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Реакция на смену роли: login → убираем auth из стека, logout с SavePost → на Feed
    LaunchedEffect(role) {
        val route = navController.currentBackStackEntry?.destination?.route ?: return@LaunchedEffect
        when {
            role && route.startsWith("auth/") -> {
                navController.popBackStack(MainRoutes.FEED, inclusive = false)
            }
            !role && route.startsWith("save_post") -> {
                navController.popBackStack(MainRoutes.FEED, inclusive = false)
            }
        }
    }

    // На auth-экранах bottom bar скрываем
    val showBottomBar = currentRoute?.startsWith("auth/") != true

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                MainBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    role = role,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MainRoutes.FEED,
            modifier = Modifier.padding(padding),
        ) {
            // --- Feed ---
            composable(MainRoutes.FEED) {
                FeedRoute(
                    role = role,
                    onOwnProfileClick = {
                        navController.navigate(MainRoutes.PROFILE) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOtherProfileClick = { authorId ->
                        navController.navigate(MainRoutes.userProfile(authorId))
                    },
                    snackbarHostState = snackbarHostState,
                )
            }

            // --- SavePost (create + edit) ---
            composable(
                route = MainRoutes.SAVE_POST,
                arguments = listOf(
                    navArgument(MainRoutes.SAVE_POST_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
            ) {
                SavePostRoute(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    snackbarHostState = snackbarHostState,
                )
            }

            // --- Profile ---
            composable(MainRoutes.PROFILE) {
                ProfileRoute(
                    role = role,
                    onLoginClick = { navController.navigate(MainRoutes.AUTH_LOGIN) },
                    onRegisterClick = { navController.navigate(MainRoutes.AUTH_REGISTER) },
                    onEditPostClick = { postId ->
                        navController.navigate(MainRoutes.savePost(postId))
                    },
                    onLoggedOut = { /* authState сам переключит */ },
                    snackbarHostState = snackbarHostState,
                )
            }

            composable(
                route = MainRoutes.USER_PROFILE,             // "user_profile/{userId}"
                arguments = listOf(
                    navArgument(MainRoutes.USER_PROFILE_ARG) {   // "userId"
                        type = NavType.StringType
                    }
                ),
            ) {
                OtherProfileRoute(
                    onBack = { navController.popBackStack() },
                    snackbarHostState = snackbarHostState,
                )
            }

            // --- Auth nested graph ---
            navigation(
                startDestination = MainRoutes.AUTH_LOGIN,
                route = MainRoutes.AUTH_GRAPH,
            ) {
                composable(MainRoutes.AUTH_LOGIN) {
                    LoginRoute(
                        onBack = { navController.popBackStack() },
                        onForgotPassword = { navController.navigate(MainRoutes.AUTH_RESET) },
                        onGoToRegister = { navController.navigate(MainRoutes.AUTH_REGISTER) },
                    )
                }
                composable(MainRoutes.AUTH_REGISTER) {
                    RegisterRoute(
                        onBack = { navController.popBackStack() },
                        onGoToLogin = {
                            navController.popBackStack(MainRoutes.AUTH_LOGIN, inclusive = false)
                        },
                    )
                }
                composable(MainRoutes.AUTH_RESET) {
                    ResetPasswordRoute(
                        onBack = { navController.popBackStack() },
                        snackbarHostState = snackbarHostState,
                    )
                }
            }
        }
    }
}