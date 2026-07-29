package com.example.trnberechnung.ui.notices

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.trnberechnung.database.SeafarerMessageEntity
import com.example.trnberechnung.repository.chartReferences
import com.example.trnberechnung.repository.previousNotices
import com.example.trnberechnung.ui.components.TideNodeBlue
import com.example.trnberechnung.ui.components.TideNodeInk
import com.example.trnberechnung.ui.components.tideNodeGlass
import com.example.trnberechnung.viewmodel.MaritimeNoticeViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class NoticeFilter(val label: String) {
    UNREAD("Ungelesen"),
    CURRENT("Aktuell"),
    ARCHIVE("Archiv"),
}

@Composable
fun MaritimeNoticeQuickLook(
    viewModel: MaritimeNoticeViewModel,
    onDismiss: () -> Unit,
    onShowAll: () -> Unit,
) {
    val unread by viewModel.unread.collectAsState()
    val current by viewModel.current.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val preview =
        remember(unread, current) {
            (unread + current)
                .distinctBy(SeafarerMessageEntity::id)
                .sortedWith(
                    compareByDescending<SeafarerMessageEntity> { it.readRevision < it.revision }
                        .thenByDescending { it.updatedAt },
                )
                .take(3)
        }
    val total = (unread + current).distinctBy(SeafarerMessageEntity::id).size

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val titleColor = if (isDark) Color(0xFFF8FAFC) else TideNodeInk
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF72757B)
    val accentColor = if (isDark) Color(0xFF60A5FA) else TideNodeBlue

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(-16, 104),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier =
                Modifier
                    .width(350.dp)
                    .tideNodeGlass(cornerRadius = 30.dp, elevation = 14.dp, alpha = 0.90f)
                    .padding(18.dp)
                    .testTag("maritime_notice_quicklook"),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, null, tint = accentColor)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Seefahrer-Nachrichten",
                        color = titleColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                    )
                    Text(
                        if (unreadCount == 0) "Keine ungelesenen Meldungen" else "$unreadCount ungelesen",
                        color = subtitleColor,
                        fontSize = 13.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color.Black.copy(alpha = 0.08f))

            if (preview.isEmpty()) {
                Text(
                    "Derzeit sind keine Meldungen zwischengespeichert.",
                    color = subtitleColor,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            } else {
                preview.forEach { notice ->
                    MaritimeNoticeCompactRow(
                        notice = notice,
                        onClick = {
                            viewModel.openDetail(notice)
                            onDismiss()
                            onShowAll()
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0x1524579F))
                        .clickable {
                            onDismiss()
                            onShowAll()
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Mehr anzeigen",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (total > preview.size) {
                    Text("${total - preview.size}", color = subtitleColor)
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accentColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaritimeNoticesSheet(
    viewModel: MaritimeNoticeViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val unread by viewModel.unread.collectAsState()
    val current by viewModel.current.collectAsState()
    val archive by viewModel.archive.collectAsState()
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val metadata by viewModel.syncMetadata.collectAsState()
    var filter by rememberSaveableNoticeFilter()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    LaunchedEffect(Unit) { viewModel.refresh(force = false) }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.closeDetail()
            onDismiss()
        },
        containerColor = if (isDark) Color(0xFF0D1B2A) else Color(0xFFF1F3F8),
        contentColor = if (isDark) Color(0xFFF8FAFC) else TideNodeInk,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB8BBC2)),
            )
        },
        modifier = Modifier.testTag("maritime_notice_sheet"),
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.94f)) {
            if (uiState.selectedSummary != null) {
                MaritimeNoticeDetail(
                    summary = uiState.selectedSummary!!,
                    detail = uiState.selectedDetail,
                    isLoading = uiState.detailLoading,
                    error = uiState.error,
                    onBack = viewModel::closeDetail,
                )
            } else {
                val unfiltered =
                    when (filter) {
                        NoticeFilter.UNREAD -> unread
                        NoticeFilter.CURRENT -> current
                        NoticeFilter.ARCHIVE -> archive
                    }
                val filteredIds = searchResults.asSequence().map { it.id }.toSet()
                val notices =
                    if (query.isBlank()) unfiltered else unfiltered.filter { it.id in filteredIds }

                MaritimeNoticeList(
                    notices = notices,
                    filter = filter,
                    onFilter = { filter = it },
                    query = query,
                    onQuery = viewModel::updateQuery,
                    isRefreshing = uiState.isRefreshing,
                    error = uiState.error,
                    isStale = metadata?.isStale == true,
                    lastUpdated = metadata?.lastIngestedAt ?: metadata?.fetchedAt,
                    onRefresh = { viewModel.refresh(force = true) },
                    onMarkAllRead = viewModel::markAllRead,
                    onNotice = viewModel::openDetail,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun rememberSaveableNoticeFilter(): androidx.compose.runtime.MutableState<NoticeFilter> =
    androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(NoticeFilter.CURRENT)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaritimeNoticeList(
    notices: List<SeafarerMessageEntity>,
    filter: NoticeFilter,
    onFilter: (NoticeFilter) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    isRefreshing: Boolean,
    error: String?,
    isStale: Boolean,
    lastUpdated: Long?,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onNotice: (SeafarerMessageEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMarkAllRead) {
                Icon(Icons.Default.CheckCircle, "Alle als gelesen markieren", tint = Color(0xFF9A9EA5))
            }
            Text(
                "Seefahrer-Nachrichten",
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("Fertig", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            NoticeFilter.entries.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = value == filter,
                    onClick = { onFilter(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, NoticeFilter.entries.size),
                    label = { Text(value.label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Meldung, Ort oder BfS-Nummer") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFE4E6EA),
                    unfocusedContainerColor = Color(0xFFE4E6EA),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TideNodeInk,
                    unfocusedTextColor = TideNodeInk,
                ),
            modifier = Modifier.fillMaxWidth().testTag("maritime_notice_search"),
        )

        if (isStale) {
            Text(
                "⌁  Offline-Daten. Der Stand kann veraltet sein.",
                color = Color(0xFFFF7A00),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                lastUpdated?.let { "Stand: ${it.formatNoticeTimestamp()}" } ?: "Noch nicht aktualisiert",
                color = Color(0xFF858991),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, "Nachrichten aktualisieren")
                }
            }
        }
        error?.let {
            Text(it, color = Color(0xFFB91C1C), fontSize = 13.sp)
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                if (notices.isEmpty()) {
                    item {
                        Text(
                            "Keine Meldungen in diesem Bereich.",
                            color = Color(0xFF858991),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        )
                    }
                }
                items(notices, key = SeafarerMessageEntity::id) { notice ->
                    MaritimeNoticeRow(notice, onClick = { onNotice(notice) })
                }
                item {
                    Text(
                        "Amtliche BfS sind eine Informationsquelle. Prüfe vor der Fahrt zusätzlich die gültigen Bekanntmachungen und nautischen Unterlagen.",
                        color = Color(0xFF858991),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MaritimeNoticeCompactRow(
    notice: SeafarerMessageEntity,
    onClick: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val titleColor = if (isDark) Color(0xFFF8FAFC) else TideNodeInk
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF73777E)
    val accentColor = if (isDark) Color(0xFF60A5FA) else TideNodeBlue

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        NoticeIcon(notice, 48)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                notice.bfsNumber,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text(
                notice.title,
                color = titleColor,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(notice.updatedAt.relativeAge(), color = subtitleColor, fontSize = 12.sp)
    }
}

@Composable
private fun MaritimeNoticeRow(
    notice: SeafarerMessageEntity,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.55f))
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        NoticeIcon(notice, 52)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notice.bfsNumber,
                    color = TideNodeBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    notice.publishedAt?.formatNoticeDate().orEmpty(),
                    color = Color(0xFF858991),
                    fontSize = 12.sp,
                )
            }
            Text(
                notice.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                notice.stateLabel(),
                color = Color(0xFF858991),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFB6BAC0))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaritimeNoticeDetail(
    summary: SeafarerMessageEntity,
    detail: SeafarerMessageEntity?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val notice = detail ?: summary
    val officialUrl = remember(notice.sourceUrl) { notice.sourceUrl.validElwisUriOrNull() }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.White.copy(alpha = 0.72f), CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
            }
            Text(
                notice.bfsNumber,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color.White.copy(alpha = 0.65f))
                        .padding(24.dp),
                ) {
                    Text(
                        notice.stateLabel().uppercase(Locale.GERMANY),
                        color = TideNodeBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        notice.title,
                        fontSize = 25.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        notice.publisher,
                        color = Color(0xFF777B82),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color.White.copy(alpha = 0.65f))
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    NoticeFact(Icons.Default.Map, "Gebiet", notice.regionPath.replace(".", " · "))
                    NoticeFact(Icons.Default.DateRange, "Gültig ab", notice.validFrom?.formatNoticeTimestamp() ?: "Nicht angegeben")
                    NoticeFact(Icons.Default.DateRange, "Gültig bis", notice.validUntil?.formatNoticeTimestamp() ?: "Bis auf Weiteres")
                }
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            error?.let {
                item {
                    Text(it, color = Color(0xFFB91C1C))
                }
            }
            if (notice.body.isNotBlank()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.62f))
                            .padding(20.dp),
                    ) {
                        Text("Meldung", fontWeight = FontWeight.Bold, color = TideNodeBlue)
                        Spacer(Modifier.height(8.dp))
                        Text(notice.body, lineHeight = 21.sp)
                    }
                }
            }
            if (notice.chartReferences().isNotEmpty()) {
                item {
                    DetailChips("Kartenverweise", notice.chartReferences())
                }
            }
            if (notice.previousNotices().isNotEmpty()) {
                item {
                    DetailChips("Vorgängermeldungen", notice.previousNotices())
                }
            }
            officialUrl?.let { uri ->
                item {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = TideNodeBlue,
                                contentColor = Color.White,
                            ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Offizielle ELWIS-Seite öffnen", fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeIcon(
    notice: SeafarerMessageEntity,
    size: Int,
) {
    val icon =
        if (notice.isTemporary) Icons.Default.Error else Icons.Default.Campaign
    Box(
        modifier =
            Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Color(0x1824579F)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = TideNodeBlue, modifier = Modifier.size((size * 0.48f).dp))
    }
}

