package com.example.trnberechnung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.model.BoatProfileRepository
import com.example.trnberechnung.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authRepo: AuthRepository? = null,
    onNavigateToLogin: () -> Unit = {},
    onStartNavigation: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val repo = remember { BoatProfileRepository(context) }
    val scrollState = rememberScrollState()

    var boatName by remember { mutableStateOf(repo.boatName) }
    var boatType by remember { mutableStateOf(repo.boatType) }
    var manufacturer by remember { mutableStateOf(repo.manufacturer) }
    var buildYear by remember { mutableStateOf(repo.buildYear) }
    var length by remember { mutableStateOf(if (repo.length > 0) repo.length.toString() else "") }
    var beam by remember { mutableStateOf(if (repo.beam > 0) repo.beam.toString() else "") }
    var draft by remember { mutableStateOf(repo.draft.toString()) }
    var displacement by remember { mutableStateOf(if (repo.displacement > 0) repo.displacement.toString() else "") }
    var speed by remember { mutableStateOf(repo.speed.toString()) }
    var safetyMargin by remember { mutableStateOf(repo.safetyMargin.toString()) }
    var fuelCapacity by remember { mutableStateOf(if (repo.fuelCapacity > 0) repo.fuelCapacity.toString() else "") }

    var showSavedBanner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NauticalBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {

        Text(
            "BOOTSPROFIL",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = NauticalTextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            if (boatName.isNotBlank()) boatName else "Kein Name hinterlegt",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = NauticalTextPrimary,
            modifier = Modifier.padding(bottom = 16.dp).testTag("boat_name_headline")
        )

        if (showSavedBanner) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = NauticalGoBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✅", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Bootsprofil gespeichert!",
                        fontWeight = FontWeight.Bold,
                        color = NauticalGo
                    )
                }
            }
        }

        SectionHeader(icon = Icons.Default.Info, title = "IDENTIFIKATION")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, NauticalDivider, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = NauticalSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                NauticalTextField(
                    value = boatName,
                    onValueChange = { boatName = it; repo.boatName = it },
                    label = "Bootsname",
                    placeholder = "z.B. Freya",
                    modifier = Modifier.testTag("boat_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                NauticalTextField(
                    value = boatType,
                    onValueChange = { boatType = it; repo.boatType = it },
                    label = "Typ / Modell",
                    placeholder = "z.B. Hallberg-Rassy 31"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NauticalTextField(
                        value = manufacturer,
                        onValueChange = { manufacturer = it; repo.manufacturer = it },
                        label = "Hersteller",
                        placeholder = "z.B. Bavaria",
                        modifier = Modifier.weight(1f)
                    )
                    NauticalTextField(
                        value = buildYear,
                        onValueChange = { buildYear = it; repo.buildYear = it },
                        label = "Baujahr",
                        placeholder = "z.B. 2018",
                        modifier = Modifier.weight(0.6f),
                        isNumber = true
                    )
                }
            }
        }

        SectionHeader(icon = Icons.Default.Build, title = "ABMESSUNGEN")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, NauticalDivider, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = NauticalSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileNumberField(
                        value = length,
                        onValueChange = { length = it; it.toFloatOrNull()?.let { v -> repo.length = v } },
                        label = "Länge (m)",
                        placeholder = "12.5",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileNumberField(
                        value = beam,
                        onValueChange = { beam = it; it.toFloatOrNull()?.let { v -> repo.beam = v } },
                        label = "Breite (m)",
                        placeholder = "3.8",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileNumberField(
                        value = draft,
                        onValueChange = { draft = it; it.toFloatOrNull()?.let { v -> repo.draft = v } },
                        label = "Tiefgang (m)",
                        placeholder = "1.5",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileNumberField(
                        value = displacement,
                        onValueChange = { displacement = it; it.toFloatOrNull()?.let { v -> repo.displacement = v } },
                        label = "Verdrängung (kg)",
                        placeholder = "8500",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        SectionHeader(icon = Icons.Default.Settings, title = "BETRIEB & NAVIGATION")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, NauticalDivider, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = NauticalSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileNumberField(
                        value = speed,
                        onValueChange = { speed = it; it.toFloatOrNull()?.let { v -> repo.speed = v } },
                        label = "Geschw. (kn)",
                        placeholder = "5.0",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileNumberField(
                        value = safetyMargin,
                        onValueChange = { safetyMargin = it; it.toFloatOrNull()?.let { v -> repo.safetyMargin = v } },
                        label = "UKC-Marge (m)",
                        placeholder = "0.5",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                ProfileNumberField(
                    value = fuelCapacity,
                    onValueChange = { fuelCapacity = it; it.toFloatOrNull()?.let { v -> repo.fuelCapacity = v } },
                    label = "Kraftstoff Tank (Liter)",
                    placeholder = "200",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, NauticalInfoText.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = NauticalInfoBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ️", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Alle Eingaben werden automatisch gespeichert und beim nächsten Start geladen. Die Bootsdaten erscheinen auch im Logbuch-Export (TXT).",
                    fontSize = 12.sp,
                    color = NauticalInfoText,
                    lineHeight = 16.sp
                )
            }
        }

        Button(
            onClick = onStartNavigation,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("navigation_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NauticalPrimary)
        ) {
            Text(
                "ZUR KARTE →",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = NauticalTextOnPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ══════════════════════════════════════════════════
        // Crewspace-Konto Sektion
        // ══════════════════════════════════════════════════
        CrewspaceProfileSection(
            authRepo = authRepo,
            onNavigateToLogin = onNavigateToLogin,
            onToggleDarkMode = onToggleDarkMode
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NauticalPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NauticalTextSecondary
        )
    }
}

@Composable
private fun NauticalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isNumber: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = NauticalTextSecondary) },
        placeholder = { Text(placeholder, color = NauticalTextSecondary.copy(alpha = 0.5f)) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NauticalPrimary,
            unfocusedBorderColor = NauticalDivider,
            focusedLabelColor = NauticalPrimary,
            unfocusedLabelColor = NauticalTextSecondary,
            cursorColor = NauticalPrimary,
            focusedTextColor = NauticalTextPrimary,
            unfocusedTextColor = NauticalTextPrimary
        )
    )
}

@Composable
private fun ProfileNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = NauticalTextSecondary) },
        placeholder = { Text(placeholder, color = NauticalTextSecondary.copy(alpha = 0.5f)) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NauticalPrimary,
            unfocusedBorderColor = NauticalDivider,
            focusedLabelColor = NauticalPrimary,
            unfocusedLabelColor = NauticalTextSecondary,
            cursorColor = NauticalPrimary,
            focusedTextColor = NauticalTextPrimary,
            unfocusedTextColor = NauticalTextPrimary
        )
    )
}

