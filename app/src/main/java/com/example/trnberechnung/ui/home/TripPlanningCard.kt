package com.example.trnberechnung.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Trip-planning glass card as shown on the TideNode home screen (Apple Glass Design).
 * Matches Bild 2: Floating pill-shaped frosted glass card with sailboat icon,
 * title, subtitle, chevron, and safe departure window info.
 */
@Composable
fun TripPlanningCard(
    onCardClick: () -> Unit,
    onRefreshDepartureWindow: () -> Unit = {},
    departureWindowText: String = "Für diese Route liegt noch kein Passagefenster vor.",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = TideNodeCardShadow, spotColor = TideNodeCardShadow)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.80f))
            .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
    ) {
        // ── Main clickable row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCardClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sailboat icon in circular blue glass badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Navigation,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Törn planen",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Start, Ziel und Abfahrt auswählen",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 17.sp
                )
            }

            // Chevron arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Öffnen",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(24.dp)
            )
        }

        // ── Sub-section for Sicheres Abfahrtsfenster ──
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 18.dp),
            thickness = 0.5.dp,
            color = Color(0x22000000)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Sailing,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sicheres Abfahrtsfenster",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF007AFF)
                )
                Text(
                    text = departureWindowText,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
            }
            IconButton(
                onClick = onRefreshDepartureWindow,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Aktualisieren",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
