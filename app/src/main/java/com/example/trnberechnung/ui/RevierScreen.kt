package com.example.trnberechnung.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trnberechnung.dto.WeatherDto
import com.example.trnberechnung.model.TideEvent
import com.example.trnberechnung.model.TideStationData
import com.example.trnberechnung.ui.theme.*
import com.example.trnberechnung.viewmodel.TideViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun RevierScreen(viewModel: TideViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Wetter, 1 = Gezeiten
    val weather by viewModel.currentWeather.collectAsState()
    val forecast by viewModel.forecastData.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val allStations by viewModel.allStations.collectAsState()
    val tideEvents by viewModel.currentTideEvents.collectAsState()
    var showStationDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val dailyForecast = remember(forecast) { aggregateToDays(forecast) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3A8DBC), Color(0xFF1B4E7A))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom Header
            RevierHeader(onRefresh = { viewModel.loadData() })

            // Tab Switcher
            RevierTabSwitcher(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    WeatherContent(
                        stationName = selectedStation?.gaugeLabel ?: "Standort wählen",
                        weather = weather,
                        forecast = forecast,
                        dailyForecast = dailyForecast,
                        onStationClick = { showStationDialog = true },
                        viewModel = viewModel
                    )
                } else {
                    val tideLoading by viewModel.tideLoading.collectAsState()
                    TideContent(
                        station = selectedStation,
                        events = tideEvents,
                        loading = tideLoading
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showStationDialog) {
        StationSelectionDialog(
            stations = allStations,
            selectedStation = selectedStation,
            onDismiss = { showStationDialog = false },
            onStationSelected = {
                viewModel.selectStation(it)
                showStationDialog = false
            }
        )
    }
}

@Composable
fun RevierHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "TideNode",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            HeaderIconButton(Icons.Default.Notifications, {})
            Spacer(modifier = Modifier.width(8.dp))
            HeaderIconButton(Icons.Default.Refresh, onRefresh)
            Spacer(modifier = Modifier.width(8.dp))
            HeaderIconButton(Icons.Default.Settings, {})
        }
    }
}

@Composable
fun HeaderIconButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun RevierTabSwitcher(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(4.dp)
    ) {
        TabItem(
            text = "Wetter",
            icon = "⛅",
            isSelected = selectedTab == 0,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(0) }
        )
        TabItem(
            text = "Gezeiten",
            icon = "🌊",
            isSelected = selectedTab == 1,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(1) }
        )
    }
}

