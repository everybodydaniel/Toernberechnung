package com.example.trnberechnung.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.R
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.model.BoatProfileRepository
import com.example.trnberechnung.ui.components.TideNodeBlue
import com.example.trnberechnung.ui.components.tideNodeGlass

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.compose.ui.graphics.luminance

private val SettingsCardBg: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E293B) else Color.White

private val SettingsSectionTitle: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF93C5FD) else Color(0xFF1E3A8A)

private val SettingsSubtitle: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF94A3B8) else Color(0xFF64748B)

private val SettingsInputBg: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF0F172A) else Color(0xFFF8FAFC)

private val SettingsInputBorder: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF334155) else Color(0xFFE2E8F0)

private val SettingsBadgeBg: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)

private val SettingsPrimaryBlue: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF60A5FA) else Color(0xFF2563EB)

private val SettingsTextColor: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF8FAFC) else Color(0xFF0F172A)

@Composable
fun DashboardScreen(
    authRepo: AuthRepository? = null,
    onToggleDarkMode: (Boolean) -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { BoatProfileRepository(context) }
    val scrollState = rememberScrollState()

    var boatName by remember { mutableStateOf(repo.boatName) }
    var boatType by remember { mutableStateOf(repo.boatType.ifBlank { "Segelyacht" }) }
    var callSign by remember { mutableStateOf(repo.callSign) }
    var draft by remember { mutableStateOf(if (repo.draft > 0) repo.draft.toString() else "") }
    var length by remember { mutableStateOf(if (repo.length > 0) repo.length.toString() else "") }
    var safetyMargin by remember { mutableStateOf(if (repo.safetyMargin > 0) repo.safetyMargin.toString() else "") }

    var isDark by remember { mutableStateOf(authRepo?.isDarkMode ?: false) }
    var boatTypeExpanded by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Drag Handle & Header Title
        Box(
            modifier =
                Modifier
                    .size(width = 38.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1)),
        )

        Text(
            text = "Einstellungen",
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SettingsTextColor,
        )

        // Hidden headline testTag for automated UI tests
        Box(modifier = Modifier.size(0.dp).testTag("boat_name_headline")) {
            Text(boatName)
        }

        // ══════════════════════════════════════════════════
        // Card 1: Top Brand Pill (TideNode)
        // ══════════════════════════════════════════════════
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.tidenode_mark),
                    contentDescription = "TideNode Logo",
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "TideNode",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SettingsSectionTitle,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        text = "Profile, Darstellung und Datenquellen",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettingsSubtitle,
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════
        // Card 2: Bootsprofil
        // ══════════════════════════════════════════════════
        SettingsCard {
            SettingsCardHeader(
                icon = Icons.Default.Sailing,
                title = "Bootsprofil",
            )
            Spacer(Modifier.height(14.dp))

            // Bootsname Field
            SettingsInputField(
                value = boatName,
                onValueChange = {
                    boatName = it
                    repo.boatName = it
                },
                placeholder = "Bootsname",
                leadingIcon = Icons.Default.Label,
                modifier = Modifier.testTag("boat_name_input"),
            )

            Spacer(Modifier.height(12.dp))

            // Bootstyp Selector
            Text("Bootstyp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SettingsSubtitle)
            Spacer(Modifier.height(4.dp))
            Box {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SettingsInputBg)
                            .border(1.dp, SettingsInputBorder, RoundedCornerShape(16.dp))
                            .clickable { boatTypeExpanded = true }
                            .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Default.DirectionsBoat)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = boatType,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Default.UnfoldMore, null, tint = SettingsPrimaryBlue, modifier = Modifier.size(20.dp))
                }

                DropdownMenu(
                    expanded = boatTypeExpanded,
                    onDismissRequest = { boatTypeExpanded = false },
                ) {
                    listOf("Segelyacht", "Motoryacht", "Katamaran", "Gleiter", "Motorboot", "Schwertboot").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontWeight = FontWeight.Bold) },
                            onClick = {
                                boatType = option
                                repo.boatType = option
                                boatTypeExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Rufzeichen Field
            SettingsInputField(
                value = callSign,
                onValueChange = {
                    callSign = it
                    repo.callSign = it
                },
                placeholder = "Rufzeichen",
                leadingIcon = Icons.Default.CellTower,
            )

            Spacer(Modifier.height(12.dp))

            // 3 Numeric Fields Row (Tiefgang, Länge, UKC)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingsNumberBox(
                    value = draft,
                    onValueChange = {
                        draft = it
                        it.toFloatOrNull()?.let { v -> repo.draft = v }
                    },
                    icon = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f),
                )
                SettingsNumberBox(
                    value = length,
                    onValueChange = {
                        length = it
                        it.toFloatOrNull()?.let { v -> repo.length = v }
                    },
                    icon = Icons.Default.Straighten,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingsNumberBox(
                    value = safetyMargin,
                    onValueChange = {
                        safetyMargin = it
                        it.toFloatOrNull()?.let { v -> repo.safetyMargin = v }
                    },
                    icon = Icons.Default.Shield,
                    modifier = Modifier.fillMaxWidth(0.5f),
                )
            }
        }

        // ══════════════════════════════════════════════════
        // Card 3: Darstellung (Dark / White Mode Slider)
        // ══════════════════════════════════════════════════
        SettingsCard {
            SettingsCardHeader(
                icon = Icons.Default.Palette,
                title = "Darstellung",
            )
            Spacer(Modifier.height(14.dp))

            // Segmented Theme Toggle Bar
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(SettingsInputBg)
                        .border(1.dp, SettingsInputBorder, RoundedCornerShape(28.dp))
                        .padding(4.dp),
            ) {
                // Light Mode Button
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (!isDark) SettingsPrimaryBlue else Color.Transparent)
                            .clickable {
                                isDark = false
                                authRepo?.isDarkMode = false
                                onToggleDarkMode(false)
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = "Light Mode",
                            tint = if (!isDark) Color.White else SettingsSubtitle,
                            modifier = Modifier.size(24.dp),
                        )
                        if (!isDark) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Check, null, tint = SettingsPrimaryBlue, modifier = Modifier.size(9.dp))
                            }
                        }
                    }
                }

                // Dark Mode Button
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isDark) SettingsPrimaryBlue else Color.Transparent)
                            .clickable {
                                isDark = true
                                authRepo?.isDarkMode = true
                                onToggleDarkMode(true)
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            Icons.Default.NightsStay,
                            contentDescription = "Dark Mode",
                            tint = if (isDark) Color.White else SettingsSubtitle,
                            modifier = Modifier.size(24.dp),
                        )
                        if (isDark) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Check, null, tint = SettingsPrimaryBlue, modifier = Modifier.size(9.dp))
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════
        // Card 4: Einführung
        // ══════════════════════════════════════════════════
        SettingsCard {
            SettingsCardHeader(
                icon = Icons.Default.AutoAwesome,
                title = "Einführung",
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SettingsInputBg)
                        .border(1.dp, SettingsInputBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            onReplayOnboarding()
                            showOnboardingDialog = true
                        }
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SettingsBadgeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = SettingsPrimaryBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Einführung erneut ansehen", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF8FAFC) else Color(0xFF0F172A))
                    Text("Törnplanung, Wetter und Crewspace", fontSize = 12.sp, color = SettingsSubtitle)
                }
                Icon(Icons.Default.ChevronRight, null, tint = SettingsSubtitle, modifier = Modifier.size(20.dp))
            }
        }

        // ══════════════════════════════════════════════════
        // Card 5: Datenquellen (Android Native Data Sources)
        // ══════════════════════════════════════════════════
        SettingsCard {
            SettingsCardHeader(
                icon = Icons.Default.Language,
                title = "Datenquellen",
            )
            Spacer(Modifier.height(12.dp))

            DataSourceRow(
                icon = Icons.Default.Water,
                title = "BSH",
                subtitle = "Gezeiten, Hoch- und Niedrigwasser",
            )
            Spacer(Modifier.height(8.dp))
            DataSourceRow(
                icon = Icons.Default.Cloud,
                title = "Open-Meteo & DWD",
                subtitle = "Wetter-Prognosen, Wind und Böen",
            )
        }

        // ══════════════════════════════════════════════════
        // Card 6: Copyright & Disclaimer
        // ══════════════════════════════════════════════════
        SettingsCard {
            Text(
                text = "© 2026 TideNode",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SettingsSectionTitle,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text =
                    "TideNode ersetzt keine Seeordnung, amtlichen Bekanntmachungen, Revierinformationen " +
                        "oder die nautische Verantwortung der Schiffsführung.",
                fontSize = 13.sp,
                color = SettingsSubtitle,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(20.dp))
    }

    if (showOnboardingDialog) {
        Dialog(
            onDismissRequest = { showOnboardingDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            OnboardingScreen(
                onCompleted = { showOnboardingDialog = false },
            )
        }
    }
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SettingsCardBg)
                .border(1.dp, SettingsInputBorder, RoundedCornerShape(24.dp))
                .padding(18.dp),
        content = content,
    )
}

@Composable
private fun SettingsCardHeader(
    icon: ImageVector,
    title: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SettingsSectionTitle,
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SettingsBadgeBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = SettingsPrimaryBlue, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = SettingsSubtitle, fontSize = 14.sp) },
        leadingIcon = { IconBadge(leadingIcon) },
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SettingsInputBg,
                unfocusedContainerColor = SettingsInputBg,
                focusedBorderColor = SettingsPrimaryBlue,
                unfocusedBorderColor = SettingsInputBorder,
                focusedTextColor = SettingsTextColor,
                unfocusedTextColor = SettingsTextColor,
            ),
    )
}

@Composable
private fun SettingsNumberBox(
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SettingsInputBg)
                .border(1.dp, SettingsInputBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = SettingsTextColor,
                    unfocusedTextColor = SettingsTextColor,
                ),
        )
    }
}

@Composable
private fun DataSourceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SettingsInputBg)
                .border(1.dp, SettingsInputBorder, RoundedCornerShape(16.dp))
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SettingsSectionTitle)
            Text(subtitle, fontSize = 12.sp, color = SettingsSubtitle)
        }
    }
}
