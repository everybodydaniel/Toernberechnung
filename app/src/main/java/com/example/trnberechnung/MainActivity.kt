package com.example.trnberechnung

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.trnberechnung.database.AppDatabase
import com.example.trnberechnung.repository.TideRepository
import com.example.trnberechnung.routing.v2.SeaMask
import com.example.trnberechnung.ui.CalculatorScreen
import com.example.trnberechnung.ui.MapScreen
import com.example.trnberechnung.ui.StationDetailScreen
import com.example.trnberechnung.ui.theme.TörnberechnungTheme
import com.example.trnberechnung.viewmodel.TideViewModel
import com.example.trnberechnung.viewmodel.TideViewModelFactory
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.module.http.HttpRequestUtil
import okhttp3.OkHttpClient
import android.util.Log
import com.example.trnberechnung.ui.MainAppScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. System-Agent setzen (für HttpURLConnection)
        val userAgent = "ToernberechnungApp/1.0 (https://example.com/toernberechnung; info@example.com) Android"
        System.setProperty("http.agent", userAgent)

        // 2. MapLibre initialisieren
        // Für MapLibre 11+ ist die Angabe des TileServers oder eines Keys oft zwingend vor der ersten Nutzung des HTTP-Stacks.
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)

        // 3. OkHttp Interceptor für MapLibre (Fix für 403 Forbidden)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .header("Referer", "https://example.com/toernberechnung")
                    .build()
                Log.d("MapLibre-HTTP", "Request to: ${request.url()} with User-Agent and Referer")
                chain.proceed(request)
            }
            .build()
        HttpRequestUtil.setOkHttpClient(okHttpClient)

        lifecycleScope.launch(Dispatchers.IO) {
            SeaMask.build(applicationContext)
        }

        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            com.example.trnberechnung.database.AppDatabase::class.java,
            "tide_database"
        )
        .fallbackToDestructiveMigration()
        .build()

        val repository = TideRepository(db.tideDao(), db.logbookDao(), db.crewMemberDao(), db.checklistDao())
        val factory = TideViewModelFactory(repository)

        setContent {
            TörnberechnungTheme {
                val viewModel: TideViewModel = viewModel(factory = factory)
                LaunchedEffect(Unit) {
                    viewModel.loadData()
                }
                MainAppScreen(viewModel)
            }
        }
    }
}
