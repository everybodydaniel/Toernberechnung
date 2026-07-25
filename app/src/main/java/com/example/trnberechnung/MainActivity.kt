package com.example.trnberechnung

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.trnberechnung.model.OnboardingPreferences
import com.example.trnberechnung.messaging.ChatNavigationState
import com.example.trnberechnung.messaging.CrewspaceMessagingService
import com.example.trnberechnung.repository.TideRepository
import com.example.trnberechnung.routing.v2.SeaMask
import com.example.trnberechnung.ui.LoginScreen
import com.example.trnberechnung.ui.MainAppScreen
import com.example.trnberechnung.ui.OnboardingScreen
import com.example.trnberechnung.ui.theme.TörnberechnungTheme
import com.example.trnberechnung.viewmodel.CrewspaceViewModelFactory
import com.example.trnberechnung.viewmodel.TideViewModel
import com.example.trnberechnung.viewmodel.TideViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.module.http.HttpRequestUtil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleChatIntent(intent)

        // 1. System-Agent setzen (für HttpURLConnection)
        val userAgent = "ToernberechnungApp/1.0 (https://example.com/toernberechnung; info@example.com) Android"
        System.setProperty("http.agent", userAgent)

        // 2. MapLibre initialisieren
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)

        // 3. OkHttp Interceptor für MapLibre (Fix für 403 Forbidden)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .header("Referer", "https://example.com/toernberechnung")
                    .build()
                Log.d("MapLibre-HTTP", "Request to: ${request.url} with User-Agent and Referer")
                chain.proceed(request)
            }
            .build()
        HttpRequestUtil.setOkHttpClient(okHttpClient)

        lifecycleScope.launch(Dispatchers.IO) {
            SeaMask.build(applicationContext)
        }

        enableEdgeToEdge()

        val tideNodeApplication = application as TideNodeApplication
        val db = tideNodeApplication.database
        val authRepo = tideNodeApplication.authRepository
        val chatRepository = tideNodeApplication.chatRepository

        val repository =
            TideRepository(
                db.tideDao(),
                db.logbookDao(),
                db.crewMemberDao(),
                db.checklistDao(),
                db.plannerEventDao(),
                db.seafarerMessageDao(),
            )
        val factory = TideViewModelFactory(repository)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST,
            )
        }

        setContent {
            var isDarkMode by remember { mutableStateOf(authRepo.isDarkMode) }

            TörnberechnungTheme(darkTheme = isDarkMode) {
                val onboardingPreferences = remember { OnboardingPreferences(applicationContext) }
                var onboardingCompleted by remember {
                    mutableStateOf(onboardingPreferences.isCompleted)
                }

                if (!onboardingCompleted) {
                    OnboardingScreen(
                        onCompleted = {
                            onboardingPreferences.markCompleted()
                            onboardingCompleted = true
                        }
                    )
                } else {
                    val rootNavController = rememberNavController()
                    val viewModel: TideViewModel = viewModel(factory = factory)
                    val crewspaceFactory =
                        CrewspaceViewModelFactory(repository, chatRepository, authRepo)

                    LaunchedEffect(Unit) {
                        viewModel.loadData()
                    }

                    val startDestination = if (!authRepo.isLoggedIn && !authRepo.isSkipped) {
                        "login"
                    } else {
                        "main"
                    }

                    NavHost(navController = rootNavController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(
                                authRepo = authRepo,
                                onLoginSuccess = {
                                    lifecycleScope.launch {
                                        runCatching { chatRepository.activate() }
                                    }
                                    rootNavController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onSkip = {
                                    rootNavController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("main") {
                            MainAppScreen(
                                viewModel = viewModel,
                                crewspaceViewModelFactory = crewspaceFactory,
                                authRepo = authRepo,
                                onNavigateToLogin = {
                                    chatRepository.deactivate()
                                    rootNavController.navigate("login") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                },
                                onLogout = {
                                    lifecycleScope.launch {
                                        chatRepository.logout()
                                        rootNavController.navigate("login") {
                                            popUpTo("main") { inclusive = true }
                                        }
                                    }
                                },
                                onToggleDarkMode = { mode ->
                                    isDarkMode = mode
                                    authRepo.isDarkMode = mode
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleChatIntent(intent)
    }

    private fun handleChatIntent(intent: Intent?) {
        ChatNavigationState.requestConversation(
            intent?.getStringExtra(CrewspaceMessagingService.EXTRA_CONVERSATION_ID)
                ?: intent?.getStringExtra("conversation_id"),
        )
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 4102
    }
}
