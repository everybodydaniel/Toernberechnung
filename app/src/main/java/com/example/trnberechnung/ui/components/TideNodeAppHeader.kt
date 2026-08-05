package com.example.trnberechnung.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.R

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance

@Composable
fun TideNodeAppHeader(
    unreadCount: Int,
    onNotifications: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val headerHeight = if (isLandscape) 52.dp else 78.dp
    val logoSize = if (isLandscape) 30.dp else 42.dp
    val titleFontSize = if (isLandscape) 20.sp else 28.sp
    val buttonSize = if (isLandscape) 36.dp else 44.dp

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(headerHeight)
                .padding(horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.tidenode_mark),
                contentDescription = "TideNode Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(logoSize).testTag("app_header_logo"),
            )
            Spacer(Modifier.size(if (isLandscape) 6.dp else 9.dp))
            Text(
                text = "TideNode",
                color = if (isDark) Color.White else Color(0xFF0F172A),
                fontSize = titleFontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.8).sp,
                modifier = Modifier.testTag("app_header_wordmark"),
            )
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    icon = Icons.Default.Notifications,
                    contentDescription = "Seefahrer-Nachrichten",
                    onClick = onNotifications,
                    size = buttonSize,
                    modifier = Modifier.testTag("app_header_notifications"),
                    badge = {
                        if (unreadCount > 0) {
                            val transition = rememberInfiniteTransition(label = "notice-badge")
                            val pulse by transition.animateFloat(
                                initialValue = 0.86f,
                                targetValue = 1.14f,
                                animationSpec =
                                    infiniteRepeatable(
                                        animation = tween(900),
                                        repeatMode = RepeatMode.Reverse,
                                    ),
                                label = "notice-badge-pulse",
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 5.dp, end = 4.dp)
                                        .size(9.dp)
                                        .scale(pulse)
                                        .background(Color(0xFFFF3B30), CircleShape),
                            )
                        }
                    },
                )
                GlassIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Bildschirm drehen / Aktualisieren",
                    onClick = onRefresh,
                    size = buttonSize,
                    modifier = Modifier.testTag("app_header_refresh"),
                )
                GlassIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Einstellungen",
                    onClick = onSettings,
                    size = buttonSize,
                    modifier = Modifier.testTag("app_header_settings"),
                )
            }
        }
    }
}
