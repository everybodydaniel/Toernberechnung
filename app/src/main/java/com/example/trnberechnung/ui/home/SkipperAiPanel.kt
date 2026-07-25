package com.example.trnberechnung.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.network.GeminiHelper
import kotlinx.coroutines.launch

data class NautiChatMessage(
    val sender: String, // "Nauti" or "User"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Skipper-KI (Nauti AI) panel for the home screen (Apple Glass Design).
 *
 * Supports two states:
 * 1. Collapsed pill (Bild 2 style): Floating glass pill with AI sparkle icon.
 * 2. Expanded interactive chat (Bild 1 style): Functional Gemini AI text input,
 *    live message history, mic input, and send actions right on the Home screen.
 */
@Composable
fun SkipperAiPanel(
    onOpenChatHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val chatMessages = remember {
        mutableStateListOf(
            NautiChatMessage("Nauti", "Moin, ich bin Nauti. Wie kann ich dir helfen?")
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    fun sendMessage() {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty() || isGenerating) return

        chatMessages.add(NautiChatMessage("User", trimmed))
        inputText = ""
        focusManager.clearFocus()
        isGenerating = true

        coroutineScope.launch {
            // Scroll to bottom of chat list
            if (chatMessages.size > 1) {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
            // Ask Gemini AI
            val response = GeminiHelper.askQuestion(trimmed)
            chatMessages.add(NautiChatMessage("Nauti", response))
            isGenerating = false

            if (chatMessages.size > 1) {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = TideNodeCardShadow, spotColor = TideNodeCardShadow)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
    ) {
        if (!isExpanded) {
            // ═══════════════════════════════════════════════
            // Collapsed Pill View (Bild 2 Style)
            // ═══════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = true }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sparkle AI Icon in Cyan/Blue Glass Badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF06B6D4), Color(0xFF0284C7))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Nauti KI",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nauti KI",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Törn, Wetter oder Gezeiten",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Aufklappen",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // ═══════════════════════════════════════════════
            // Expanded Interactive View (Bild 1 Style)
            // ═══════════════════════════════════════════════
            // ── Header Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chat history icon button
                IconButton(
                    onClick = onOpenChatHistory,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "Verlauf",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Nauti Avatar
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B3A5C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sailing,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Neuer Chat",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Skipper-KI",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Reset Chat
                IconButton(
                    onClick = {
                        chatMessages.clear()
                        chatMessages.add(NautiChatMessage("Nauti", "Moin, ich bin Nauti. Wie kann ich dir helfen?"))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Neu starten",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Collapse Button
                IconButton(
                    onClick = { isExpanded = false },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Einklappen",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0x1F000000), thickness = 0.5.dp)

            // ── Chat Messages Stream ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 220.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(chatMessages) { message ->
                        val isUser = message.sender == "User"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isUser) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth(0.92f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Sailing,
                                        contentDescription = null,
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .offset(y = 3.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "NAUTI",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF007AFF),
                                            letterSpacing = 0.8.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = message.text,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A),
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                                        .background(Color(0xFF007AFF))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }

                    if (isGenerating) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 22.dp, top = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF007AFF)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nauti überlegt...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // ── Interactive Message Input Field ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Nachricht an Nauti...",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() })
                )

                // Voice Mic Button
                IconButton(
                    onClick = {
                        if (inputText.isEmpty()) {
                            inputText = "Wie ist die aktuelle Gezeitenlage?"
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Sprachnachricht",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Send Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isGenerating) Color(0xFF007AFF) else Color(0xFFCBD5E1)
                        )
                        .clickable(enabled = inputText.isNotBlank() && !isGenerating) {
                            sendMessage()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Senden",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .offset(x = 1.dp)
                    )
                }
            }
        }
    }
}
