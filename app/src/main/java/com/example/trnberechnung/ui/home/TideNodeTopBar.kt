package com.example.trnberechnung.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Floating top bar for the TideNode home screen (Apple Glass Design).
 * Displays over the map with the TideNode logo/branding on the left
 * and notification, refresh, and settings icon buttons on the right in frosted glass style.
 */
@Composable
fun TideNodeTopBar(
    onNotificationsClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Left: Logo + TideNode text (Frosted Glass Badge) ──
        Row(
            modifier = Modifier
                .shadow(8.dp, CircleShape, ambientColor = TideNodeCardShadow, spotColor = TideNodeCardShadow)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.75f))
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo Icon (Blue gradient circle with sailboat)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0284C7), Color(0xFF1E3A8A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Sailing,
                    contentDescription = "TideNode Logo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TideNode",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.3).sp
            )
        }

        // ── Right: Apple Glass Action Icon Buttons ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassTopBarIconButton(
                icon = Icons.Filled.Notifications,
                contentDescription = "Benachrichtigungen",
                onClick = onNotificationsClick
            )
            GlassTopBarIconButton(
                icon = Icons.Filled.Refresh,
                contentDescription = "Aktualisieren",
                onClick = onRefreshClick
            )
            GlassTopBarIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = "Einstellungen",
                onClick = onSettingsClick
            )
        }
    }
}

/**
 * Individual frosted glass circular icon button for the top bar (Bild 2 style).
 */
@Composable
private fun GlassTopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .shadow(8.dp, CircleShape, ambientColor = TideNodeCardShadow, spotColor = TideNodeCardShadow)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.75f))
            .border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF1B3A5C),
            modifier = Modifier.size(20.dp)
        )
    }
}
