package com.example.trnberechnung.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.database.SeafarerMessageEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tab enum for the Seefahrer-Nachrichten panel.
 */
enum class SeafarerTab(val label: String) {
    UNREAD("Ungelesen"),
    CURRENT("Aktuell"),
    ARCHIVE("Archiv")
}

/**
 * Seefahrer-Nachrichten (Nautical Messages) panel for the home screen.
 * Includes tabs for Unread/Current/Archive, a search bar, and message list.
 *
 * Connected to real BfS data from the BSH ELWIS API via TideViewModel.
 */
@Composable
fun SeafarerMessagesPanel(
    unreadMessages: List<SeafarerMessageEntity>,
    allMessages: List<SeafarerMessageEntity>,
    archivedMessages: List<SeafarerMessageEntity>,
    unreadCount: Int,
    onDoneClick: () -> Unit,
    onMessageClick: (SeafarerMessageEntity) -> Unit = {},
    onArchiveMessage: (String) -> Unit = {},
    onSearchQuery: (String) -> Unit = {},
    searchResults: List<SeafarerMessageEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SeafarerTab.UNREAD) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphismCard(cornerRadius = 16.dp, elevation = 6.dp)
    ) {
        // ── Drag handle ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TideNodeTextTertiary)
            )
        }

        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = TideNodeTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Seefahrer-Nachrichten",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TideNodeTextPrimary,
                modifier = Modifier.weight(1f)
            )
            // "Fertig" button
            TextButton(onClick = onDoneClick) {
                Text(
                    text = "Fertig",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TideNodeBlue
                )
            }
        }

        // ── Tab row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TideNodeTabBg)
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SeafarerTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TideNodeTabActiveBg else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) TideNodeTextPrimary else TideNodeTextSecondary
                        )
                        // Show unread badge on first tab
                        if (tab == SeafarerTab.UNREAD && unreadCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(TideNodeBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Search bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TideNodeSearchBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Suchen",
                tint = TideNodeTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Simple search placeholder (tappable, or could be a TextField)
            if (searchQuery.isEmpty()) {
                Text(
                    text = "Meldung, Ort oder BfS-Nummer",
                    fontSize = 14.sp,
                    color = TideNodeTextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Message content ──
        val displayMessages = when (selectedTab) {
            SeafarerTab.UNREAD -> unreadMessages
            SeafarerTab.CURRENT -> allMessages
            SeafarerTab.ARCHIVE -> archivedMessages
        }

        if (displayMessages.isEmpty()) {
            // "Alles gelesen" empty state
            AllReadCard()
        } else {
            // Message list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(horizontal = 16.dp)
            ) {
                displayMessages.take(10).forEach { message ->
                    SeafarerMessageCard(
                        message = message,
                        onClick = { onMessageClick(message) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * "Alles gelesen" (All read) empty state card.
 */
@Composable
private fun AllReadCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TideNodeTabBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = TideNodeBlue,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Alles gelesen",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TideNodeTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Neue amtliche BfS erscheinen hier automatisch.",
            fontSize = 13.sp,
            color = TideNodeTextSecondary,
            lineHeight = 18.sp
        )
    }
}

/**
 * Individual message card in the seafarer messages list.
 */
@Composable
private fun SeafarerMessageCard(
    message: SeafarerMessageEntity,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TideNodeSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Unread dot
        if (!message.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = 6.dp)
                    .clip(CircleShape)
                    .background(TideNodeBlue)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = message.title,
                    fontSize = 14.sp,
                    fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.Normal,
                    color = TideNodeTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dateFormat.format(Date(message.publishedAt)),
                    fontSize = 11.sp,
                    color = TideNodeTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${message.area} · ${message.bfsNumber}",
                fontSize = 12.sp,
                color = TideNodeTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (message.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content,
                    fontSize = 12.sp,
                    color = TideNodeTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
