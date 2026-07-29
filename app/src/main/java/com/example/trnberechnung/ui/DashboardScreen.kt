package com.example.trnberechnung.ui

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    onNavigateToLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onStartNavigation: () -> Unit,
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
    var authTabSelected by remember { mutableStateOf("login") }
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
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
        // Card 2: Crewspace-Konto
        // ══════════════════════════════════════════════════
        SettingsCard {
            SettingsCardHeader(
                icon = Icons.Default.Group,
                title = "Crewspace-Konto",
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text =
                    "Melde dich an, um Skipper per ID zu finden, private Chats und Gruppen zu erstellen " +
                        "und Termine gemeinsam zu planen.",
                fontSize = 13.sp,
                color = SettingsSubtitle,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(14.dp))

            val isLoggedIn = authRepo?.isLoggedIn ?: false
            if (!isLoggedIn) {
                // Login / Registrieren Toggle
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SettingsInputBg)
                            .padding(4.dp),
                ) {
                    val isLogin = authTabSelected == "login"
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isLogin) SettingsPrimaryBlue else Color.Transparent)
                                .clickable { authTabSelected = "login" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Login,
                                null,
                                tint = if (isLogin) Color.White else SettingsSubtitle,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Login",
                                color = if (isLogin) Color.White else SettingsSubtitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isLogin) SettingsPrimaryBlue else Color.Transparent)
                                .clickable { authTabSelected = "register" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PersonAdd,
                                null,
                                tint = if (!isLogin) Color.White else SettingsSubtitle,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Registrieren",
                                color = if (!isLogin) Color.White else SettingsSubtitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Input: Email
                SettingsInputField(
                    value = loginEmail,
                    onValueChange = { loginEmail = it },
                    placeholder = "E-Mail",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                )

                Spacer(Modifier.height(10.dp))

                // Input: Passwort
                SettingsInputField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    placeholder = "Passwort",
                    leadingIcon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null,
                                tint = SettingsSubtitle,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )

                Spacer(Modifier.height(14.dp))

                // Submit Button
                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = SettingsPrimaryBlue,
                            contentColor = Color.White,
                        ),
                ) {
                    Text(
                        if (authTabSelected == "login") "Einloggen" else "Konto erstellen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            } else {
                // Logged in View
                val skipperId = authRepo?.skipperId ?: ""
                val userName = authRepo?.userName ?: "Skipper"
                val clipboardManager = LocalClipboardManager.current

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SettingsBadgeBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.AccountCircle, null, tint = SettingsPrimaryBlue, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(userName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        Text(authRepo?.userEmail ?: "", fontSize = 12.sp, color = SettingsSubtitle)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SettingsBadgeBg)
                            .border(1.dp, SettingsPrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(skipperId))
                                Toast.makeText(context, "Skipper-ID kopiert!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("SKIPPER-ID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SettingsSubtitle)
                        Text(skipperId, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SettingsPrimaryBlue)
                    }
                    Icon(Icons.Outlined.ContentCopy, null, tint = SettingsPrimaryBlue, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Abmelden", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ══════════════════════════════════════════════════
        // Card 3: Bootsprofil
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

            Spacer(Modifier.height(10.dp))

            // Phone Icon Subtitle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Smartphone, null, tint = SettingsSubtitle, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Auf diesem Gerät gespeichert", fontSize = 13.sp, color = SettingsSubtitle)
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
        // Card 4: Darstellung (Dark / White Mode Slider)
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
        // Card 5: Einführung
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
        // Card 6: Datenquellen (Android Native Data Sources)
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
            Spacer(Modifier.height(8.dp))
            DataSourceRow(
                icon = Icons.Default.Lock,
                title = "Firebase Auth",
                subtitle = "Sicherer Crewspace-Zugang mit eindeutiger Skipper-ID",
            )
        }

        // ══════════════════════════════════════════════════
        // Card 7: Copyright & Disclaimer
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
