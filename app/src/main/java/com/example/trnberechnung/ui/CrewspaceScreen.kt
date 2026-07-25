package com.example.trnberechnung.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.trnberechnung.messaging.ChatNavigationState
import com.example.trnberechnung.model.*
import com.example.trnberechnung.viewmodel.CrewspaceViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ══════════════════════════════════════════════════════════════
// Crewspace Farbpalette (helles Design, wie in den Screenshots)
// ══════════════════════════════════════════════════════════════
private val CrewspaceBg = Color(0xFFF2F2F7)             // iOS-artiger Hintergrund
private val CrewspaceSurface = Color.White
private val CrewspacePrimary = Color(0xFF1B3A5C)         // Dunkles Marineblau (Titel)
private val CrewspaceAccent = Color(0xFF2563EB)          // Kräftiges Blau (Buttons, aktiver Tab)
private val CrewspaceTextPrimary = Color(0xFF1C1C1E)     // Fast-Schwarz
private val CrewspaceTextSecondary = Color(0xFF8E8E93)   // Grau
private val CrewspaceTabBg = Color(0xFFE5E5EA)           // Tab-Hintergrund (inaktiv)
private val CrewspaceTabActive = Color(0xFF1B3A5C)       // Aktiver Tab (dunkel)
private val CrewspaceTabActiveText = Color.White
private val CrewspaceTabInactiveText = Color(0xFF6B7280)
private val CrewspaceDivider = Color(0xFFE5E5EA)
private val CrewspaceCardBg = Color(0xFFF8F8FA)          // Leicht getönter Card-Hintergrund
private val CrewspaceUnreadBadge = Color(0xFF2563EB)

/**
 * Haupt-Composable für den gesamten Crewspace-Screen.
 * Enthält Header, Segmented Tabs und routet zu den drei Sub-Screens.
 */
@Composable
fun CrewspaceScreen(
    viewModel: CrewspaceViewModel,
    authRepo: AuthRepository? = null,
    onNavigateToLogin: () -> Unit = {}
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
        ChatDetailScreen(viewModel = viewModel, thread = uiState.activeChatThread!!)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CrewspaceBg)
    ) {
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
                CrewspaceTab.AI_ASSISTENT -> {
                    AiAssistentTabContent(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }

    // ── BottomSheet für neue Unterhaltung ──
    if (uiState.showNewConversationSheet) {
        NewConversationBottomSheet(uiState = uiState, viewModel = viewModel)
    }
}

// ══════════════════════════════════════════════════════════════
// Header: "Crewspace" Titel + Untertitel + Edit-Button
// ══════════════════════════════════════════════════════════════

@Composable
private fun CrewspaceHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Crewspace",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CrewspacePrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Crew, Gespräche und Termine an einem Ort",
                fontSize = 13.sp,
                color = CrewspaceTextSecondary
            )
        }

        // Edit-Button (oben rechts)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CrewspaceSurface)
                .border(1.dp, CrewspaceDivider, CircleShape)
                .clickable { /* Bearbeiten-Aktion */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Bearbeiten",
                tint = CrewspacePrimary,
                modifier = Modifier.size(18.dp)
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
    TabItem(CrewspaceTab.AI_ASSISTENT, Icons.Default.Star, "AI")
)

