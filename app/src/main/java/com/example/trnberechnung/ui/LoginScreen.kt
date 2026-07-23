package com.example.trnberechnung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepo: AuthRepository,
    onLoginSuccess: () -> Unit,
    onSkip: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Verifizierung
    var isVerifying by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf<String?>(null) }
    var enteredCode by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NauticalBackground)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        if (isVerifying) {
            // ── VERIFIZIERUNG UI ──
            Icon(
                Icons.Outlined.Lock,
                contentDescription = "Lock",
                tint = NauticalPrimary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "VERIFIZIERUNG",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NauticalPrimary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Wir haben einen 6-stelligen Code an ${email.trim()} gesendet. Bitte gib ihn hier ein.",
                fontSize = 14.sp,
                color = NauticalTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NauticalSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = enteredCode,
                        onValueChange = { enteredCode = it; errorMessage = null },
                        label = { Text("Code eingeben", color = NauticalTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            msg,
                            color = NauticalNoGo,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val coroutineScope = rememberCoroutineScope()

            Button(
                onClick = {
                    if (enteredCode.trim() == verificationCode) {
                        coroutineScope.launch {
                            authRepo.loginWithFirebase(name.trim(), email.trim())
                            onLoginSuccess()
                        }
                    } else {
                        errorMessage = "Der eingegebene Code ist falsch."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NauticalPrimary)
            ) {
                Text(
                    "BESTÄTIGEN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NauticalTextOnPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    isVerifying = false
                    enteredCode = ""
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Abbrechen",
                    color = NauticalTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            // ── LOGIN / REGISTRIERUNG UI ──
            Icon(
                Icons.Default.LocationOn,
                contentDescription = "Logo",
                tint = NauticalPrimary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "WILLKOMMEN IM CREWSPACE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NauticalPrimary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Melde dich an, um eine Skipper-ID zu erhalten und mit deiner Crew in Kontakt zu bleiben.",
                fontSize = 14.sp,
                color = NauticalTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Eingabefelder ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NauticalSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Dein Name", color = NauticalTextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NauticalPrimary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("E-Mail-Adresse", color = NauticalTextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NauticalPrimary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            msg,
                            color = NauticalNoGo,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Buttons ──
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedEmail = email.trim()
                    
                    if (trimmedName.isBlank() || trimmedEmail.isBlank()) {
                        errorMessage = "Bitte fülle alle Felder aus."
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        errorMessage = "Bitte gib eine gültige E-Mail-Adresse ein."
                    } else if (authRepo.hasAccount(trimmedEmail)) {
                        // Bekannter Account: Direkt einloggen!
                        authRepo.login(trimmedName, trimmedEmail)
                        onLoginSuccess()
                    } else {
                        // Neuer Account: Verifizierung starten
                        val code = (100000..999999).random().toString()
                        verificationCode = code
                        isVerifying = true
                        errorMessage = null
                        // Mock-E-Mail senden (hier per Toast für Demozwecke)
                        Toast.makeText(context, "Test-Zwecke: Dein Code lautet $code", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NauticalPrimary)
            ) {
                Text(
                    "ANMELDEN / REGISTRIEREN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NauticalTextOnPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    authRepo.skip()
                    onSkip()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Ohne Account fortfahren (eingeschränkt)",
                    color = NauticalTextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
