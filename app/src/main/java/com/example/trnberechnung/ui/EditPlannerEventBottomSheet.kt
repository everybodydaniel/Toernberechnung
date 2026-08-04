package com.example.trnberechnung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.model.ChatThread
import com.example.trnberechnung.model.PlannerEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlannerEventBottomSheet(
    event: PlannerEvent,
    chatThreads: List<ChatThread>,
    onDismiss: () -> Unit,
    onSave: (PlannerEvent) -> Unit,
    onDelete: () -> Unit,
    onShare: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Lokale States für alle Felder
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var startTime by remember { mutableStateOf(event.startTime ?: "") }
    var endTime by remember { mutableStateOf(event.endTime ?: "") }
    var location by remember { mutableStateOf(event.location ?: "") }
    var category by remember { mutableStateOf(event.category) }

    var showShareMenu by remember { mutableStateOf(false) }

    // Farben passend zum Crewspace (vereinfacht für die Komponente)
    val primaryBlue = Color(0xFF2563EB)
    val surfaceColor = Color(0xFFF8F8FA)
    val textColor = Color(0xFF1B3A5C)
    val secondaryText = Color(0xFF8E8E93)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (event.title.isEmpty()) "Neuer Termin" else "Termin bearbeiten",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen", tint = secondaryText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!showShareMenu) {
                // Titel Input
                EditField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Titel des Termins",
                    icon = Icons.Default.Title,
                    placeholder = "z.B. Ablegen Richtung Norderney"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Zeit Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = "Beginn",
                            icon = Icons.Default.AccessTime,
                            placeholder = "09:00"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = "Ende",
                            icon = Icons.Default.Timer,
                            placeholder = "14:30"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ort Input
                EditField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Ort / Hafen",
                    icon = Icons.Default.LocationOn,
                    placeholder = "z.B. Greetsiel"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Kategorie Auswahl
                Text(
                    text = "Kategorie",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Allgemein", "Navigation", "Verpflegung", "Landgang").forEach { cat ->
                        val isSelected = category == cat
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { category = cat },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) primaryBlue else surfaceColor,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)) else null
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Beschreibung
                EditField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Beschreibung (optional)",
                    icon = Icons.AutoMirrored.Outlined.Notes,
                    placeholder = "Details zum Termin...",
                    singleLine = false,
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (event.title.isNotEmpty()) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (event.title.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showShareMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, primaryBlue),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Teilen", color = primaryBlue)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Button(
                            onClick = {
                                onSave(event.copy(
                                    title = title,
                                    description = description,
                                    startTime = startTime.ifBlank { null },
                                    endTime = endTime.ifBlank { null },
                                    location = location.ifBlank { null },
                                    category = category
                                ))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            enabled = title.isNotBlank(),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Speichern", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Share Menu
                Text(
                    text = "In Chat teilen",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (chatThreads.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Keine aktiven Chats gefunden.", color = secondaryText)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(chatThreads) { thread ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onShare(thread.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(primaryBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = thread.participantName,
                                    fontSize = 16.sp,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = secondaryText.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { showShareMenu = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Zurück zur Bearbeitung", color = primaryBlue)
                }
            }
        }
    }
}

@Composable
private fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.Gray.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF2563EB)) },
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            minLines = minLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f),
                focusedContainerColor = Color(0xFFF8F8FA),
                unfocusedContainerColor = Color(0xFFF8F8FA),
                cursorColor = Color(0xFF2563EB)
            )
        )
    }
}
