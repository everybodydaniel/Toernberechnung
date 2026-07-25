package com.example.trnberechnung.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ══════════════════════════════════════════════════════════════
// TideNode Home – Apple Glassmorphism Colors & Utilities
// ══════════════════════════════════════════════════════════════

import androidx.compose.foundation.border

// ── Apple Glass overlay palette (iOS-inspired frosted glass) ──
val TideNodeBlue          = Color(0xFF007AFF)
val TideNodeBlueDark      = Color(0xFF0056B3)
val TideNodeNavy          = Color(0xFF1B3A5C)
val TideNodeSurface       = Color(0xFFFFFFFE)
val TideNodeSurfaceAlpha  = Color(0xC8FFFFFF)  // ~78% white transparency for glass
val TideNodeSheetBg       = Color(0xD8FFFFFF)  // ~85% white for glass sheet
val TideNodeGlassBorder   = Color(0x99FFFFFF)  // ~60% white border for glass edge
val TideNodeTextPrimary   = Color(0xFF1C1C1E)
val TideNodeTextSecondary = Color(0xFF6E6E73)
val TideNodeTextTertiary  = Color(0xFF8E8E93)
val TideNodeTabBg         = Color(0xF2F2F7FF)
val TideNodeTabActiveBg   = Color.White
val TideNodeDivider       = Color(0x33000000)
val TideNodeSearchBg      = Color(0xE6F2F2F7)
val TideNodeIconCircleBg  = Color(0xB3FFFFFF)  // Translucent white for top bar icons
val TideNodeCheckGreen    = Color(0xFF34C759)
val TideNodeCardShadow    = Color(0x1F000000)   // 12% shadow

/**
 * Modifier extension for a frosted-glass card panel (Apple Glass style).
 */
fun Modifier.glassmorphismCard(
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 10.dp
): Modifier = this
    .shadow(elevation, RoundedCornerShape(cornerRadius), ambientColor = TideNodeCardShadow, spotColor = TideNodeCardShadow)
    .clip(RoundedCornerShape(cornerRadius))
    .background(TideNodeSurfaceAlpha)
    .border(1.dp, TideNodeGlassBorder, RoundedCornerShape(cornerRadius))

/**
 * Modifier extension for floating pill elements (Apple Glass style).
 */
fun Modifier.appleGlassPill(
    cornerRadius: Dp = 32.dp,
    elevation: Dp = 8.dp
): Modifier = this
    .shadow(elevation, RoundedCornerShape(cornerRadius), ambientColor = TideNodeCardShadow, spotColor = TideNodeCardShadow)
    .clip(RoundedCornerShape(cornerRadius))
    .background(TideNodeSurfaceAlpha)
    .border(1.dp, TideNodeGlassBorder, RoundedCornerShape(cornerRadius))

/**
 * Modifier extension for the bottom sheet container.
 */
fun Modifier.glassmorphismSheet(): Modifier = this
    .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
    .background(TideNodeSheetBg)
    .border(1.dp, TideNodeGlassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))

/**
 * Modifier extension for small icon circle buttons (notification, refresh, settings).
 */
fun Modifier.iconCircle(
    backgroundColor: Color = TideNodeIconCircleBg,
    size: Dp = 36.dp
): Modifier = this
    .clip(RoundedCornerShape(50))
    .background(backgroundColor)