@Composable
private fun CrewspaceSegmentedTabs(
    selectedTab: CrewspaceTab,
    onTabSelected: (CrewspaceTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CrewspaceTabBg)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tabItems.forEach { item ->
            val isSelected = selectedTab == item.tab

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) CrewspaceTabActive else Color.Transparent
                    )
                    .clickable { onTabSelected(item.tab) }
                    .padding(vertical = 8.dp),
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
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) CrewspaceTabActiveText else CrewspaceTabInactiveText
                    )
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CrewspaceSurface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CrewspaceAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = CrewspaceAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name + letzte Nachricht
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.participantName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrewspaceTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = thread.lastMessage.ifBlank { "Keine Nachrichten" },
                    fontSize = 13.sp,
                    color = CrewspaceTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Zeit + Ungelesen-Badge
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = time,
                    fontSize = 12.sp,
                    color = CrewspaceTextSecondary
                )
                if (thread.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
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

            Spacer(modifier = Modifier.width(4.dp))
            Text("›", fontSize = 18.sp, color = CrewspaceTextSecondary.copy(alpha = 0.4f))
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
                        onValueChange = { viewModel.updateNewConversationSkipperId(it) },
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
                            val parsedDate = LocalDate.parse(dateStr)
                            viewModel.addPlannerEventWithDate(title, desc, parsedDate)
                        } catch (e: Exception) {
                            // ignore if date is invalid
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
                val eventTitle = parts.getOrNull(0) ?: "Termin"
                val eventDesc = parts.getOrNull(1) ?: ""
                val eventDateStr = parts.getOrNull(2) ?: ""
                
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
                                text = "$eventTitle am $eventDateStr",
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
                        
                        if (!message.isOwnMessage) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onAcceptEvent(eventTitle, eventDesc, eventDateStr) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = textColor
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Annehmen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
            onAddEvent = { viewModel.addPlannerEvent("Neuer Termin") },
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
            onSave = { newTitle, newDesc -> 
                viewModel.updatePlannerEvent(eventToEdit!!, newTitle, newDesc)
                eventToEdit = null
            },
            onDelete = {
                viewModel.deletePlannerEvent(eventToEdit!!)
                eventToEdit = null
            },
            onShare = { threadId ->
                val text = "${eventToEdit!!.title}|${eventToEdit!!.description}|${eventToEdit!!.date}"
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CrewspaceSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Monat-Navigation ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateMonth(false) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("◀", fontSize = 14.sp, color = CrewspaceAccent)
                }

                Text(
                    text = "$monthName $year",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrewspaceTextPrimary
                )

                IconButton(
                    onClick = { viewModel.navigateMonth(true) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("▶", fontSize = 14.sp, color = CrewspaceAccent)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CrewspaceTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Kalender-Grid ──
            val firstDayOfMonth = yearMonth.atDay(1)
            val daysInMonth = yearMonth.lengthOfMonth()
            // Montag = 1 ... Sonntag = 7
            val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Mo ... 7=So
            val totalCells = startDayOfWeek - 1 + daysInMonth
            val rows = (totalCells + 6) / 7
            val today = LocalDate.now()

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> CrewspaceAccent
                                                isToday -> CrewspaceAccent.copy(alpha = 0.12f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { viewModel.selectDate(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            fontSize = 14.sp,
                                            fontWeight = when {
                                                isSelected -> FontWeight.Bold
                                                isToday -> FontWeight.SemiBold
                                                else -> FontWeight.Normal
                                            },
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> CrewspaceAccent
                                                else -> CrewspaceTextPrimary
                                            }
                                        )
                                        // Event-Indikator (kleiner Punkt)
                                        if (hasEvents && !isSelected) {
                                            Box(
                                                modifier = Modifier
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CrewspaceSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Tag-Titel + "+ Termin" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dayOfWeek, $day. $month",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrewspaceTextPrimary
                )

                TextButton(
                    onClick = onAddEvent,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+ Termin",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrewspaceAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = CrewspaceDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            if (events.isEmpty()) {
                // ── Empty State ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = CrewspaceTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Noch nichts geplant",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = CrewspaceTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tippe auf \"+ Termin\", um einen Eintrag hinzuzufügen.",
                        fontSize = 12.sp,
                        color = CrewspaceTextSecondary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // ── Event-Liste ──
                events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEventClick(event) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Farbiger Balken links
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CrewspaceAccent)
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = CrewspaceTextPrimary
                            )
                            if (event.description.isNotBlank()) {
                                Text(
                                    text = event.description,
                                    fontSize = 12.sp,
                                    color = CrewspaceTextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteEvent(event) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Löschen",
                                tint = CrewspaceTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CrewspaceSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header-Zeile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Kreis mit Anzahl
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CrewspaceAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${uiState.onBoardCount}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "an Bord",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrewspaceTextPrimary
                    )
                }
            }

            // Crewmitglieder-Liste
            if (uiState.crewMembers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CrewspaceDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                uiState.crewMembers.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        Spacer(modifier = Modifier.width(12.dp))

                        // Name und Rolle
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = CrewspaceTextPrimary
                            )
                            Text(
                                text = member.crewRole.label,
                                fontSize = 12.sp,
                                color = CrewspaceTextSecondary
                            )
                        }

                        // Status-Indikator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (member.isOnBoard) Color(0xFF34C759)
                                    else Color(0xFFFF3B30)
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Löschen-Button
                        IconButton(
                            onClick = { viewModel.deleteCrew(member) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Entfernen",
                                tint = CrewspaceTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Empty-State
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Noch keine Crewmitglieder hinzugefügt.",
                    fontSize = 13.sp,
                    color = CrewspaceTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ── Card: "Crewmitglied hinzufügen" ────────────────────────────

@Composable
private fun CrewAddMemberCard(uiState: CrewspaceUiState, viewModel: CrewspaceViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CrewspaceSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Titel
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.PersonAdd,
                    contentDescription = null,
                    tint = CrewspaceAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Crewmitglied hinzufügen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrewspaceTextPrimary
                )
            }

            HorizontalDivider(color = CrewspaceDivider, thickness = 0.5.dp)

            // ── Name Feld ──
            CrewspaceTextField(
                value = uiState.addName,
                onValueChange = { viewModel.updateAddName(it) },
                placeholder = "Name (Optional, falls ID vorhanden)",
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = CrewspaceTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // ── Skipper-ID Feld ──
            var showChatDropdown by remember { mutableStateOf(false) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    CrewspaceTextField(
                        value = uiState.addSkipperId,
                        onValueChange = { viewModel.updateAddSkipperId(it) },
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
                
                // Chat-Auswahl-Button
                Box {
                    IconButton(
                        onClick = { showChatDropdown = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(CrewspaceCardBg, RoundedCornerShape(10.dp))
                            .border(1.dp, CrewspaceDivider, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "Aus Chat auswählen",
                            tint = CrewspaceAccent
                        )
                    }

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

            // ── Rollen-Auswahl (Filter-Chips) ──
            Column {
                Text(
                    text = "ROLLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrewspaceTextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val mainRoles = listOf(CrewRole.SKIPPER, CrewRole.CO_SKIPPER, CrewRole.NAVIGATION)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mainRoles.forEach { role ->
                        val isSelected = uiState.addSelectedRole == role
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) CrewspaceAccent
                                    else CrewspaceCardBg
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CrewspaceAccent else CrewspaceDivider,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.updateAddSelectedRole(role) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = role.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else CrewspaceTextPrimary
                            )
                        }
                    }
                }
            }

            // ── Notfallkontakt ──
            CrewspaceTextField(
                value = uiState.addEmergencyContact,
                onValueChange = { viewModel.updateAddEmergencyContact(it) },
                placeholder = "Notfallkontakt",
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = CrewspaceTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // ── Telefon ──
            CrewspaceTextField(
                value = uiState.addPhone,
                onValueChange = { viewModel.updateAddPhone(it) },
                placeholder = "Telefon",
                keyboardType = KeyboardType.Phone,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = CrewspaceTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // ── Medizinische Hinweise ──
            CrewspaceTextField(
                value = uiState.addMedicalNotes,
                onValueChange = { viewModel.updateAddMedicalNotes(it) },
                placeholder = "Medizinische Hinweise",
                leadingIcon = {
                    Icon(
                        Icons.Outlined.MedicalServices,
                        contentDescription = null,
                        tint = CrewspaceTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // ── Hinzufügen-Button ──
            Button(
                onClick = { viewModel.addCrewMember() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = uiState.addSkipperId.isNotBlank() || uiState.addName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrewspaceAccent,
                    contentColor = Color.White,
                    disabledContainerColor = CrewspaceDivider,
                    disabledContentColor = CrewspaceTextSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Hinzufügen",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
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
                color = CrewspaceTextSecondary.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CrewspaceAccent,
            unfocusedBorderColor = CrewspaceDivider,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = CrewspaceCardBg,
            cursorColor = CrewspaceAccent,
            focusedTextColor = CrewspaceTextPrimary,
            unfocusedTextColor = CrewspaceTextPrimary
        )
    )
}

// ══════════════════════════════════════════════════════════════
// AI Assistent Tab – Gemini 2.5 Flash Chat
// ══════════════════════════════════════════════════════════════

private val AiAccentColor = Color(0xFF8B5CF6)    // Lila-Akzent für AI
private val AiBubbleUser = Color(0xFF2563EB)      // Blau (User)
private val AiBubbleAi = Color(0xFFF3F0FF)        // Helles Lila (AI)
private val AiTextOnBubbleAi = Color(0xFF1C1C1E)  // Dunkel auf hellem Hintergrund

@Composable
fun AiAssistentTabContent(
    uiState: CrewspaceUiState,
    viewModel: CrewspaceViewModel
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Auto-scroll wenn neue Nachricht kommt
    LaunchedEffect(uiState.aiMessages.size) {
        if (uiState.aiMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.aiMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CrewspaceBg)
    ) {
        // ── Willkommens-Header ──
        if (uiState.aiMessages.isEmpty()) {
            AiWelcomeHeader()
        }

        // ── Nachrichten-Liste ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(uiState.aiMessages) { message ->
                AiChatBubble(message = message)
            }

            // Loading-Indikator
            if (uiState.aiIsLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(AiBubbleAi)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AiAccentColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Denkt nach…",
                                color = CrewspaceTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Schnellvorschläge (wenn noch keine Nachrichten) ──
        if (uiState.aiMessages.isEmpty() && !uiState.aiIsLoading) {
            AiQuickSuggestions(onSuggestionClick = { suggestion ->
                viewModel.updateAiInput(suggestion)
                viewModel.sendAiMessage()
            })
        }

        // ── Input-Leiste ──
        AiInputBar(
            input = uiState.aiInput,
            isLoading = uiState.aiIsLoading,
            onInputChange = { viewModel.updateAiInput(it) },
            onSend = {
                viewModel.sendAiMessage()
                focusManager.clearFocus()
            }
        )
    }
}

@Composable
private fun AiWelcomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(AiAccentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "AI",
                tint = AiAccentColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Törn-Assistent",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = CrewspacePrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Powered by Gemini 2.5 Flash",
            fontSize = 12.sp,
            color = AiAccentColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Frag mich alles rund ums Segeln, Navigation,\nWetter, Gezeiten und Revierplanung!",
            fontSize = 14.sp,
            color = CrewspaceTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun AiQuickSuggestions(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        "⛵ Wie bereite ich mich auf eine Nordsee-Überfahrt vor?",
        "🌊 Erkläre mir die Gezeitenberechnung",
        "☀️ Tipps für Segeln bei Starkwind",
        "🗺️ Beste Segelreviere in der Ostsee"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        suggestions.forEach { suggestion ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClick(suggestion) },
                shape = RoundedCornerShape(12.dp),
                color = CrewspaceSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CrewspaceDivider)
            ) {
                Text(
                    text = suggestion,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 13.sp,
                    color = CrewspaceTextPrimary
                )
            }
        }
    }
}

@Composable
private fun AiChatBubble(message: AiChatMessage) {
    val isUser = message.isFromUser
    val alignment = if (isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AiAccentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "AI",
                    tint = AiAccentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) AiBubbleUser else AiBubbleAi)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = if (isUser) Color.White else AiTextOnBubbleAi,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AiInputBar(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CrewspaceSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = {
                    Text("Frag den Törn-Assistenten…", color = CrewspaceTextSecondary, fontSize = 14.sp)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AiAccentColor,
                    unfocusedBorderColor = CrewspaceDivider,
                    focusedContainerColor = CrewspaceCardBg,
                    unfocusedContainerColor = CrewspaceCardBg,
                    cursorColor = AiAccentColor,
                    focusedTextColor = CrewspaceTextPrimary,
                    unfocusedTextColor = CrewspaceTextPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (input.isNotBlank() && !isLoading) AiAccentColor
                        else CrewspaceDivider
                    )
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
