package com.epic.aiexpensevoice.presentation.navigation

import androidx.compose.ui.res.stringResource
import com.epic.aiexpensevoice.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(session?.isLoggedIn, currentBackStackEntry?.destination?.route) {
        val route = currentBackStackEntry?.destination?.route
        val isAuthRoute = route == Route.Login.route || route == Route.Register.route || route == Route.Splash.route
        if (session != null && !session!!.isLoggedIn && !isAuthRoute) {
            navController.navigate(Route.Login.route) {
                popUpTo(Route.Shell.route) { inclusive = true }
            }
        }
    }

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
    val labelResId: Int,
    val icon: ImageVector,
)

@Composable
private fun MainShell(
    container: AppContainer,
    parentNavController: NavHostController,
) {
    val factory = AppViewModelFactory(container)
    val navController = rememberNavController()
    val items = listOf(
        BottomItem(Route.Dashboard, R.string.nav_dashboard, Icons.Default.PieChart),
        BottomItem(Route.Expenes, R.string.nav_expenses, Icons.AutoMirrored.Filled.ReceiptLong),
        BottomItem(Route.Chat, R.string.nav_chat, Icons.Default.ChatBubble),
        BottomItem(Route.Profile, R.string.nav_profile, Icons.Default.AccountCircle),
    )

    GradientScreen {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
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
                                icon = { Icon(item.icon, contentDescription = stringResource(item.labelResId)) },
                                label = { Text(stringResource(item.labelResId)) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Route.Dashboard.route,
                modifier = Modifier.padding(innerPadding),
            ) {
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
