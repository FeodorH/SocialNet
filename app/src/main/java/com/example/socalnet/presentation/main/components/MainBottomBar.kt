package com.example.socalnet.presentation.main.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.socalnet.presentation.main.navigation.MainRoutes

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val requiresAuth: Boolean,
)

private val bottomItems = listOf(
    BottomItem(MainRoutes.FEED, "Лента", Icons.Default.Home, requiresAuth = false),
    BottomItem(MainRoutes.savePost(), "Создать", Icons.Default.Add, requiresAuth = true),
    BottomItem(MainRoutes.PROFILE, "Профиль", Icons.Default.Person, requiresAuth = false),
)

@Composable
fun MainBottomBar(
    navController: NavHostController,
    currentRoute: String?,
    role: Boolean,
) {
    NavigationBar {
        bottomItems.forEach { item ->
            val enabled = !item.requiresAuth || role
            val selected = currentRoute
                ?.substringBefore("?")
                ?.substringBefore("/{")
                ?.let { currentPrefix ->
                    item.route.substringBefore("?").startsWith(currentPrefix) ||
                            currentPrefix.startsWith(item.route.substringBefore("?"))
                } ?: false

            NavigationBarItem(
                selected = selected,
                enabled = enabled,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}