package com.example.trnberechnung.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.trnberechnung.ui.MapScreen
import com.example.trnberechnung.viewmodel.TideViewModel
import com.example.trnberechnung.viewmodel.RoutePlanningViewModel

/**
 * TideNode HomeScreen – the main "Karte" tab (Apple Glass Design).
 *
 * Displays a fullscreen nautical chart (MapScreen) with overlaid Apple Glass frosted panels:
 * 1. Floating TideNode glass top bar
 * 2. Glass bottom sheet with:
 *    - Törn Planen (Trip Planning) floating glass card
 *    - Skipper-KI (Nauti AI) interactive chat panel
 *    - Seefahrer-Nachrichten (Seafarer Messages) panel
 */
@Composable
fun HomeScreen(
    viewModel: TideViewModel,
    routeViewModel: RoutePlanningViewModel,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRoutePlanning: () -> Unit = {},
    onNavigateToChatHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stations by viewModel.data.collectAsState()
    val allStations by viewModel.allStations.collectAsState()
    val routeUiState by routeViewModel.uiState.collectAsState()

    // Seafarer messages
    val unreadMessages by viewModel.unreadMessages.collectAsState()
    val allActiveMessages by viewModel.allActiveMessages.collectAsState()
    val archivedMessages by viewModel.archivedMessages.collectAsState()
    val unreadCount by viewModel.unreadMessageCount.collectAsState()
    val searchResults by viewModel.seafarerSearchResults.collectAsState()

    // Sync BfS messages on first load
    LaunchedEffect(Unit) {
        viewModel.syncSeafarerMessages()
    }

    // Consume scroll events so map doesn't steal from the sheet
    val consumeAllScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return available
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ═══════════════════════════════════════════════
        // Layer 1: Fullscreen Nautical Chart (MapScreen)
        // ═══════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(consumeAllScrollConnection)
        ) {
            MapScreen(
                stations = stations,
                routePoints = routeUiState.route,
                routeSegments = routeUiState.routeSegments,
                depthPoints = routeUiState.depthPoints,
                harbors = allStations,
                onHarborClick = { harbor ->
                    viewModel.selectStation(harbor)
                },
                onMapClick = { /* no-op on home */ },
                onStationSelected = { station ->
                    viewModel.selectStation(station)
                }
            )
        }

        // ═══════════════════════════════════════════════
        // Layer 2: Floating Apple Glass Top Bar
        // ═══════════════════════════════════════════════
        TideNodeTopBar(
            onNotificationsClick = { /* Notifications action */ },
            onRefreshClick = {
                viewModel.loadData()
                viewModel.syncSeafarerMessages()
            },
            onSettingsClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 4.dp)
        )

        // ═══════════════════════════════════════════════
        // Layer 3: Apple Glass Bottom Sheet Overlay
        // ═══════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.60f)
                .shadow(16.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White.copy(alpha = 0.75f))
                .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // ── Drag Handle ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF94A3B8))
                )
            }

            // ── Panel 1: Törn Planen (Apple Glass Card) ──
            TripPlanningCard(
                onCardClick = onNavigateToRoutePlanning,
                onRefreshDepartureWindow = {
                    viewModel.loadData()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Panel 2: Skipper-KI Nauti Chat (Apple Glass & Fully Functional) ──
            SkipperAiPanel(
                onOpenChatHistory = onNavigateToChatHistory
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Panel 3: Seefahrer-Nachrichten ──
            SeafarerMessagesPanel(
                unreadMessages = unreadMessages,
                allMessages = allActiveMessages,
                archivedMessages = archivedMessages,
                unreadCount = unreadCount,
                searchResults = searchResults,
                onDoneClick = {
                    viewModel.markAllMessagesAsRead()
                },
                onMessageClick = { message ->
                    viewModel.markMessageAsRead(message.id)
                },
                onArchiveMessage = { messageId ->
                    viewModel.archiveMessage(messageId)
                },
                onSearchQuery = { query ->
                    viewModel.searchSeafarerMessages(query)
                }
            )

            // Extra padding at bottom for floating navigation bar
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
