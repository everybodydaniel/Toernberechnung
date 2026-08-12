package com.example.trnberechnung.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.R

@Composable
fun TideNodeAppHeader(
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val headerHeight = if (isLandscape) 52.dp else 78.dp
    val logoSize = if (isLandscape) 30.dp else 42.dp
    val titleFontSize = if (isLandscape) 20.sp else 28.sp
    val buttonSize = if (isLandscape) 44.dp else 48.dp

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
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(logoSize)
                        .clip(RoundedCornerShape(logoSize * 0.27f))
                        .testTag("app_header_logo"),
            )
            Spacer(Modifier.size(if (isLandscape) 6.dp else 9.dp))
            Text(
                text = "TideNode",
                color = if (isDark) Color.White else Color(0xFF0F172A),
                fontSize = titleFontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.8).sp,
                modifier = Modifier.testTag("app_header_wordmark").semantics { heading() },
            )
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
