package com.example.trnberechnung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.ui.theme.NauticalBackground
import com.example.trnberechnung.ui.theme.NauticalDivider
import com.example.trnberechnung.ui.theme.NauticalNoGo
import com.example.trnberechnung.ui.theme.NauticalPrimary
import com.example.trnberechnung.ui.theme.NauticalSurface
import com.example.trnberechnung.ui.theme.NauticalTextOnPrimary
import com.example.trnberechnung.ui.theme.NauticalTextPrimary
import com.example.trnberechnung.ui.theme.NauticalTextSecondary
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepo: AuthRepository,
    onLoginSuccess: () -> Unit,
    onSkip: () -> Unit,
) {
    var isRegistration by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf(authRepo.configurationError) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        errorMessage =
            when {
                !authRepo.isFirebaseConfigured -> authRepo.configurationError
                isRegistration && cleanName.isBlank() -> "Bitte gib deinen Namen ein."
                !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() ->
                    "Bitte gib eine gültige E-Mail-Adresse ein."
                password.length < 6 -> "Das Passwort muss mindestens 6 Zeichen lang sein."
                isRegistration && password != passwordConfirmation ->
                    "Die Passwörter stimmen nicht überein."
                else -> null
            }
        if (errorMessage != null) return

        isSubmitting = true
        scope.launch {
            val result =
                if (isRegistration) {
                    authRepo.register(cleanName, cleanEmail, password)
                } else {
                    authRepo.signIn(cleanEmail, password)
                }
            result.fold(
                onSuccess = {
                    isSubmitting = false
                    onLoginSuccess()
                },
                onFailure = {
                    isSubmitting = false
                    errorMessage = it.toLoginMessage()
                },
            )
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(NauticalBackground)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = NauticalPrimary,
            modifier = Modifier.height(72.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRegistration) "CREWSPACE-KONTO ERSTELLEN" else "WILLKOMMEN IM CREWSPACE",
            fontWeight = FontWeight.Bold,
            color = NauticalPrimary,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                if (isRegistration) {
                    "Deine Firebase-ID wird zur plattformübergreifenden Chat-ID."
                } else {
                    "Melde dich mit deinem Firebase-Konto an."
                },
            fontSize = 14.sp,
            color = NauticalTextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NauticalSurface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isRegistration) {
                    LoginTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        label = "Dein Name",
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
                LoginTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = "E-Mail-Adresse",
                    keyboardType = KeyboardType.Email,
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                LoginTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = "Passwort",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                    },
                )
                if (isRegistration) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LoginTextField(
                        value = passwordConfirmation,
                        onValueChange = {
                            passwordConfirmation = it
                            errorMessage = null
                        },
                        label = "Passwort wiederholen",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null)
                        },
                    )
                }
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = it, color = NauticalNoGo, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = ::submit,
            enabled = !isSubmitting && authRepo.isFirebaseConfigured,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NauticalPrimary),
        ) {
            Text(
                text =
                    when {
                        isSubmitting -> "BITTE WARTEN …"
                        isRegistration -> "KONTO ERSTELLEN"
                        else -> "ANMELDEN"
                    },
                fontWeight = FontWeight.Bold,
                color = NauticalTextOnPrimary,
            )
        }
        TextButton(
            onClick = {
                isRegistration = !isRegistration
                errorMessage = authRepo.configurationError
            },
            enabled = !isSubmitting,
        ) {
            Text(
                text =
                    if (isRegistration) {
                        "Bereits registriert? Anmelden"
                    } else {
                        "Noch kein Konto? Registrieren"
                    },
                color = NauticalPrimary,
            )
        }
        TextButton(
            onClick = {
                authRepo.skip()
                onSkip()
            },
            enabled = !isSubmitting,
        ) {
            Text("Ohne Account fortfahren (Chat deaktiviert)", color = NauticalTextSecondary)
        }
    }
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation =
            if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NauticalPrimary,
                unfocusedBorderColor = NauticalDivider,
                focusedLabelColor = NauticalPrimary,
                unfocusedLabelColor = NauticalTextSecondary,
                cursorColor = NauticalPrimary,
                focusedTextColor = NauticalTextPrimary,
                unfocusedTextColor = NauticalTextPrimary,
            ),
    )
}

private fun Throwable.toLoginMessage(): String =
    when (this) {
        is FirebaseAuthException ->
            when (errorCode) {
                "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
                    "E-Mail-Adresse oder Passwort ist falsch."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Für diese E-Mail existiert bereits ein Konto."
                "ERROR_WEAK_PASSWORD" -> "Bitte verwende ein stärkeres Passwort."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Firebase ist derzeit nicht erreichbar."
                else -> localizedMessage ?: "Firebase-Anmeldung fehlgeschlagen."
            }
        else -> localizedMessage ?: "Anmeldung fehlgeschlagen."
    }
