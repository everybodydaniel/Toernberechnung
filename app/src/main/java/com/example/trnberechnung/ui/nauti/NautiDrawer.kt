package com.example.trnberechnung.ui.nauti

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.trnberechnung.database.NautiConversationEntity
import com.example.trnberechnung.database.NautiMessageEntity
import com.example.trnberechnung.nauti.NautiActionCodec
import com.example.trnberechnung.repository.NautiConversationRepository
import com.example.trnberechnung.ui.theme.TideNodeBlue
import com.example.trnberechnung.ui.theme.TideNodeCyan
import com.example.trnberechnung.ui.theme.TideNodeInk
import com.example.trnberechnung.ui.components.tideNodeGlass
import com.example.trnberechnung.viewmodel.NautiPanelMode
import com.example.trnberechnung.viewmodel.NautiViewModel
import java.util.Locale

@Composable
fun NautiDrawer(
    viewModel: NautiViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val conversations by viewModel.filteredConversations.collectAsStateWithLifecycle()
    val allConversations by viewModel.conversations.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()

    when (uiState.mode) {
        NautiPanelMode.COMPACT -> {
            NautiCompactPill(
                onClick = viewModel::showChat,
                modifier = modifier.testTag("NautiInlinePanel"),
            )
        }

        NautiPanelMode.CHAT,
        NautiPanelMode.HISTORY,
        -> {
            Column(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.76f)
                        .heightIn(min = 420.dp)
                        .tideNodeGlass(cornerRadius = 30.dp, elevation = 16.dp, alpha = 0.80f)
                        .testTag("NautiInlinePanel"),
            ) {
                NautiDragHandle(viewModel::showCompact)
                if (uiState.mode == NautiPanelMode.HISTORY) {
                    NautiHistory(
                        conversations = conversations,
                        query = uiState.historyQuery,
                        onQuery = viewModel::updateHistoryQuery,
                        onBack = viewModel::showChat,
                        onNew = viewModel::createNewChat,
                        onSelect = viewModel::selectConversation,
                        onRename = viewModel::renameConversation,
                        onPin = viewModel::setPinned,
                        onDelete = viewModel::deleteConversation,
                    )
                } else {
                    val active =
                        allConversations.firstOrNull { it.id == uiState.activeConversationId }
                    NautiChat(
                        title = active?.title ?: "Neuer Chat",
                        messages = messages,
                        draft = uiState.draft,
                        isSending = uiState.isSending,
                        error = uiState.error,
                        onHistory = viewModel::showHistory,
                        onNew = viewModel::createNewChat,
                        onCollapse = viewModel::showCompact,
                        onDraft = viewModel::updateDraft,
                        onSend = viewModel::send,
                        onConfirmAction = viewModel::confirmAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun NautiCompactPill(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val titleColor = if (isDark) Color(0xFFF8FAFC) else TideNodeInk
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF62666C)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(68.dp)
                .tideNodeGlass(cornerRadius = 34.dp, elevation = 10.dp, alpha = 0.85f)
                .clickable(onClick = onClick)
                .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.AutoAwesome, null, tint = TideNodeCyan, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Nauti KI", color = titleColor, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Text("Törn, Wetter oder Gezeiten", color = subtitleColor, fontSize = 13.sp)
        }
        Text("›", color = TideNodeCyan, fontWeight = FontWeight.Bold, fontSize = 30.sp)
    }
}

@Composable
private fun NautiDragHandle(onCollapse: () -> Unit) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            if (amount > 0) dragDistance += amount
                        },
                        onDragEnd = {
                            if (dragDistance > 70f) onCollapse()
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                }
                .testTag("NautiInlineDragHandle"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 42.dp, height = 5.dp)
                .clip(CircleShape)
                .background(Color(0xFF9CA0A6).copy(alpha = 0.72f)),
        )
    }
}

