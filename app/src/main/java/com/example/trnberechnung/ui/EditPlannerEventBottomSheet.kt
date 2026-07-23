package com.example.trnberechnung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onShare: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var showShareMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Termin bearbeiten",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B3A5C)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!showShareMenu) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        focusedLabelColor = Color(0xFF2563EB),
                        unfocusedLabelColor = Color.DarkGray,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        focusedLabelColor = Color(0xFF2563EB),
                        unfocusedLabelColor = Color.DarkGray,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Löschen")
                    }
                    
                    Row {
                        OutlinedButton(
                            onClick = { showShareMenu = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Teilen")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { onSave(title, description) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            enabled = title.isNotBlank()
                        ) {
                            Text("Speichern")
                        }
                    }
                }
            } else {
                Text(
                    text = "Teilen mit:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (chatThreads.isEmpty()) {
                    Text("Keine Chats verfügbar.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(chatThreads) { thread ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onShare(thread.id) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE5E5EA)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = thread.participantName.take(1).uppercase(),
                                        color = Color(0xFF1C1C1E),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = thread.participantName,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1C1C1E)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { showShareMenu = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Zurück", color = Color.Gray)
                }
            }
        }
    }
}