@Composable
fun TabItem(text: String, icon: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun WeatherContent(
    stationName: String,
    weather: WeatherDto?,
    forecast: List<WeatherDto>,
    dailyForecast: List<DailyForecast>,
    onStationClick: () -> Unit,
    viewModel: TideViewModel
) {
    // Hero Weather
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onStationClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stationName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
            Text(
                "${weather?.temperature?.toInt() ?: "--"}°",
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
            Text(
                translateCondition(weather?.condition),
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            val today = dailyForecast.firstOrNull()
            if (today != null) {
                Text("H: ${today.highTemp}°  T: ${today.lowTemp}°", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            Text(
                "Gefühlt ${weather?.dewPoint?.toInt() ?: "--"}° · Aktualisiert vor 5 Min.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    // Hourly Forecast
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("48-STUNDEN-VORHERSAGE", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                items(forecast.take(24)) { hour ->
                    val time = try {
                        OffsetDateTime.parse(hour.timestamp).atZoneSameInstant(ZoneId.of("Europe/Berlin")).format(DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) { "--:--" }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(time, color = Color.White, fontSize = 12.sp)
                        Text(iconToEmoji(hour.icon ?: hour.condition), fontSize = 24.sp, modifier = Modifier.padding(vertical = 4.dp))
                        Text("${hour.temperature?.toInt() ?: "--"}°", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${(hour.windSpeed?.div(1.852))?.toInt() ?: "--"} kn", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        Text("B ${(hour.windGustSpeed?.div(1.852))?.toInt() ?: "--"}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }
        }
    }

    // Wind Compass & Details
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Air, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("WIND IM REVIER", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                    WindCompass(degrees = weather?.windDirection ?: 0)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WindDetailRow("Grundwind", "${(weather?.windSpeed?.div(1.852))?.toInt() ?: "--"} kn")
                    WindDetailRow("Böen", "${(weather?.windGustSpeed?.div(1.852))?.toInt() ?: "--"} kn")
                    WindDetailRow("Richtung", "${windDirectionToText(weather?.windDirection ?: 0)} · ${weather?.windDirection ?: 0}°")
                }
            }
        }
    }

    // Wind Karte
    val allStations by viewModel.allStations.collectAsState()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Map, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("WINDKARTE OSTFRIESLAND", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            }
            Text(
                "Windkarte",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                WindMapComponent(
                    modifier = Modifier.fillMaxSize(),
                    stations = allStations,
                    currentWeather = weather
                )

                // Overlay information
                Column(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "LIVE",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Wind & Böen Graph
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("WIND UND BÖEN", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("● GRUNDWIND", color = Color(0xFF4FC3F7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("● BÖEN", color = Color(0xFFFFB74D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            WindGustGraph(forecast.take(12))
        }
    }

    // 7-Day Forecast
    if (dailyForecast.isNotEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("7-TAGE-VORHERSAGE", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                val weekMin = dailyForecast.minOf { it.lowTemp }
                val weekMax = dailyForecast.maxOf { it.highTemp }
                dailyForecast.forEachIndexed { index, day ->
                    if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                    RevierWeekForecastRow(day = day, weekMin = weekMin, weekMax = weekMax)
                }
            }
        }
    }
}

@Composable
fun WindCompass(degrees: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        drawCircle(
            color = Color.White.copy(alpha = 0.1f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Cardinal directions
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            alpha = 150
        }

        drawContext.canvas.nativeCanvas.drawText("N", center.x, center.y - radius + 15.dp.toPx(), paint)
        drawContext.canvas.nativeCanvas.drawText("S", center.x, center.y + radius - 5.dp.toPx(), paint)
        drawContext.canvas.nativeCanvas.drawText("W", center.x - radius + 10.dp.toPx(), center.y + 5.dp.toPx(), paint)
        drawContext.canvas.nativeCanvas.drawText("O", center.x + radius - 10.dp.toPx(), center.y + 5.dp.toPx(), paint)

        // Arrow
        val angleRad = (degrees - 90) * PI / 180.0
        val arrowLen = radius - 20.dp.toPx()
        val endX = center.x + (arrowLen * cos(angleRad)).toFloat()
        val endY = center.y + (arrowLen * sin(angleRad)).toFloat()

        drawLine(
            color = Color(0xFF4FC3F7),
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = center
        )

        // Direction label in center
        val centerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 14.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        drawContext.canvas.nativeCanvas.drawText(windDirectionToText(degrees), center.x, center.y + 30.dp.toPx(), centerPaint)
    }
}

@Composable
fun WindGustGraph(forecast: List<WeatherDto>) {
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        if (forecast.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 20.dp.toPx()
        val graphHeight = height - padding * 2

        val windSpeeds = forecast.map { (it.windSpeed ?: 0.0) / 1.852 }
        val gustSpeeds = forecast.map { (it.windGustSpeed ?: 0.0) / 1.852 }

        val maxVal = (gustSpeeds.maxOrNull() ?: 10.0).coerceAtLeast(30.0).toFloat()
        val stepX = width / (forecast.size - 1)

        fun y(v: Double) = height - padding - (v.toFloat() / maxVal * graphHeight)

        // Fill area between wind and gust
        val fillPath = Path()
        fillPath.moveTo(0f, y(windSpeeds[0]))
        for (i in 1 until forecast.size) {
            fillPath.lineTo(i * stepX, y(windSpeeds[i]))
        }
        for (i in forecast.size - 1 downTo 0) {
            fillPath.lineTo(i * stepX, y(gustSpeeds[i]))
        }
        fillPath.close()
        drawPath(fillPath, color = Color.White.copy(alpha = 0.1f))

        // Wind line
        val windPath = Path()
        windPath.moveTo(0f, y(windSpeeds[0]))
        for (i in 1 until forecast.size) {
            windPath.lineTo(i * stepX, y(windSpeeds[i]))
        }
        drawPath(windPath, color = Color(0xFF4FC3F7), style = Stroke(width = 2.dp.toPx()))

        // Gust line
        val gustPath = Path()
        gustPath.moveTo(0f, y(gustSpeeds[0]))
        for (i in 1 until forecast.size) {
            gustPath.lineTo(i * stepX, y(gustSpeeds[i]))
        }
        drawPath(gustPath, color = Color(0xFFFFB74D), style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
    }
}

@Composable
fun RevierWeekForecastRow(day: DailyForecast, weekMin: Int, weekMax: Int) {
    val range = (weekMax - weekMin).coerceAtLeast(1).toFloat()
    val startFrac = ((day.lowTemp - weekMin) / range).coerceIn(0f, 1f)
    val endFrac = ((day.highTemp - weekMin) / range).coerceIn(0f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(day.dayLabel, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp, modifier = Modifier.width(40.dp))
        Text(iconToEmoji(day.condition), fontSize = 20.sp, modifier = Modifier.width(32.dp))
        if (day.maxPrecipProb > 0) {
            Text("${day.maxPrecipProb}%", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.width(36.dp))
        } else {
            Spacer(modifier = Modifier.width(36.dp))
        }
        Text("${day.lowTemp}°", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.1f))) {
            val cs: Array<Pair<Float, Color>> = arrayOf(
                0f to Color.Transparent,
                startFrac to Color.Transparent,
                startFrac to Color(0xFF4FC3F7),
                endFrac to Color(0xFFFFB74D),
                endFrac to Color.Transparent,
                1f to Color.Transparent
            )
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colorStops = cs)))
        }
        Text("${day.highTemp}°", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun WindDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun TideContent(
    station: TideStationData?,
    events: List<TideEvent>,
    loading: Boolean = false
) {
    val now = LocalDateTime.now()
    val windowStart = now.minusHours(18)
    val windowEnd = now.plusHours(18)

    val windowEvents = remember(events, now.hour) {
        events.mapNotNull { event ->
            try {
                val cleanTs = event.timestamp
                    .replace("T", " ")
                    .replace(Regex("Z$"), "")
                    .replace(Regex("\\+\\d{2}:\\d{2}$"), "")
                    .replace(Regex("\\+\\d{2}$"), "")
                    .trim()
                val dt = try {
                    LocalDateTime.parse(cleanTs, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                } catch (_: Exception) {
                    LocalDateTime.parse(cleanTs, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }
                event to dt
            } catch (_: Exception) {
                null
            }
        }.filter { (_, dt) ->
            !dt.isBefore(windowStart) && !dt.isAfter(windowEnd)
        }
    }

    // Gezeitengrundwerte
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "GEZEITENGRUNDWERTE",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TideBasisTile("MHW", station?.meanHighWater?.let { "%.2f m".format(it) } ?: "3.10 m")
                TideBasisTile("MNW", station?.meanLowWater?.let { "%.2f m".format(it) } ?: "0.67 m")
                TideBasisTile("MTH", "2.43 m")
            }
        }
    }

    // Wasserstandsvorhersage
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "WASSERSTANDSVORHERSAGE",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                station?.gaugeLabel ?: "Station wählen",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            val updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            Text(
                "Ausgegeben heute, $updateTime Uhr",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val displayEvents = events.filter {
                    try {
                        val cleanTs = it.timestamp
                            .replace("T", " ")
                            .replace(Regex("Z$"), "")
                            .replace(Regex("\\+\\d{2}:\\d{2}$"), "")
                            .replace(Regex("\\+\\d{2}$"), "")
                            .trim()
                        val dt = try {
                            LocalDateTime.parse(
                                cleanTs,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            )
                        } catch (_: Exception) {
                            LocalDateTime.parse(cleanTs, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        }
                        dt.isAfter(LocalDateTime.now())
                    } catch (e: Exception) {
                        false
                    }
                }.take(3)

                if (displayEvents.isNotEmpty()) {
                    displayEvents.forEach { ev ->
                        val timeStr = try {
                            val cleanTs = ev.timestamp
                                .replace(Regex("\\+\\d{2}:\\d{2}$"), "")
                                .replace(Regex("\\+\\d{2}$"), "")
                                .trim()
                            val dt = try {
                                LocalDateTime.parse(
                                    cleanTs,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                )
                            } catch (_: Exception) {
                                LocalDateTime.parse(cleanTs, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            }
                            dt.format(DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (_: Exception) {
                            ev.timestamp.substringAfter(" ").take(5)
                        }
                        TideEventTile(
                            Modifier.weight(1f),
                            ev.type,
                            timeStr,
                            "%.2f m".format(ev.value ?: 0.0),
                            "+/- 0,0"
                        )
                    }
                } else {
                    TideEventTile(Modifier.weight(1f), "HW", "--:--", "-.-- m", "+/- 0,0")
                    TideEventTile(Modifier.weight(1f), "NW", "--:--", "-.-- m", "+/- 0,0")
                    TideEventTile(Modifier.weight(1f), "HW", "--:--", "-.-- m", "+/- 0,0")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "LOKALER WASSERSTANDSVERLAUF · SKN",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 12.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
            ) {
                if (windowEvents.size < 2) {
                    Text(
                        if (loading) "Lade Daten..." else "Keine Daten verfügbar",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    TideCurveCanvas(windowEvents.map { it.first }, windowStart, windowEnd, now)
                }
            }
        }
    }
}

@Composable
private fun TideCurveCanvas(
    events: List<TideEvent>,
    windowStart: LocalDateTime,
    windowEnd: LocalDateTime,
    now: LocalDateTime
) {
    val tideColor = Color(0xFF4FC3F7)
    val gridColor = Color.White.copy(alpha = 0.1f)
    val nowColor = Color(0xFFFF5252)
    val gradientColor = Color(0xFF4FC3F7).copy(alpha = 0.15f)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val labelTextSize = with(density) { 10.sp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val plotPaddingTop = 15.dp.toPx()
        val plotPaddingBottom = 25.dp.toPx()
        val plotHeight = height - plotPaddingTop - plotPaddingBottom
        val plotBottomY = height - plotPaddingBottom

        val windowMinutes = java.time.Duration.between(windowStart, windowEnd).toMinutes().toDouble()
        if (windowMinutes <= 0) return@Canvas

        val pts = events.mapNotNull { ev ->
            try {
                val cleanTs = ev.timestamp
                    .replace("T", " ")
                    .replace(Regex("Z$"), "")
                    .replace(Regex("\\+\\d{2}:\\d{2}$"), "")
                    .replace(Regex("\\+\\d{2}$"), "")
                    .trim()
                val dt = try {
                    LocalDateTime.parse(cleanTs, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                } catch (_: Exception) {
                    LocalDateTime.parse(cleanTs, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }
                val minutesFromStart =
                    java.time.Duration.between(windowStart, dt).toMinutes().toDouble()
                Triple(minutesFromStart, ev.value ?: 0.0, ev.type)
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.first }

        if (pts.isEmpty()) return@Canvas

        val values = pts.map { it.second }
        val maxVal = (values.maxOrNull() ?: 4.0)
        val minVal = (values.minOrNull() ?: 0.0)
        val pad = ((maxVal - minVal) * 0.15).coerceAtLeast(0.3)
        val yMax = maxVal + pad
        val yMin = minVal - pad
        val yRange = (yMax - yMin).coerceAtLeast(0.5)

        fun yForLevel(level: Double): Float =
            plotBottomY - ((level - yMin) / yRange * plotHeight).toFloat()

        fun xForMinute(min: Double): Float =
            (min / windowMinutes * width).toFloat()

        // Grid lines
        for (frac in listOf(0f, 0.5f, 1f)) {
            val y = plotPaddingTop + plotHeight * frac
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Time labels
        val hourPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            alpha = 100
            textSize = labelTextSize
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        var hourTick = windowStart.withMinute(0).withSecond(0).withNano(0)
        hourTick = hourTick.plusHours(((6 - hourTick.hour % 6) % 6).toLong())
        while (!hourTick.isAfter(windowEnd)) {
            val minutes = java.time.Duration.between(windowStart, hourTick).toMinutes().toDouble()
            val x = xForMinute(minutes)
            drawContext.canvas.nativeCanvas.drawText(
                "%02d:00".format(hourTick.hour),
                x,
                height - 5.dp.toPx(),
                hourPaint
            )
            hourTick = hourTick.plusHours(6)
        }

        // Curve
        if (pts.size >= 2) {
            val path = Path()
            val fillPath = Path()
            var started = false
            val step = 4
            for (xPx in 0..width.toInt() step step) {
                val minute = xPx.toDouble() / width * windowMinutes
                val before = pts.lastOrNull { it.first <= minute }
                val after = pts.firstOrNull { it.first > minute }
                val level: Double = when {
                    before != null && after != null -> {
                        val span = after.first - before.first
                        val progress = if (span > 0.0) ((minute - before.first) / span) else 0.0
                        val cosInterp = (1 - kotlin.math.cos(progress * Math.PI)) / 2.0
                        before.second + (after.second - before.second) * cosInterp
                    }
                    before != null -> before.second
                    after != null -> after.second
                    else -> (yMax + yMin) / 2.0
                }
                val yPos = yForLevel(level)
                if (!started) {
                    path.moveTo(xPx.toFloat(), yPos)
                    fillPath.moveTo(xPx.toFloat(), plotBottomY)
                    fillPath.lineTo(xPx.toFloat(), yPos)
                    started = true
                } else {
                    path.lineTo(xPx.toFloat(), yPos)
                    fillPath.lineTo(xPx.toFloat(), yPos)
                }
            }
            fillPath.lineTo(width, plotBottomY)
            fillPath.close()

            drawPath(fillPath, color = gradientColor)
            drawPath(
                path = path,
                color = tideColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Points
            for ((minutes, level, type) in pts) {
                if (minutes < 0 || minutes > windowMinutes) continue
                val cx = xForMinute(minutes)
                val cy = yForLevel(level)
                drawCircle(
                    color = if (type == "HW") Color(0xFF4FC3F7) else Color(0xFFFFB74D),
                    radius = 4.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        // Now line
        val nowMinutes = java.time.Duration.between(windowStart, now).toMinutes().toDouble()
        if (nowMinutes in 0.0..windowMinutes) {
            val nowX = xForMinute(nowMinutes)
            drawLine(
                color = nowColor,
                start = Offset(nowX, plotPaddingTop),
                end = Offset(nowX, plotBottomY),
                strokeWidth = 2.dp.toPx()
            )
            val nowPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.RED
                textSize = labelTextSize
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText("JETZT", nowX, plotPaddingTop - 5.dp.toPx(), nowPaint)
        }
    }
}

@Composable
fun TideBasisTile(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
fun TideEventTile(modifier: Modifier, type: String, time: String, height: String, diff: String) {
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (type == "HW") Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = if (type == "HW") Color(0xFF4FC3F7) else Color(0xFFFFB74D),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(type, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(time, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(height, color = Color.White, fontSize = 12.sp)
            Text(diff, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
    ) {
        content()
    }
}

@Composable
fun StationSelectionDialog(
    stations: List<TideStationData>,
    selectedStation: TideStationData?,
    onDismiss: () -> Unit,
    onStationSelected: (TideStationData) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Revier auswählen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Ostfriesische Inseln", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                stations.forEach { station ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (station == selectedStation) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onStationSelected(station) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (station == selectedStation) {
                                Box(modifier = Modifier.size(4.dp, 16.dp).background(Color(0xFF4FC3F7), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(station.gaugeLabel ?: station.area, color = Color.White, fontSize = 16.sp)
                        }
                        Text("17°", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private fun iconToEmoji(iconOrCondition: String?): String = when (iconOrCondition) {
    "clear-day", "clear-night", "dry" -> "☀️"
    "partly-cloudy-day", "partly-cloudy-night" -> "⛅"
    "cloudy" -> "☁️"
    "fog" -> "🌫️"
    "rain" -> "🌧️"
    "sleet" -> "🌨️"
    "snow" -> "❄️"
    "hail" -> "🧊"
    "thunderstorm" -> "⛈️"
    "wind" -> "💨"
    else -> "☀️"
}

private fun aggregateToDays(hourlyData: List<WeatherDto>): List<DailyForecast> {
    if (hourlyData.isEmpty()) return emptyList()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dayNames = listOf("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa")
    val today = LocalDate.now()
    val berlinZone = ZoneId.of("Europe/Berlin")
    val grouped = hourlyData.groupBy { dto ->
        try {
            val utc = OffsetDateTime.parse(dto.timestamp ?: "")
            utc.atZoneSameInstant(berlinZone).toLocalDate().format(fmt)
        } catch (_: Exception) {
            dto.timestamp?.take(10) ?: ""
        }
    }.filterKeys { it.isNotEmpty() }
    return grouped.entries.take(7).mapIndexed { index, (dateStr, hours) ->
        val date = try { LocalDate.parse(dateStr, fmt) } catch (_: Exception) { today.plusDays(index.toLong()) }
        val dayLabel = if (date == today) "Heute" else dayNames[date.dayOfWeek.value % 7]
        val temps = hours.mapNotNull { it.temperature }
        val precipProbs = hours.mapNotNull { it.precipitationProbability }
        val icons = hours.mapNotNull { it.icon }
        val dominantIcon = icons.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "clear-day"
        DailyForecast(
            dayLabel = dayLabel,
            condition = dominantIcon,
            highTemp = temps.maxOrNull()?.toInt() ?: 0,
            lowTemp = temps.minOrNull()?.toInt() ?: 0,
            maxWind = 0,
            totalPrecip = 0.0,
            maxPrecipProb = precipProbs.maxOrNull() ?: 0,
            minVisibility = null
        )
    }
}
