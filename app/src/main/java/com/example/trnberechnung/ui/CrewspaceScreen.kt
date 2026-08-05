package com.example.trnberechnung.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.trnberechnung.messaging.ChatNavigationState
import com.example.trnberechnung.model.*
import com.example.trnberechnung.viewmodel.CrewspaceViewModel
import com.example.trnberechnung.logic.ValidationUtils
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

import androidx.compose.ui.graphics.luminance

// ══════════════════════════════════════════════════════════════
// Crewspace Farbpalette (Moderneres Look & Feel)
// ══════════════════════════════════════════════════════════════
private val CrewspaceBg: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF0A0F14) else Color(0xFFF8F9FA)

private val CrewspaceSurface: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF161E26) else Color.White

private val CrewspacePrimary: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF3B82F6) else Color(0xFF1E3A8A)

private val CrewspaceAccent: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF60A5FA) else Color(0xFF2563EB)

private val CrewspaceTextPrimary: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF1F5F9) else Color(0xFF111827)

private val CrewspaceTextSecondary: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF94A3B8) else Color(0xFF6B7280)

private val CrewspaceTabBg: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E293B) else Color(0xFFF1F5F9)

private val CrewspaceTabActive: Color
    @Composable get() = CrewspaceAccent

private val CrewspaceTabActiveText: Color = Color.White

private val CrewspaceTabInactiveText: Color
    @Composable get() = CrewspaceTextSecondary

private val CrewspaceDivider: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E293B) else Color(0xFFE5E7EB)

private val CrewspaceCardBg: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E293B) else Color(0xFFF3F4F6)

private val CrewspaceUnreadBadge = Color(0xFFEF4444)

/**
 * Haupt-Composable für den gesamten Crewspace-Screen.
 * Enthält Header, Segmented Tabs und routet zu den drei Sub-Screens.
 */
@Composable
fun CrewspaceScreen(
    viewModel: CrewspaceViewModel,
    authRepo: AuthRepository? = null,
    onNavigateToLogin: () -> Unit = {},
    topOverlayClearance: Dp = 0.dp,
    bottomOverlayClearance: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeConversationId = uiState.activeChatThread?.id

    DisposableEffect(activeConversationId) {
        ChatNavigationState.setActiveConversation(activeConversationId)
        onDispose {
            if (ChatNavigationState.activeConversationId.value == activeConversationId) {
                ChatNavigationState.setActiveConversation(null)
            }
        }
    }

    // Wenn ein aktiver Chat offen ist, zeige den Chat-Detail-Screen
    if (uiState.activeChatThread != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CrewspaceBg)
                .padding(bottom = bottomOverlayClearance)
        ) {
            ChatDetailScreen(viewModel = viewModel, thread = uiState.activeChatThread!!)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_crewspace")
            .background(CrewspaceBg)
            .padding(bottom = bottomOverlayClearance)
    ) {
        Spacer(modifier = Modifier.height(topOverlayClearance + 4.dp))
        // ── Header ──
        CrewspaceHeader()

        // ── Segmented Tab Row ──
        CrewspaceSegmentedTabs(
            selectedTab = uiState.selectedTab,
            onTabSelected = { viewModel.selectTab(it) }
        )

        // ── Suchleiste (nur im Chat-Tab sichtbar) ──
        AnimatedVisibility(
            visible = uiState.selectedTab == CrewspaceTab.CHATS,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            CrewspaceSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )
        }

        // ── Tab-Content ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (uiState.selectedTab) {
                CrewspaceTab.CHATS -> {
                    ChatsTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        authRepo = authRepo,
                        onNavigateToLogin = onNavigateToLogin
                    )
                }
                CrewspaceTab.PLANUNG -> {
                    PlanungTabContent(uiState = uiState, viewModel = viewModel)
                }
                CrewspaceTab.CREW -> {
                    CrewTabContent(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }

    // ── BottomSheet für neue Unterhaltung ──
    if (uiState.showNewConversationSheet) {
        NewConversationBottomSheet(uiState = uiState, viewModel = viewModel)
    }
}
// Header: "Crewspace" Titel + Untertitel + Edit-Button
// ══════════════════════════════════════════════════════════════