@Composable
private fun NoticeFact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = TideNodeBlue)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = Color(0xFF7C8087), fontSize = 13.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailChips(
    title: String,
    values: List<String>,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.62f))
            .padding(20.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = TideNodeBlue)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach {
                Text(
                    it,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1324579F))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun SeafarerMessageEntity.stateLabel(): String =
    when (publicationState) {
        SeafarerMessageEntity.PUBLICATION_UPDATED -> "Geändert"
        SeafarerMessageEntity.PUBLICATION_REVOKED -> "Aufgehoben"
        SeafarerMessageEntity.PUBLICATION_EXPIRED -> "Abgelaufen"
        else -> "Aktuell"
    }

private fun String?.validElwisUriOrNull(): Uri? {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return null
    val host = uri.host?.lowercase(Locale.ROOT) ?: return null
    return uri.takeIf {
        it.scheme.equals("https", ignoreCase = true) &&
            (host == "elwis.de" || host.endsWith(".elwis.de"))
    }
}

private val noticeDateFormatter =
    DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMANY)
        .withZone(ZoneId.of("Europe/Berlin"))
private val noticeTimestampFormatter =
    DateTimeFormatter.ofPattern("d. MMM yyyy 'um' HH:mm", Locale.GERMANY)
        .withZone(ZoneId.of("Europe/Berlin"))

private fun Long.formatNoticeDate(): String = noticeDateFormatter.format(Instant.ofEpochMilli(this))

private fun Long.formatNoticeTimestamp(): String =
    noticeTimestampFormatter.format(Instant.ofEpochMilli(this))

private fun Long.relativeAge(now: Long = System.currentTimeMillis()): String {
    val hours = ((now - this).coerceAtLeast(0L) / 3_600_000L)
    return when {
        hours < 1 -> "Jetzt"
        hours < 24 -> "$hours Std."
        else -> "${hours / 24} Tg."
    }
}