// ══════════════════════════════════════════════════════════════
// Crewspace-Konto Profil-Sektion
// ══════════════════════════════════════════════════════════════

@Composable
private fun CrewspaceProfileSection(
    authRepo: AuthRepository? = null,
    onNavigateToLogin: () -> Unit = {},
    onToggleDarkMode: (Boolean) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    val isLoggedIn = authRepo?.isLoggedIn ?: false
    val skipperId = authRepo?.skipperId ?: ""
    val userName = authRepo?.userName ?: ""
    val userEmail = authRepo?.userEmail ?: ""

    Column {
        // Header
        SectionHeader(icon = Icons.Default.Person, title = "CREWSPACE-KONTO")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // ── Profilbild + Name + Email ──
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar-Platzhalter
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(NauticalPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = NauticalPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        if (isLoggedIn) {
                            Text(
                                text = userName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NauticalTextPrimary
                            )
                            Text(
                                text = userEmail,
                                fontSize = 13.sp,
                                color = NauticalTextSecondary
                            )
                        } else {
                            Text(
                                text = "Gastmodus",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NauticalTextPrimary
                            )
                            Text(
                                text = "Nicht angemeldet",
                                fontSize = 13.sp,
                                color = NauticalTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = NauticalDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ── Skipper-ID Box ──
                Text(
                    text = "DEINE SKIPPER-ID",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NauticalTextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoggedIn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NauticalPrimary.copy(alpha = 0.06f))
                            .border(1.dp, NauticalPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(skipperId))
                                Toast
                                    .makeText(context, "Skipper-ID kopiert!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = skipperId,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NauticalPrimary,
                            modifier = Modifier.weight(1f),
                            letterSpacing = 0.3.sp
                        )
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Kopieren",
                            tint = NauticalPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Teile diese ID, damit andere dich im Crewspace finden können.",
                        fontSize = 11.sp,
                        color = NauticalTextSecondary.copy(alpha = 0.7f)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray.copy(alpha = 0.1f))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Melde dich an, um eine ID zu erhalten",
                            fontSize = 13.sp,
                            color = NauticalTextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = NauticalDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // ── App Appearance ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = NauticalTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Dark Mode",
                        fontSize = 15.sp,
                        color = NauticalTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    var isDark by remember { mutableStateOf(authRepo?.isDarkMode ?: false) }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { 
                            isDark = it
                            onToggleDarkMode(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NauticalPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = NauticalDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // ── Anmeldung & Sicherheit ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { /* Navigation zu Sicherheitseinstellungen */ }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = NauticalTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Anmeldung & Sicherheit",
                        fontSize = 15.sp,
                        color = NauticalTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text("›", fontSize = 18.sp, color = NauticalTextSecondary.copy(alpha = 0.4f))
                }

                if (isLoggedIn) {
                    // ── Abmelden ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                authRepo?.logout()
                                onNavigateToLogin()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Abmelden",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF3B30)
                        )
                    }
                } else {
                    // ── Anmelden ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNavigateToLogin() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = NauticalPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Anmelden / Account verbinden",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = NauticalPrimary
                        )
                    }
                }
            }
        }
    }
}