@Composable
private fun NautiChat(
    title: String,
    messages: List<NautiMessageEntity>,
    draft: String,
    isSending: Boolean,
    error: String?,
    onHistory: () -> Unit,
    onNew: () -> Unit,
    onCollapse: () -> Unit,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onConfirmAction: (NautiMessageEntity) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val iconBg = if (isDark) Color(0xFF1E293B) else Color.White.copy(alpha = 0.72f)
    val titleColor = if (isDark) Color.White else TideNodeInk
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF74787E)

    Column(Modifier.fillMaxSize().testTag("NautiInlineChat")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onHistory) {
                Icon(Icons.Default.History, "Verlauf", tint = subtitleColor)
            }
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = TideNodeCyan)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = titleColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Skipper-KI", color = subtitleColor, fontSize = 12.sp)
            }
            IconButton(onClick = onNew) {
                Icon(Icons.Default.Edit, "Neuer Chat", tint = subtitleColor)
            }
            IconButton(onClick = onCollapse) {
                Icon(Icons.Default.ExpandMore, "Chat einklappen", tint = subtitleColor)
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.08f)))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages, key = NautiMessageEntity::id) { message ->
                NautiMessageBubble(message, onConfirmAction)
            }
            if (isSending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = TideNodeCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Nauti denkt nach…", color = Color(0xFF74787E), fontSize = 13.sp)
                    }
                }
            }
        }

        error?.let {
            Text(
                it,
                color = Color(0xFFB91C1C),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )
        }
        NautiComposer(
            draft = draft,
            enabled = !isSending,
            onDraft = onDraft,
            onSend = onSend,
        )
    }
}

@Composable
private fun NautiMessageBubble(
    message: NautiMessageEntity,
    onConfirmAction: (NautiMessageEntity) -> Unit,
) {
    val fromUser = message.role == NautiConversationRepository.ROLE_USER
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bubbleBg = if (fromUser) TideNodeBlue else (if (isDark) Color(0xFF1E293B) else Color.White.copy(alpha = 0.55f))
    val textContentColor = if (fromUser) Color.White else (if (isDark) Color(0xFFF8FAFC) else TideNodeInk)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!fromUser) {
            Icon(
                Icons.Default.AutoAwesome,
                null,
                tint = TideNodeCyan,
                modifier = Modifier.padding(top = 8.dp).size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(if (fromUser) 0.82f else 0.90f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (fromUser) 20.dp else 5.dp,
                            bottomEnd = if (fromUser) 5.dp else 20.dp,
                        ),
                    )
                    .background(bubbleBg)
                    .padding(14.dp),
        ) {
            if (!fromUser) {
                Text(
                    "NAUTI",
                    color = TideNodeCyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                )
            }
            Text(
                message.content,
                color = textContentColor,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
            val action = NautiActionCodec.decode(message.actionType, message.actionPayloadJson)
            if (action != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onConfirmAction(message) },
                    enabled = message.actionState != "executed" && message.actionState != "rejected",
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = TideNodeCyan,
                            contentColor = Color.White,
                        ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        when (message.actionState) {
                            "executed" -> "Ausgeführt"
                            "rejected" -> "Ungültige Aktion"
                            else ->
                                if (message.requiresConfirmation) {
                                    "Prüfen und ausführen"
                                } else {
                                    "Erneut öffnen"
                                }
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun NautiComposer(
    draft: String,
    enabled: Boolean,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    val speech =
        rememberNautiSpeechInput(
            onTranscript = { transcript ->
                val separator = if (draft.isBlank()) "" else " "
                onDraft(draft.trimEnd() + separator + transcript)
            },
        )
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) speech.start()
        }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val composerBg = if (isDark) Color(0xFF0F172A) else Color.White.copy(alpha = 0.48f)
    val inputTextColor = if (isDark) Color.White else TideNodeInk
    val placeholderColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF74787E)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(composerBg)
                .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraft,
            placeholder = { Text("Nachrichten", color = placeholderColor) },
            enabled = enabled,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (draft.isNotBlank()) onSend() }),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = inputTextColor,
                    unfocusedTextColor = inputTextColor,
                ),
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                if (speech.isListening) {
                    speech.stop()
                } else if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    speech.start()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        ) {
            Icon(
                if (speech.isListening) Icons.Default.Stop else Icons.Default.Mic,
                if (speech.isListening) "Diktat stoppen" else "Diktat starten",
                tint = if (speech.isListening) Color(0xFFDC2626) else TideNodeInk,
            )
        }
        IconButton(onClick = onSend, enabled = enabled && draft.isNotBlank()) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                "Senden",
                tint = if (enabled && draft.isNotBlank()) TideNodeCyan else Color(0xFFB1B4B8),
            )
        }
    }
}

