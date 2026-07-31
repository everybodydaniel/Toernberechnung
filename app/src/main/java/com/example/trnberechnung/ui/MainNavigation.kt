package com.example.trnberechnung.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.trnberechnung.TideNodeApplication
import com.example.trnberechnung.mapplanning.AndroidRouteAssessmentProvider
import com.example.trnberechnung.mapplanning.CatalogFairwayRouteResolver
import com.example.trnberechnung.mapplanning.RoutePlanningViewModel
import com.example.trnberechnung.mapplanning.RoutePlanningViewModelFactory
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.nauti.GeminiNautiClient
import com.example.trnberechnung.navigation.ActiveVoyageState
import com.example.trnberechnung.navigation.SensorHeadingProvider
import com.example.trnberechnung.navigation.VoyageServiceController
import com.example.trnberechnung.ui.components.GlassIconButton
import com.example.trnberechnung.ui.components.TideNodeAppHeader
import com.example.trnberechnung.ui.components.TideNodeBlue
import com.example.trnberechnung.ui.components.TideNodeInk
import com.example.trnberechnung.ui.components.tideNodeGlass
import com.example.trnberechnung.ui.map.MapTabScreen
import com.example.trnberechnung.ui.navigation.FullScreenNavigationScreen
import com.example.trnberechnung.ui.notices.MaritimeNoticeQuickLook
import com.example.trnberechnung.ui.notices.MaritimeNoticesSheet
import com.example.trnberechnung.ui.theme.NauticalBackground
import com.example.trnberechnung.viewmodel.CrewspaceViewModel
import com.example.trnberechnung.viewmodel.CrewspaceViewModelFactory
import com.example.trnberechnung.viewmodel.MaritimeNoticeViewModel
import com.example.trnberechnung.viewmodel.NautiViewModel
import com.example.trnberechnung.viewmodel.TideViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object MapRoute : Screen("map_route", "Karte", Icons.Default.Map)

    data object Revier : Screen("revier", "Wetter", Icons.Default.Cloud)

    data object Crew : Screen("crew", "Crewspace", Icons.Default.People)

    data object Logbook : Screen("logbook", "Logbuch", Icons.AutoMirrored.Filled.MenuBook)

    data object Settings : Screen("settings", "Einstellungen", Icons.Default.Map)

    data object Navigation : Screen("navigation", "Navigation", Icons.Default.Map)
}

val bottomNavItems = listOf(Screen.MapRoute, Screen.Revier, Screen.Crew, Screen.Logbook)

