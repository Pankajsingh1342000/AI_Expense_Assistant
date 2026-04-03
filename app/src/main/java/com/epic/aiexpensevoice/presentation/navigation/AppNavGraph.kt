package com.epic.aiexpensevoice.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.epic.aiexpensevoice.core.common.AppContainer
import com.epic.aiexpensevoice.core.common.AppViewModelFactory
import com.epic.aiexpensevoice.presentation.components.GradientScreen
import com.epic.aiexpensevoice.presentation.screen.auth.AuthViewModel
import com.epic.aiexpensevoice.presentation.screen.auth.LoginScreen
import com.epic.aiexpensevoice.presentation.screen.auth.RegisterScreen
import com.epic.aiexpensevoice.presentation.screen.chat.ChatScreen
import com.epic.aiexpensevoice.presentation.screen.chat.ChatViewModel
import com.epic.aiexpensevoice.presentation.screen.dashboard.DashboardScreen
import com.epic.aiexpensevoice.presentation.screen.dashboard.DashboardViewModel
import com.epic.aiexpensevoice.presentation.screen.expenses.ExpensesScreen
import com.epic.aiexpensevoice.presentation.screen.expenses.ExpensesViewModel
import com.epic.aiexpensevoice.presentation.screen.profile.ProfileScreen
import com.epic.aiexpensevoice.presentation.screen.profile.ProfileViewModel
import com.epic.aiexpensevoice.presentation.screen.splash.SplashScreen
import com.epic.aiexpensevoice.presentation.screen.splash.SplashViewModel
import kotlinx.coroutines.delay

@Composable
fun AppNavGraph(container: AppContainer) {
    val factory = AppViewModelFactory(container)
    val navController = rememberNavController()
    val splashViewModel: SplashViewModel = viewModel(factory = factory)
    val session by splashViewModel.session.collectAsState()

    NavHost(navController = navController, startDestination = Route.Splash.route) {
        composable(Route.Splash.route) {
            SplashScreen()
            LaunchedEffect(session?.isLoggedIn) {
                val restoredSession = session ?: return@LaunchedEffect
                delay(900)
                navController.navigate(if (restoredSession.isLoggedIn) Route.Shell.route else Route.Login.route) {
                    popUpTo(Route.Splash.route) { inclusive = true }
                }
            }
        }
        composable(Route.Login.route) {
            val viewModel: AuthViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Route.Shell.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                },
                onNavigateRegister = { navController.navigate(Route.Register.route) },
            )
        }
        composable(Route.Register.route) {
            val viewModel: AuthViewModel = viewModel(factory = factory)
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.Register.route) { inclusive = true }
                    }
                },
                onNavigateLogin = { navController.popBackStack() },
            )
        }
        composable(Route.Shell.route) {
            MainShell(container = container, parentNavController = navController)
        }
    }
}

private data class BottomItem(
    val route: Route,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun MainShell(
    container: AppContainer,
    parentNavController: NavHostController,
) {
    val factory = AppViewModelFactory(container)
    val navController = rememberNavController()
    val items = listOf(
        BottomItem(Route.Chat, "Chat", Icons.Default.ChatBubble),
        BottomItem(Route.Dashboard, "Dashboard", Icons.Default.PieChart),
        BottomItem(Route.Expenes, "Expenses", Icons.AutoMirrored.Filled.ReceiptLong),
        BottomItem(Route.Profile, "Profile", Icons.Default.AccountCircle),
    )

    GradientScreen {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                    val current by navController.currentBackStackEntryAsState()
                    items.forEach { item ->
                        val selected = current?.destination?.hierarchy?.any { it.route == item.route.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(navController = navController, startDestination = Route.Chat.route, modifier = Modifier.padding(innerPadding)) {
                composable(Route.Chat.route) {
                    val viewModel: ChatViewModel = viewModel(factory = factory)
                    ChatScreen(viewModel)
                }
                composable(Route.Dashboard.route) {
                    val viewModel: DashboardViewModel = viewModel(factory = factory)
                    DashboardScreen(viewModel)
                }
                composable(Route.Expenes.route) {
                    val viewModel: ExpensesViewModel = viewModel(factory = factory)
                    ExpensesScreen(viewModel)
                }
                composable(Route.Profile.route) {
                    val viewModel: ProfileViewModel = viewModel(factory = factory)
                    ProfileScreen(
                        viewModel = viewModel,
                        onLoggedOut = {
                            parentNavController.navigate(Route.Login.route) {
                                popUpTo(Route.Shell.route) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}