@Composable
private fun NautiHistory(
    conversations: List<NautiConversationEntity>,
    query: String,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onSelect: (NautiConversationEntity) -> Unit,
    onRename: (String, String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    val renameTarget = remember { mutableStateOf<NautiConversationEntity?>(null) }
    val deleteTarget = remember { mutableStateOf<NautiConversationEntity?>(null) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val titleColor = if (isDark) Color(0xFFF8FAFC) else TideNodeInk
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF72767C)
    val iconTint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val searchBg = if (isDark) Color(0xFF0F172A) else Color.White.copy(alpha = 0.45f)
    val searchBorder = if (isDark) Color(0xFF334155) else Color.Black.copy(alpha = 0.08f)
    val itemTitleColor = if (isDark) Color(0xFFF8FAFC) else TideNodeInk
    val itemSubtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF73777E)

    Column(Modifier.fillMaxSize().testTag("NautiInlineHistory")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück zum Chat", tint = iconTint)
            }
            Column(Modifier.weight(1f)) {
                Text("Verlauf", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = titleColor)
                Text("${conversations.size} Unterhaltungen", color = subtitleColor, fontSize = 12.sp)
            }
            IconButton(onClick = onNew) {
                Icon(Icons.Default.Add, "Neuer Chat", tint = iconTint)
            }
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ExpandMore, "Verlauf schließen", tint = iconTint)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = subtitleColor) },
            placeholder = { Text("Chats suchen", color = subtitleColor) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = searchBg,
                    unfocusedContainerColor = searchBg,
                    focusedBorderColor = searchBorder,
                    unfocusedBorderColor = searchBorder,
                    focusedTextColor = titleColor,
                    unfocusedTextColor = titleColor,
                ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            items(conversations, key = NautiConversationEntity::id) { conversation ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(conversation) }
                            .padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.History, null, tint = TideNodeCyan)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            conversation.title,
                            color = itemTitleColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (conversation.isPinned) "Angeheftet" else "Lokaler Chat",
                            color = itemSubtitleColor,
                            fontSize = 12.sp,
                        )
                    }
                    IconButton(onClick = { onPin(conversation.id, !conversation.isPinned) }) {
                        Icon(
                            Icons.Default.PushPin,
                            if (conversation.isPinned) "Lösen" else "Anheften",
                            tint = if (conversation.isPinned) TideNodeCyan else iconTint,
                        )
                    }
                    IconButton(onClick = { renameTarget.value = conversation }) {
                        Icon(Icons.Default.Edit, "Umbenennen", tint = iconTint)
                    }
                    IconButton(onClick = { deleteTarget.value = conversation }) {
                        Icon(Icons.Default.Delete, "Löschen", tint = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C))
                    }
                }
            }
        }
    }

    renameTarget.value?.let { conversation ->
        var value by remember(conversation.id) { mutableStateOf(conversation.title) }
        AlertDialog(
            onDismissRequest = { renameTarget.value = null },
            title = { Text("Chat umbenennen") },
            text = {
                OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(conversation.id, value)
                        renameTarget.value = null
                    },
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget.value = null }) {
                    Text("Abbrechen")
                }
            },
        )
    }

    deleteTarget.value?.let { conversation ->
        AlertDialog(
            onDismissRequest = { deleteTarget.value = null },
            title = { Text("Chat löschen?") },
            text = { Text("„${conversation.title}“ wird dauerhaft von diesem Gerät entfernt.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(conversation.id)
                        deleteTarget.value = null
                    },
                ) {
                    Text("Löschen", color = Color(0xFFB91C1C))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget.value = null }) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

private class NautiSpeechInput(
    context: Context,
    private val onTranscript: (String) -> Unit,
) : RecognitionListener {
    private val recognizer =
        SpeechRecognizer.createSpeechRecognizer(context.applicationContext).also {
            it.setRecognitionListener(this)
        }
    var isListening by mutableStateOf(false)
        private set

    fun start() {
        if (isListening) return
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMANY.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        runCatching {
            isListening = true
            recognizer.startListening(intent)
        }.onFailure {
            isListening = false
        }
    }

    fun stop() {
        recognizer.stopListening()
        isListening = false
    }

    fun destroy() {
        recognizer.destroy()
        isListening = false
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        isListening = false
    }

    override fun onError(error: Int) {
        isListening = false
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf(String::isNotBlank)
            ?.let(onTranscript)
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(
        eventType: Int,
        params: Bundle?,
    ) = Unit
}

@Composable
private fun rememberNautiSpeechInput(onTranscript: (String) -> Unit): NautiSpeechInput {
    val context = LocalContext.current
    val input = remember { NautiSpeechInput(context, onTranscript) }
    DisposableEffect(input) {
        onDispose(input::destroy)
    }
    return input
}