@Composable
private fun CrewspaceHeader() {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = if (isLandscape) 2.dp else 12.dp)
    ) {
        Text(
            text = "Crewspace",
            fontSize = if (isLandscape) 20.sp else 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CrewspacePrimary,
            letterSpacing = (-0.5).sp
        )
        if (!isLandscape) {
            Text(
                text = "Crew, Gespräche und Termine",
                fontSize = 14.sp,
                color = CrewspaceTextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Segmented Tab Row (iOS-Stil)
// ══════════════════════════════════════════════════════════════

private data class TabItem(
    val tab: CrewspaceTab,
    val icon: ImageVector,
    val label: String
)

private val tabItems = listOf(
    TabItem(CrewspaceTab.CHATS, Icons.AutoMirrored.Outlined.Chat, "Chats"),
    TabItem(CrewspaceTab.PLANUNG, Icons.Outlined.DateRange, "Planung"),
    TabItem(CrewspaceTab.CREW, Icons.Outlined.Groups, "Crew"),
)

@Composable
private fun CrewspaceSegmentedTabs(
    selectedTab: CrewspaceTab,
    onTabSelected: (CrewspaceTab) -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = if (isLandscape) 2.dp else 8.dp),
        color = CrewspaceTabBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabItems.forEach { item ->
                val isSelected = selectedTab == item.tab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) CrewspaceTabActive else Color.Transparent
                        )
                        .clickable { onTabSelected(item.tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) CrewspaceTabActiveText else CrewspaceTabInactiveText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CrewspaceTabActiveText else CrewspaceTabInactiveText
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Suchleiste
// ══════════════════════════════════════════════════════════════

@Composable
private fun CrewspaceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CrewspaceSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "Suchen",
            tint = CrewspaceTextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        if (query.isEmpty()) {
            Text(
                text = "Chats durchsuchen",
                fontSize = 15.sp,
                color = CrewspaceTextSecondary
            )
        }
        // Hinweis: In einer vollständigen Implementierung würde hier ein
        // BasicTextField stehen. Für das Grundgerüst reicht der visuelle Platzhalter.
    }
}

// ══════════════════════════════════════════════════════════════
// ══════════════════════════════════════════════════════════════
// Chats Tab – Vollständige Implementierung
// ══════════════════════════════════════════════════════════════

@Composable
private fun ChatsTabContent(
    uiState: CrewspaceUiState,
    viewModel: CrewspaceViewModel,
    authRepo: AuthRepository? = null,
    onNavigateToLogin: () -> Unit = {}
) {
    val isLoggedIn = authRepo?.isLoggedIn ?: true // Fallback true, falls authRepo null ist

    if (!isLoggedIn) {
        // ── Gast-Modus UI ──
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = CrewspaceTextSecondary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Du bist im Gastmodus",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrewspaceTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Melde dich an, um mit anderen Skippern in Kontakt zu treten und Crews zu planen.",
                    fontSize = 15.sp,
                    color = CrewspaceTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = CrewspacePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp).fillMaxWidth()
                ) {
                    Text("Jetzt Anmelden", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    if (uiState.chatThreads.isEmpty()) {
        // ── Empty State: "Dein Crewspace ist bereit" ──
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CrewspaceCardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = null,
                        tint = CrewspaceAccent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Dein Crewspace ist bereit",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrewspaceTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Starte einen Direktchat über eine Skipper-ID oder\nerstelle eine Gruppe für deine nächste Tour.",
                        fontSize = 13.sp,
                        color = CrewspaceTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.showNewConversationSheet() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrewspaceAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "+ Unterhaltung starten",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    } else {
        // ── Chat-Liste ──
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.chatThreads, key = { it.id }) { thread ->
                ChatThreadCard(
                    thread = thread,
                    onClick = { viewModel.openChat(thread) }
                )
            }
        }
    }
}

// ── Chat-Thread Card ─────────────────────────────────────────