@Composable
fun MainAppScreen(
    viewModel: TideViewModel,
    crewspaceViewModelFactory: CrewspaceViewModelFactory? = null,
    authRepo: AuthRepository? = null,
    onNavigateToLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onToggleDarkMode: (Boolean) -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
) {
    val context = LocalContext.current
    val application = context.applicationContext as TideNodeApplication
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.MapRoute.route
    val isMainTab = currentRoute in bottomNavItems.map(Screen::route)

    val crewspaceViewModel =
        crewspaceViewModelFactory?.let {
            viewModel<CrewspaceViewModel>(factory = it)
        }
    val noticeViewModel: MaritimeNoticeViewModel =
        viewModel(
            factory =
                remember(application) {
                    MaritimeNoticeViewModel.Factory(application.maritimeNoticeRepository)
                },
        )
    val nautiViewModel: NautiViewModel =
        viewModel(
            factory =
                remember(application) {
                    NautiViewModel.Factory(
                        application.nautiConversationRepository,
                        GeminiNautiClient(),
                    )
                },
        )
    val routePlanningViewModel: RoutePlanningViewModel =
        viewModel(
            factory =
                remember(viewModel) {
                    RoutePlanningViewModelFactory(
                        AndroidRouteAssessmentProvider(
                            stations = viewModel.allStations,
                            fairwayRouteResolver = CatalogFairwayRouteResolver,
                        ),
                        metricRouteResolver = CatalogFairwayRouteResolver,
                    )
                },
        )
    val headingProvider =
        remember(application) {
            SensorHeadingProvider(
                context = context,
                latestLocation = {
                    application.navigationLocationProvider.latestFix.value
                },
            )
        }

    val unreadCount by noticeViewModel.unreadCount.collectAsState()
    val activeVoyageState by application.activeVoyageManager.state.collectAsState()
    var showNoticeQuickLook by remember { mutableStateOf(false) }
    var showNoticeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        application.activeVoyageManager.restoreActiveVoyage()
    }

    val refreshCoordinator =
        remember(viewModel, crewspaceViewModel, routePlanningViewModel) {
            AppRefreshCoordinator(
                refreshMap = {
                    viewModel.loadData()
                    routePlanningViewModel.refresh()
                    routePlanningViewModel.refreshPassageWindow()
                },
                refreshWeather = viewModel::loadData,
                refreshCrewspace = { crewspaceViewModel?.refresh() },
                refreshLogbook = viewModel::loadData,
            )
        }

    val topClearance = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 78.dp
    val bottomSystemInset =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomOverlayClearance = 88.dp
    val mapBottomClearance = bottomSystemInset + bottomOverlayClearance

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.MapRoute.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Screen.MapRoute.route) {
                MapTabScreen(
                    tideViewModel = viewModel,
                    planningViewModel = routePlanningViewModel,
                    nautiViewModel = nautiViewModel,
                    activeVoyageManager = application.activeVoyageManager,
                    locationProvider = application.navigationLocationProvider,
                    topOverlayClearance = topClearance,
                    bottomOverlayClearance = mapBottomClearance,
                    onOpenWeather = {
                        navController.navigateMainTab(Screen.Revier.route)
                    },
                    onOpenNavigation = {
                        navController.navigate(Screen.Navigation.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Screen.Revier.route) {
                MainTabContent {
                    WeatherScreen(
                        viewModel = viewModel,
                        topOverlayClearance = topClearance,
                        bottomOverlayClearance = bottomOverlayClearance,
                    )
                }
            }
            composable(Screen.Crew.route) {
                MainTabContent {
                    if (crewspaceViewModel != null) {
                        CrewspaceScreen(
                            viewModel = crewspaceViewModel,
                            authRepo = authRepo,
                            onNavigateToLogin = onNavigateToLogin,
                            topOverlayClearance = topClearance,
                            bottomOverlayClearance = bottomOverlayClearance,
                        )
                    } else {
                        CrewScreen(
                            viewModel = viewModel,
                            bottomOverlayClearance = bottomOverlayClearance,
                        )
                    }
                }
            }
            composable(Screen.Logbook.route) {
                MainTabContent {
                    LogbookScreen(
                        viewModel = viewModel,
                        topOverlayClearance = topClearance,
                        bottomOverlayClearance = bottomOverlayClearance,
                    )
                }
            }
            composable(Screen.Settings.route) {
                SettingsDestination(
                    onBack = navController::popBackStack,
                ) {
                    DashboardScreen(
                        authRepo = authRepo,
                        onNavigateToLogin = onNavigateToLogin,
                        onLogout = onLogout,
                        onStartNavigation = {
                            navController.navigateMainTab(Screen.MapRoute.route)
                        },
                        onToggleDarkMode = onToggleDarkMode,
                        onReplayOnboarding = onReplayOnboarding,
                    )
                }
            }
            composable(Screen.Navigation.route) {
                if (activeVoyageState !is ActiveVoyageState.Active) {
                    LaunchedEffect(Unit) {
                        navController.navigateMainTab(Screen.MapRoute.route)
                    }
                }
                FullScreenNavigationScreen(
                    activeVoyageManager = application.activeVoyageManager,
                    locationProvider = application.navigationLocationProvider,
                    headingProvider = headingProvider,
                    onMinimize = {
                        navController.navigateMainTab(Screen.MapRoute.route)
                    },
                    onVoyageFinished = {
                        VoyageServiceController.stop(context)
                        navController.navigateMainTab(Screen.MapRoute.route)
                    },
                )
            }
        }

        if (isMainTab) {
            TideNodeAppHeader(
                unreadCount = unreadCount,
                onNotifications = { showNoticeQuickLook = !showNoticeQuickLook },
                onRefresh = { refreshCoordinator.refresh(currentRoute) },
                onSettings = {
                    showNoticeQuickLook = false
                    navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                },
                modifier = Modifier.align(Alignment.TopCenter).testTag("global_app_header"),
            )

            TideNodeBottomNavigation(
                navController = navController,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            if (
                currentRoute == Screen.MapRoute.route &&
                activeVoyageState is ActiveVoyageState.Active
            ) {
                ActiveVoyageResumePill(
                    onClick = {
                        (context as? Activity)?.let(VoyageServiceController::startFromVisibleActivity)
                        navController.navigate(Screen.Navigation.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = 28.dp,
                                end = 28.dp,
                                bottom = mapBottomClearance + 8.dp,
                            ),
                )
            }
        }

        if (showNoticeQuickLook && isMainTab) {
            MaritimeNoticeQuickLook(
                viewModel = noticeViewModel,
                onDismiss = { showNoticeQuickLook = false },
                onShowAll = { showNoticeSheet = true },
            )
        }
    }

    if (showNoticeSheet) {
        MaritimeNoticesSheet(
            viewModel = noticeViewModel,
            onDismiss = { showNoticeSheet = false },
        )
    }
}

@Composable
private fun MainTabContent(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        content()
    }
}

@Composable
private fun SettingsDestination(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = topInset + 60.dp),
        ) {
            content()
        }
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Zurück",
            onClick = onBack,
            modifier =
                Modifier
                    .padding(start = 16.dp, top = topInset + 8.dp)
                    .testTag("settings_back"),
        )
    }
}

@Composable
private fun TideNodeBottomNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Row(
        modifier =
            modifier
                .padding(start = 18.dp, end = 18.dp, bottom = bottomInset + 10.dp)
                .fillMaxWidth()
                .height(72.dp)
                .tideNodeGlass(cornerRadius = 34.dp, elevation = 14.dp, alpha = 0.80f)
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("global_bottom_navigation"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(62.dp)
                        .background(
                            color =
                                if (selected) {
                                    TideNodeBlue.copy(alpha = 0.12f)
                                } else {
                                    Color.Transparent
                                },
                            shape = RoundedCornerShape(28.dp),
                        )
                        .selectable(
                            selected = selected,
                            onClick = { navController.navigateMainTab(screen.route) },
                        )
                        .testTag("nav_${screen.route}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.title,
                    tint = if (selected) TideNodeBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(25.dp).testTag("nav_icon_${screen.route}"),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = screen.title,
                    color = if (selected) TideNodeBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    modifier = Modifier.testTag("nav_text_${screen.route}")
                )
            }
        }
    }
}

@Composable
private fun ActiveVoyageResumePill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .tideNodeGlass(cornerRadius = 25.dp, elevation = 10.dp, alpha = 0.88f)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .testTag("active_voyage_resume"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Map, null, tint = TideNodeBlue)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Aktive Fahrt",
                color = TideNodeInk,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Navigation fortsetzen",
                color = Color(0xFF62666C),
                fontSize = 12.sp,
            )
        }
        Text("›", color = TideNodeBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

private fun NavHostController.navigateMainTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
