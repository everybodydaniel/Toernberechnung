package com.example.trnberechnung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.trnberechnung.ui.home.*
import com.example.trnberechnung.ui.theme.*
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.messaging.ChatNavigationState
import com.example.trnberechnung.viewmodel.CrewspaceViewModel
import com.example.trnberechnung.viewmodel.CrewspaceViewModelFactory
import com.example.trnberechnung.viewmodel.RoutePlanningViewModel
import com.example.trnberechnung.viewmodel.TideViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object MapRoute : Screen("map_route", "Karte", Icons.Outlined.Map)
    object Revier : Screen("revier", "Revier", null)
    object Crew : Screen("crew", "Crewspace", Icons.AutoMirrored.Outlined.Chat)
    object Logbook : Screen("logbook", "Logbuch", Icons.Outlined.Book)
    object Settings : Screen("settings", "Einstellungen", Icons.Default.Settings)
    // Detail route for full route planning (opened from "Törn planen" card)
    object RoutePlanningDetail : Screen("route_planning_detail", "Törn planen", null)
}

val bottomNavItems = listOf(
    Screen.MapRoute,
    Screen.Revier,
    Screen.Crew,
    Screen.Logbook
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: TideViewModel,
    crewspaceViewModelFactory: CrewspaceViewModelFactory? = null,
    authRepo: AuthRepository? = null,
    onNavigateToLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onToggleDarkMode: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if the current screen is the home (map) screen
    // Home screen uses its own floating top bar + glassmorphism bottom bar
    val isHomeScreen = currentRoute == Screen.MapRoute.route

    LaunchedEffect(Unit) {
        ChatNavigationState.pendingConversationId.collect { conversationId ->
            if (conversationId != null && currentRoute != Screen.Crew.route) {
                navController.navigate(Screen.Crew.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(
        containerColor = if (isHomeScreen) Color.Transparent else NauticalBackground,
        topBar = {
            // Only show the Scaffold top bar for non-home screens
            if (!isHomeScreen) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Sailing,
                                contentDescription = "Logo",
                                tint = NauticalPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "TideNode",
                                fontWeight = FontWeight.ExtraBold,
                                color = NauticalTextPrimary,
                                fontSize = 20.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadData() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = NauticalPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NauticalBackground
                    )
                )
            }
        },
        bottomBar = {
            TideNodeBottomBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.MapRoute.route,
            // Home screen is edge-to-edge (no padding from top bar); others use innerPadding
            modifier = if (isHomeScreen) Modifier else Modifier.padding(innerPadding)
        ) {
            // ── Home Screen (Karte tab) – Apple Glass design ──
            composable(Screen.MapRoute.route) {
                val routeViewModel: RoutePlanningViewModel = viewModel()
                HomeScreen(
                    viewModel = viewModel,
                    routeViewModel = routeViewModel,
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToRoutePlanning = {
                        navController.navigate(Screen.RoutePlanningDetail.route)
                    },
                    onNavigateToChatHistory = {
                        // Open crew tab if user explicitly requests full crew chat history
                        navController.navigate(Screen.Crew.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }

            // ── Route Planning Detail (opened from Törn planen card) ──
            composable(Screen.RoutePlanningDetail.route) {
                val routeViewModel: RoutePlanningViewModel = viewModel()
                RoutePlanningScreen(viewModel, routeViewModel)
            }

            composable(Screen.Revier.route) {
                RevierScreen(viewModel)
            }
            composable(Screen.Crew.route) {
                if (crewspaceViewModelFactory != null) {
                    val crewspaceViewModel: CrewspaceViewModel = viewModel(factory = crewspaceViewModelFactory)
                    CrewspaceScreen(
                        viewModel = crewspaceViewModel,
                        authRepo = authRepo,
                        onNavigateToLogin = onNavigateToLogin
                    )
                } else {
                    CrewScreen(viewModel)
                }
            }
            composable(Screen.Logbook.route) {
                LogbookScreen(viewModel)
            }
            composable(Screen.Settings.route) {
                DashboardScreen(
                    authRepo = authRepo,
                    onNavigateToLogin = onNavigateToLogin,
                    onLogout = onLogout,
                    onStartNavigation = {
                        navController.navigate(Screen.MapRoute.route)
                    },
                    onToggleDarkMode = onToggleDarkMode
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Bottom Navigation Bar – Floating Apple Glass Capsule (Bild 2 Style)
// ══════════════════════════════════════════════════════════════

@Composable
fun TideNodeBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            containerColor = Color.White.copy(alpha = 0.80f),
            tonalElevation = 0.dp,
            modifier = Modifier
                .shadow(14.dp, RoundedCornerShape(36.dp), ambientColor = Color(0x1F000000), spotColor = Color(0x1F000000))
                .clip(RoundedCornerShape(36.dp))
                .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(36.dp))
                .height(68.dp)
        ) {
            bottomNavItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                NavigationBarItem(
                    modifier = Modifier.testTag("nav_${screen.route}"),
                    icon = {
                        when (screen.route) {
                            "map_route" -> Icon(
                                Icons.Outlined.Map,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                            "revier" -> Icon(
                                Icons.Outlined.WbSunny,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                            "crew" -> Icon(
                                Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                            "logbook" -> Icon(
                                Icons.Outlined.Book,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                            else -> {
                                if (screen.icon != null) {
                                    Icon(
                                        screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    },
                    label = {
                        Text(
                            screen.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF007AFF),
                        selectedTextColor = Color(0xFF007AFF),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0xFF007AFF).copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}


// Keep the old BottomNavigationBar for backward compatibility
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    TideNodeBottomBar(navController = navController)
}