@Composable
private fun ChatThreadCard(thread: ChatThread, onClick: () -> Unit) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val time = remember(thread.lastMessageTimestamp) {
        Instant.ofEpochMilli(thread.lastMessageTimestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = CrewspaceSurface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Moderner Avatar mit Initialen-Stil
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(CrewspaceAccent.copy(alpha = 0.1f))
                    .border(1.dp, CrewspaceAccent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = thread.participantName.take(1).uppercase(),
                    color = CrewspaceAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name + letzte Nachricht
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.participantName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrewspaceTextPrimary
                    )
                    Text(
                        text = time,
                        fontSize = 12.sp,
                        color = CrewspaceTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.lastMessage.ifBlank { "Keine Nachrichten" },
                        fontSize = 14.sp,
                        color = CrewspaceTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (thread.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(CrewspaceUnreadBadge),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${thread.unreadCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Neue Unterhaltung BottomSheet ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewConversationBottomSheet(
    uiState: CrewspaceUiState,
    viewModel: CrewspaceViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { viewModel.hideNewConversationSheet() },
        sheetState = sheetState,
        containerColor = CrewspaceSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CrewspaceDivider)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Titel
            Text(
                text = "Neue Unterhaltung",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CrewspaceTextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            // ── Direkt / Gruppe Tabs ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, CrewspaceDivider, RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    NewConversationTab.DIRECT to "Direkt",
                    NewConversationTab.GROUP to "Gruppe"
                ).forEach { (tab, label) ->
                    val isSelected = uiState.newConversationTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) CrewspaceBg else Color.Transparent
                            )
                            .clickable { viewModel.setNewConversationTab(tab) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) CrewspaceTextPrimary else CrewspaceTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Skipper-ID Feld ──
            Text(
                text = "SKIPPER ÜBER ID FINDEN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CrewspaceTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            var showCrewDropdown by remember { mutableStateOf(false) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    CrewspaceTextField(
                        value = uiState.newConversationSkipperId,
                        onValueChange = { input ->
                            viewModel.updateNewConversationSkipperId(ValidationUtils.sanitizeSkipperId(input))
                        },
                        placeholder = "Skipper-ID",
                        trailingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Suchen",
                                tint = CrewspaceTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Aus Crew auswählen
                Box {
                    IconButton(
                        onClick = { showCrewDropdown = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(CrewspaceCardBg, RoundedCornerShape(10.dp))
                            .border(1.dp, CrewspaceDivider, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            Icons.Outlined.Groups,
                            contentDescription = "Aus Crew auswählen",
                            tint = CrewspaceAccent
                        )
                    }

                    DropdownMenu(
                        expanded = showCrewDropdown,
                        onDismissRequest = { showCrewDropdown = false },
                        modifier = Modifier.background(CrewspaceSurface)
                    ) {
                        if (uiState.crewMembers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Keine Crew vorhanden", color = CrewspaceTextSecondary) },
                                onClick = { showCrewDropdown = false }
                            )
                        } else {
                            uiState.crewMembers.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.name.ifBlank { "Unbenannt" }) },
                                    onClick = {
                                        viewModel.updateNewConversationSkipperId(member.skipperId)
                                        showCrewDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            uiState.chatError?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFB42318),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            // ── Chat starten Button ──
            Button(
                onClick = {
                    viewModel.startChat(
                        skipperId = uiState.newConversationSkipperId
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.newConversationSkipperId.isNotBlank() && !uiState.chatBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrewspaceAccent,
                    contentColor = Color.White,
                    disabledContainerColor = CrewspaceDivider,
                    disabledContentColor = CrewspaceTextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = if (uiState.chatBusy) "Wird geöffnet …" else "→ Chat starten",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Chat-Detail-Screen
// ══════════════════════════════════════════════════════════════

@Composable
private fun ChatDetailScreen(viewModel: CrewspaceViewModel, thread: ChatThread) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    var previousMessageIds by remember(thread.id) { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(thread.messages.map { it.id }) {
        val currentIds = thread.messages.map { it.id }
        val appendedAtBottom =
            previousMessageIds.isEmpty() ||
                (
                    currentIds.size > previousMessageIds.size &&
                        currentIds.take(previousMessageIds.size) == previousMessageIds
                )
        if (currentIds.isNotEmpty() && appendedAtBottom) {
            // Index 0 is the "Ältere Nachrichten laden" row.
            listState.animateScrollToItem(currentIds.size)
        }
        previousMessageIds = currentIds
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CrewspaceBg)
            .imePadding()
    ) {
        // ── Chat Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CrewspaceBg)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.closeChat() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurück",
                    tint = CrewspaceTextPrimary
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CrewspaceAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = CrewspaceAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = thread.participantName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrewspaceTextPrimary
                )
                Text(
                    text = if (thread.type == ChatThreadType.DIRECT) "Direktnachricht" else "Gruppe",
                    fontSize = 12.sp,
                    color = CrewspaceTextSecondary
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            if (
                thread.type == ChatThreadType.DIRECT &&
                (thread.isChatAvailable || thread.isBlockedByMe)
            ) {
                IconButton(
                    onClick = {
                        if (thread.isBlockedByMe) {
                            viewModel.unblockActiveChat()
                        } else {
                            viewModel.blockActiveChat()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription =
                            if (thread.isBlockedByMe) {
                                "Blockierung aufheben"
                            } else {
                                "Kontakt blockieren"
                            },
                        tint =
                            if (thread.isBlockedByMe) {
                                Color(0xFFFF3B30)
                            } else {
                                CrewspaceTextSecondary
                            },
                    )
                }
            }
        }

        uiState.chatError?.let { error ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFE8E6))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = error,
                    color = Color(0xFFB42318),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = viewModel::clearChatError,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fehler schließen")
                }
            }
        }

        // ── Nachrichten-Liste ──
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item(key = "load_older") {
                TextButton(
                    onClick = viewModel::loadOlderMessages,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ältere Nachrichten laden")
                }
            }
            items(thread.messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onRetry = { viewModel.retryMessage(message.id) },
                    onAcceptEvent = { title, desc, dateStr ->
                        try {
                            // Versuche das Datum zu parsen. Falls es fehlschlägt, öffne den Edit-Sheet für manuelles Anpassen
                            val parsedDate = runCatching { LocalDate.parse(dateStr) }.getOrNull()
                            if (parsedDate != null) {
                                viewModel.addPlannerEventWithDate(title, desc, parsedDate)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                )
            }
        }

        // ── Chat-Input-Leiste ──
        if (thread.isChatAvailable) {
            ChatInputBar(
                text = uiState.chatInput,
                onTextChange = { viewModel.updateChatInput(it) },
                onSend = {
                    viewModel.sendMessage()
                    focusManager.clearFocus()
                },
                onAttachImage = { uri ->
                    viewModel.sendAttachmentMessage(ChatMessageType.IMAGE, uri)
                },
                onSendVoice = { uri, duration ->
                    viewModel.sendAttachmentMessage(ChatMessageType.VOICE, uri, duration)
                },
            )
        } else {
            Surface(color = CrewspaceSurface) {
                Text(
                    text = "Chat nicht verfügbar",
                    color = CrewspaceTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
    }
}

// ── Chat-Bubble ────────────────────────────────────────────

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onRetry: () -> Unit = {},
    onAcceptEvent: (title: String, desc: String, dateStr: String) -> Unit = { _, _, _ -> }
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val time = remember(message.timestamp) {
        Instant.ofEpochMilli(message.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)
    }

    val alignment = if (message.isOwnMessage) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isOwnMessage) CrewspaceAccent else CrewspaceSurface
    val textColor = if (message.isOwnMessage) Color.White else CrewspaceTextPrimary
    val timeColor = if (message.isOwnMessage) Color.White.copy(alpha = 0.7f) else CrewspaceTextSecondary
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (message.isOwnMessage) 16.dp else 4.dp,
        bottomEnd = if (message.isOwnMessage) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        when (message.type) {
            ChatMessageType.TEXT -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(bubbleShape)
                        .background(bubbleColor)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = message.content,
                            fontSize = 15.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = time,
                            fontSize = 11.sp,
                            color = timeColor
                        )
                    }
                }
            }
            ChatMessageType.VOICE -> {
                VoiceMessageBubble(
                    durationSeconds = message.voiceDurationSeconds,
                    time = time,
                    isOwnMessage = message.isOwnMessage,
                    mediaSource = message.localMediaUri ?: message.mediaUrl,
                )
            }
            ChatMessageType.IMAGE -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(bubbleShape)
                        .background(bubbleColor)
                        .padding(4.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            coil.compose.AsyncImage(
                                model = message.localMediaUri ?: message.mediaUrl,
                                contentDescription = "Bild",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = time,
                            fontSize = 11.sp,
                            color = timeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            ChatMessageType.EVENT -> {
                val parts = message.content.split("|")
                val isOldFormat = parts.size >= 2 && parts.all { it.isNotEmpty() }

                val eventTitle: String
                val eventDesc: String
                val eventDateStr: String

                if (isOldFormat) {
                    eventTitle = parts.getOrNull(0) ?: "Termin"
                    eventDesc = parts.getOrNull(1) ?: ""
                    eventDateStr = parts.getOrNull(2) ?: ""
                } else {
                    // New format is just the text, but let's try to parse if it was structured somehow
                    // Actually, I changed it to: "${e.title}$timeStr$locStr\n${e.description}\nDatum: ${e.date}"
                    val lines = message.content.lines()
                    eventTitle = lines.getOrNull(0) ?: "Termin"
                    eventDesc = lines.getOrNull(1) ?: ""
                    eventDateStr = lines.getOrNull(2)?.removePrefix("Datum: ") ?: ""
                }

                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(bubbleShape)
                        .background(bubbleColor)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DateRange, contentDescription = "Termin", tint = textColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (eventDateStr.isNotBlank()) "$eventTitle am $eventDateStr" else eventTitle,
                                fontSize = 15.sp,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (eventDesc.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = eventDesc,
                                fontSize = 13.sp,
                                color = textColor.copy(alpha = 0.8f)
                            )
                        }

                        if (!message.isOwnMessage && eventDateStr.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    // Try to parse date if possible, otherwise use today or some fallback
                                    onAcceptEvent(eventTitle, eventDesc, eventDateStr)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = textColor
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Kalender hinzufügen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = time,
                            fontSize = 11.sp,
                            color = timeColor,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
            ChatMessageType.UNKNOWN -> {
                Box(
                    modifier =
                        Modifier
                            .widthIn(max = 280.dp)
                            .clip(bubbleShape)
                            .background(bubbleColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Nicht unterstützte Nachricht",
                        fontSize = 14.sp,
                        color = textColor,
                    )
                }
            }
        }

        if (message.isOwnMessage && message.deliveryState != ChatDeliveryState.SENT) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text =
                        when (message.deliveryState) {
                            ChatDeliveryState.PENDING -> "Wird gesendet …"
                            ChatDeliveryState.UPLOADING -> "Wird hochgeladen …"
                            ChatDeliveryState.FAILED -> "Senden fehlgeschlagen"
                            ChatDeliveryState.SENT -> ""
                        },
                    color =
                        if (message.deliveryState == ChatDeliveryState.FAILED) {
                            Color(0xFFB42318)
                        } else {
                            CrewspaceTextSecondary
                        },
                    fontSize = 11.sp,
                )
                if (message.deliveryState == ChatDeliveryState.FAILED) {
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                        Text("Erneut", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ── Sprachnachricht-Bubble ──────────────────────────────────

@Composable
private fun VoiceMessageBubble(
    durationSeconds: Int,
    time: String,
    isOwnMessage: Boolean,
    mediaSource: String?,
) {
    val context = LocalContext.current
    var player by remember(mediaSource) { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember(mediaSource) { mutableStateOf(false) }
    var isPreparing by remember(mediaSource) { mutableStateOf(false) }
    DisposableEffect(mediaSource) {
        onDispose {
            player?.release()
            player = null
        }
    }

    fun togglePlayback() {
        val source = mediaSource ?: return
        val activePlayer = player
        if (isPreparing) return
        if (activePlayer != null && isPlaying) {
            runCatching { activePlayer.pause() }
                .onSuccess { isPlaying = false }
            return
        }
        if (activePlayer != null) {
            runCatching { activePlayer.start() }
                .onSuccess { isPlaying = true }
            return
        }
        runCatching {
            android.media.MediaPlayer().also { newPlayer ->
                player = newPlayer
                isPreparing = true
                val uri = android.net.Uri.parse(source)
                if (uri.scheme == "http" || uri.scheme == "https") {
                    newPlayer.setDataSource(source)
                } else {
                    newPlayer.setDataSource(context, uri)
                }
                newPlayer.setOnPreparedListener {
                    isPreparing = false
                    it.start()
                    isPlaying = true
                }
                newPlayer.setOnCompletionListener {
                    isPlaying = false
                    it.seekTo(0)
                }
                newPlayer.setOnErrorListener { failedPlayer, _, _ ->
                    isPreparing = false
                    isPlaying = false
                    failedPlayer.release()
                    player = null
                    true
                }
                newPlayer.prepareAsync()
            }
        }.onFailure {
            Toast.makeText(context, "Audio konnte nicht abgespielt werden.", Toast.LENGTH_SHORT).show()
            player?.release()
            player = null
            isPreparing = false
        }
    }

    val bgColor = if (isOwnMessage) CrewspaceAccent else CrewspaceSurface
    val contentColor = if (isOwnMessage) Color.White else CrewspaceTextPrimary
    val timeColor = if (isOwnMessage) Color.White.copy(alpha = 0.7f) else CrewspaceTextSecondary
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val durationText = "%d:%02d".format(minutes, seconds)

    Column {
        Box(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                        bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                    )
                )
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play-Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f))
                        .clickable(enabled = mediaSource != null, onClick = ::togglePlayback),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Abspielen",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Waveform-Dummy
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(8, 14, 10, 16, 12, 8, 14, 10)
                    barHeights.forEach { height ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(height.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(contentColor.copy(alpha = 0.6f))
                        )
                    }
                }

                // Dauer
                Text(
                    text = durationText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }
        }

        // Zeit unter der Bubble
        Text(
            text = time,
            fontSize = 11.sp,
            color = timeColor,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
        )
    }
}

// ── Chat-Input-Leiste ──────────────────────────────────────

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: (String) -> Unit,
    onSendVoice: (String, Int) -> Unit,
) {
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<java.io.File?>(null) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var isRecording by remember { mutableStateOf(false) }

    @Suppress("DEPRECATION")
    fun startRecording() {
        if (isRecording) return
        val directory = java.io.File(context.cacheDir, "chat_recordings").apply { mkdirs() }
        val file = java.io.File(directory, "voice-${java.util.UUID.randomUUID()}.m4a")
        runCatching {
            val newRecorder =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    android.media.MediaRecorder(context)
                } else {
                    android.media.MediaRecorder()
                }
            newRecorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            newRecorder.setAudioEncodingBitRate(96_000)
            newRecorder.setAudioSamplingRate(44_100)
            newRecorder.setMaxDuration(900_000)
            newRecorder.setOutputFile(file.absolutePath)
            recorder = newRecorder
            recordingFile = file
            recordingStartedAt = android.os.SystemClock.elapsedRealtime()
            isRecording = true
            newRecorder.setOnInfoListener { completedRecorder, what, _ ->
                if (what == android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    val stopped = runCatching { completedRecorder.stop() }.isSuccess
                    completedRecorder.release()
                    recorder = null
                    recordingFile = null
                    isRecording = false
                    if (stopped && file.isFile) {
                        onSendVoice(android.net.Uri.fromFile(file).toString(), 900)
                    } else {
                        file.delete()
                    }
                }
            }
            newRecorder.prepare()
            newRecorder.start()
        }.onFailure {
            recorder?.release()
            recorder = null
            recordingFile = null
            recordingStartedAt = 0L
            isRecording = false
            file.delete()
            Toast.makeText(context, "Audioaufnahme konnte nicht gestartet werden.", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording(send: Boolean) {
        val activeRecorder = recorder ?: return
        val file = recordingFile
        val duration =
            ((android.os.SystemClock.elapsedRealtime() - recordingStartedAt + 999L) / 1_000L)
                .toInt()
                .coerceAtLeast(1)
        val stopped = runCatching { activeRecorder.stop() }.isSuccess
        activeRecorder.release()
        recorder = null
        recordingFile = null
        isRecording = false
        if (send && stopped && file?.isFile == true) {
            onSendVoice(android.net.Uri.fromFile(file).toString(), duration)
        } else {
            file?.delete()
        }
    }

    val audioPermissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                startRecording()
            } else {
                Toast.makeText(
                    context,
                    "Für Sprachnachrichten wird Mikrofonzugriff benötigt.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.let { active ->
                runCatching { active.stop() }
                active.release()
            }
            recorder = null
            recordingFile?.delete()
        }
    }

    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onAttachImage(it.toString()) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CrewspaceSurface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Galerie-Icon
        IconButton(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = "Galerie",
                tint = CrewspaceTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Statistik-Icon
        IconButton(
            onClick = { Toast.makeText(context, "Statistik noch nicht verfügbar", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = "Statistik",
                tint = CrewspaceTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Textfeld
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(CrewspaceBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "Nachricht",
                    fontSize = 15.sp,
                    color = CrewspaceTextSecondary.copy(alpha = 0.6f)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    color = CrewspaceTextPrimary
                ),
                cursorBrush = SolidColor(CrewspaceAccent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Mikrofon / Senden Button
        if (text.isBlank()) {
            IconButton(
                onClick = {
                    if (isRecording) {
                        stopRecording(send = true)
                    } else if (
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        startRecording()
                    } else {
                        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color(0xFFFF3B30) else CrewspaceAccent)
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Outlined.Mic,
                    contentDescription =
                        if (isRecording) "Aufnahme stoppen und senden" else "Sprachaufnahme starten",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CrewspaceAccent)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Senden",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Planung Tab – Vollständige Implementierung
// ══════════════════════════════════════════════════════════════

@Composable
private fun PlanungTabContent(uiState: CrewspaceUiState, viewModel: CrewspaceViewModel) {
    val scrollState = rememberScrollState()
    val eventsForDay = viewModel.eventsForSelectedDate()

    var eventToEdit by remember { mutableStateOf<PlannerEvent?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Kalender-Card ──
        CalendarCard(uiState = uiState, viewModel = viewModel)

        // ── Tages-Detail-Card ──
        DayDetailCard(
            selectedDate = uiState.selectedDate,
            events = eventsForDay,
            onAddEvent = {
                eventToEdit = PlannerEvent(date = uiState.selectedDate, title = "")
            },
            onDeleteEvent = { viewModel.deletePlannerEvent(it) },
            onEventClick = { eventToEdit = it }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (eventToEdit != null) {
        EditPlannerEventBottomSheet(
            event = eventToEdit!!,
            chatThreads = uiState.chatThreads,
            onDismiss = { eventToEdit = null },
            onSave = { updatedEvent ->
                if (uiState.plannerEvents.any { it.id == updatedEvent.id }) {
                    viewModel.updatePlannerEvent(updatedEvent)
                } else {
                    viewModel.addPlannerEvent(updatedEvent)
                }
                eventToEdit = null
            },
            onDelete = {
                viewModel.deletePlannerEvent(eventToEdit!!)
                eventToEdit = null
            },
            onShare = { threadId ->
                val e = eventToEdit!!
                // Formatierte Nachricht für den Chat - Wir nutzen wieder das Pipe-Format für die "Annehmen"-Logik,
                // aber erweitern es um die neuen Felder für die Anzeige.
                val timeStr = if (e.startTime != null) " (${e.startTime}${if (e.endTime != null) "-${e.endTime}" else ""})" else ""
                val locStr = if (e.location != null) " @ ${e.location}" else ""
                val displayTitle = "${e.title}$timeStr$locStr"

                val text = "$displayTitle|${e.description}|${e.date}"

                val thread = uiState.chatThreads.find { it.id == threadId }
                if (thread != null) {
                    viewModel.openChat(thread)
                    viewModel.sendAttachmentMessage(ChatMessageType.EVENT, text)
                    eventToEdit = null
                }
            }
        )
    }
}

// ── Custom Kalender-Card ──────────────────────────────────────

@Composable
private fun CalendarCard(uiState: CrewspaceUiState, viewModel: CrewspaceViewModel) {
    val yearMonth = YearMonth.from(uiState.currentMonth)
    val germanLocale = Locale.GERMAN
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, germanLocale)
        .replaceFirstChar { it.titlecase(germanLocale) }
    val year = yearMonth.year

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CrewspaceSurface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Monat-Navigation ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateMonth(false) },
                    modifier = Modifier.size(40.dp).background(CrewspaceCardBg, CircleShape)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = CrewspaceAccent)
                }

                Text(
                    text = "$monthName $year",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CrewspaceTextPrimary
                )

                IconButton(
                    onClick = { viewModel.navigateMonth(true) },
                    modifier = Modifier.size(40.dp).background(CrewspaceCardBg, CircleShape)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CrewspaceAccent)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Wochentag-Header ──
            val dayLabels = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayLabels.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrewspaceTextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Kalender-Grid ──
            val firstDayOfMonth = yearMonth.atDay(1)
            val daysInMonth = yearMonth.lengthOfMonth()
            val startDayOfWeek = firstDayOfMonth.dayOfWeek.value
            val totalCells = startDayOfWeek - 1 + daysInMonth
            val rows = (totalCells + 6) / 7
            val today = LocalDate.now()

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - (startDayOfWeek - 1) + 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..daysInMonth) {
                                val date = yearMonth.atDay(dayNumber)
                                val isSelected = date == uiState.selectedDate
                                val isToday = date == today
                                val hasEvents = uiState.plannerEvents.any { it.date == date }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> CrewspaceAccent
                                                isToday -> CrewspaceAccent.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { viewModel.selectDate(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> CrewspaceAccent
                                                else -> CrewspaceTextPrimary
                                            }
                                        )
                                        if (hasEvents && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(CrewspaceAccent)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Tages-Detail-Card ─────────────────────────────────────────

@Composable
private fun DayDetailCard(
    selectedDate: LocalDate,
    events: List<PlannerEvent>,
    onAddEvent: () -> Unit,
    onDeleteEvent: (PlannerEvent) -> Unit,
    onEventClick: (PlannerEvent) -> Unit
) {
    val germanLocale = Locale.GERMAN
    val dayOfWeek = selectedDate.dayOfWeek
        .getDisplayName(TextStyle.FULL, germanLocale)
        .replaceFirstChar { it.titlecase(germanLocale) }
    val day = selectedDate.dayOfMonth
    val month = selectedDate.month
        .getDisplayName(TextStyle.FULL, germanLocale)
        .replaceFirstChar { it.titlecase(germanLocale) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CrewspaceSurface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dayOfWeek,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrewspaceAccent,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$day. $month",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrewspaceTextPrimary
                    )
                }

                Button(
                    onClick = onAddEvent,
                    colors = ButtonDefaults.buttonColors(containerColor = CrewspaceAccent.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CrewspaceAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Termin", color = CrewspaceAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = CrewspaceDivider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            if (events.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CrewspaceCardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = CrewspaceTextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Alles ruhig heute",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrewspaceTextSecondary
                    )
                }
            } else {
                events.forEach { event ->
                    val categoryColor = when (event.category) {
                        "Navigation" -> Color(0xFF3B82F6)
                        "Verpflegung" -> Color(0xFF10B981)
                        "Landgang" -> Color(0xFFF59E0B)
                        else -> CrewspaceAccent
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onEventClick(event) },
                        color = CrewspaceCardBg,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = event.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrewspaceTextPrimary
                                    )
                                    if (event.startTime != null) {
                                        Text(
                                            text = " • ${event.startTime}",
                                            fontSize = 13.sp,
                                            color = CrewspaceAccent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                val subText = buildString {
                                    if (!event.location.isNullOrBlank()) {
                                        append(event.location)
                                        if (event.description.isNotBlank()) append(" • ")
                                    }
                                    append(event.description)
                                }

                                if (subText.isNotBlank()) {
                                    Text(
                                        text = subText,
                                        fontSize = 12.sp,
                                        color = CrewspaceTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteEvent(event) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Löschen",
                                    tint = Color(0xFFEF4444).copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Crew Tab – Vollständige Implementierung
// ══════════════════════════════════════════════════════════════

@Composable
private fun CrewTabContent(uiState: CrewspaceUiState, viewModel: CrewspaceViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Card 1: "X an Bord" Übersicht ──
        CrewOnBoardCard(uiState = uiState, viewModel = viewModel)

        // ── Card 2: "Crewmitglied hinzufügen" Formular ──
        CrewAddMemberCard(uiState = uiState, viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Card: "X an Bord" ──────────────────────────────────────────

@Composable
private fun CrewOnBoardCard(uiState: CrewspaceUiState, viewModel: CrewspaceViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CrewspaceSurface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CrewspaceAccent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${uiState.onBoardCount}",
                            color = CrewspaceAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Aktuell an Bord",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrewspaceTextPrimary
                    )
                }
            }

            if (uiState.crewMembers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CrewspaceDivider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                uiState.crewMembers.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CrewspaceAccent.copy(alpha = 0.05f))
                                .border(1.dp, CrewspaceAccent.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                color = CrewspaceAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrewspaceTextPrimary
                            )
                            Text(
                                text = member.crewRole.label,
                                fontSize = 13.sp,
                                color = CrewspaceTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (member.isOnBoard) Color(0xFF10B981)
                                    else Color(0xFFEF4444)
                                )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(
                            onClick = { viewModel.deleteCrew(member) },
                            modifier = Modifier.size(32.dp).background(CrewspaceCardBg, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Entfernen",
                                tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Noch keine Crewmitglieder hinzugefügt.",
                    fontSize = 14.sp,
                    color = CrewspaceTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// ── Card: "Crewmitglied hinzufügen" ────────────────────────────

@Composable
private fun CrewAddMemberCard(uiState: CrewspaceUiState, viewModel: CrewspaceViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CrewspaceSurface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CrewspaceAccent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        tint = CrewspaceAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Mitglied hinzufügen",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrewspaceTextPrimary
                )
            }

            HorizontalDivider(color = CrewspaceDivider, thickness = 1.dp)

            CrewspaceTextField(
                value = uiState.addName,
                onValueChange = { input ->
                    viewModel.updateAddName(ValidationUtils.sanitizeName(input))
                },
                placeholder = "Name des Crewmitglieds",
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )

            var showChatDropdown by remember { mutableStateOf(false) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    CrewspaceTextField(
                        value = uiState.addSkipperId,
                        onValueChange = { input ->
                            viewModel.updateAddSkipperId(ValidationUtils.sanitizeSkipperId(input))
                        },
                        placeholder = "Skipper-ID (für Direkt-Chat)",
                        leadingIcon = {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showChatDropdown = true },
                    modifier = Modifier
                        .size(52.dp)
                        .background(CrewspaceCardBg, RoundedCornerShape(16.dp))
                        .border(1.dp, CrewspaceDivider, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = "Aus Chat auswählen",
                        tint = CrewspaceAccent
                    )

                    DropdownMenu(
                        expanded = showChatDropdown,
                        onDismissRequest = { showChatDropdown = false },
                        modifier = Modifier.background(CrewspaceSurface)
                    ) {
                        val availableChats = uiState.chatThreads.filter { it.type == ChatThreadType.DIRECT }
                        if (availableChats.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Keine Chats vorhanden", color = CrewspaceTextSecondary) },
                                onClick = { showChatDropdown = false }
                            )
                        } else {
                            availableChats.forEach { thread ->
                                DropdownMenuItem(
                                    text = { Text(thread.participantName) },
                                    onClick = {
                                        viewModel.updateAddSkipperId(thread.participantSkipperId)
                                        viewModel.updateAddName(thread.participantName)
                                        showChatDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column {
                Text(
                    text = "ROLLE AN BORD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CrewspaceTextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                val mainRoles = listOf(CrewRole.SKIPPER, CrewRole.CO_SKIPPER, CrewRole.NAVIGATION)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mainRoles.forEach { role ->
                        val isSelected = uiState.addSelectedRole == role
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateAddSelectedRole(role) },
                            color = if (isSelected) CrewspaceAccent else CrewspaceCardBg,
                            shape = RoundedCornerShape(12.dp),
                            border = if (!isSelected) BorderStroke(1.dp, CrewspaceDivider) else null
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = role.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else CrewspaceTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            CrewspaceTextField(
                value = uiState.addEmergencyContact,
                onValueChange = { input ->
                    viewModel.updateAddEmergencyContact(ValidationUtils.sanitizeName(input))
                },
                placeholder = "Notfallkontakt (Name)",
                leadingIcon = {
                    Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )

            CrewspaceTextField(
                value = uiState.addPhone,
                onValueChange = { input ->
                    viewModel.updateAddPhone(ValidationUtils.sanitizePhone(input))
                },
                placeholder = "Notfall-Telefonnummer",
                keyboardType = KeyboardType.Phone,
                leadingIcon = {
                    Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )

            CrewspaceTextField(
                value = uiState.addMedicalNotes,
                onValueChange = { input ->
                    viewModel.updateAddMedicalNotes(ValidationUtils.sanitizeMedicalNotes(input))
                },
                placeholder = "Medizinische Hinweise / Allergien",
                leadingIcon = {
                    Icon(Icons.Outlined.MedicalServices, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            )

            Button(
                onClick = { viewModel.addCrewMember() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = uiState.addSkipperId.isNotBlank() || uiState.addName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrewspaceAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Crewmitglied hinzufügen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Wiederverwendbares TextField im Crewspace-Stil ─────────────

@Composable
private fun CrewspaceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = CrewspaceTextSecondary.copy(alpha = 0.5f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CrewspaceAccent,
            unfocusedBorderColor = CrewspaceDivider,
            focusedContainerColor = CrewspaceCardBg,
            unfocusedContainerColor = CrewspaceCardBg,
            cursorColor = CrewspaceAccent,
            focusedTextColor = CrewspaceTextPrimary,
            unfocusedTextColor = CrewspaceTextPrimary,
            focusedLeadingIconColor = CrewspaceAccent,
            unfocusedLeadingIconColor = CrewspaceTextSecondary
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    )
}

// ══════════════════════════════════════════════════════════════
