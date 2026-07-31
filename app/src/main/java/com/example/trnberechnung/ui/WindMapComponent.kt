package com.example.trnberechnung.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.trnberechnung.dto.WeatherDto
import com.example.trnberechnung.model.TideStationData
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.example.trnberechnung.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.utils.ColorUtils
import android.graphics.Color as AndroidColor

@Composable
fun WindMapComponent(
    modifier: Modifier = Modifier,
    stations: List<TideStationData>,
    currentWeather: WeatherDto?,
    isLive: Boolean = false
) {
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView?.onStart()
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_STOP -> mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapView = this
                    onCreate(null)
                    onStart()
                    onResume()

                    getMapAsync { map ->
                        map.uiSettings.isZoomGesturesEnabled = true
                        map.uiSettings.isScrollGesturesEnabled = true
                        map.uiSettings.isRotateGesturesEnabled = false
                        map.uiSettings.isTiltGesturesEnabled = false

                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(53.7, 7.5))
                            .zoom(8.0)
                            .build()

                        map.setStyle(
                            Style.Builder().fromJson(
                                """
                                {
                                  "version": 8,
                                  "glyphs": "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf",
                                  "sources": {
                                    "osm": {
                                      "type": "raster",
                                      "tiles": ["https://tile.openstreetmap.de/{z}/{x}/{y}.png"],
                                      "tileSize": 256,
                                      "attribution": "&copy; OpenStreetMap contributors"
                                    }
                                  },
                                  "layers": [
                                    {"id": "osm-layer", "type": "raster", "source": "osm"}
                                  ]
                                }
                                """.trimIndent()
                            )
                        ) { style ->
                            Log.d("WindMapComponent", "Style loaded successfully")

                            // Generate 16 wind tiles (rounded box + arrow) as background icons
                            val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
                            directions.forEachIndexed { index, dir ->
                                val rotation = index * 22.5f
                                val tileBitmap = createWindTileBitmap(ctx, rotation)
                                style.addImage("wind-tile-$dir", tileBitmap)
                            }

                            val sm = SymbolManager(this@apply, map, style)
                            sm.iconAllowOverlap = true
                            sm.iconIgnorePlacement = true
                            sm.textAllowOverlap = true
                            sm.textIgnorePlacement = true

                            symbolManager = sm
                            Log.d("WindMapComponent", "SymbolManager initialized")
                        }
                    }
                }
            },
            update = { _ ->
                // Basic view updates handled by LaunchedEffect
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { _ ->
                // Lifecycle managed by DisposableEffect
            }
        )
    }

    LaunchedEffect(stations, currentWeather, symbolManager, isLive) {
        symbolManager?.let { sm ->
            // Small delay to ensure MapLibre surface is fully initialized
            delay(100)
            updateWindMarkers(sm, stations, currentWeather, isLive)
        }
    }
}

private fun findNearestForecast(forecast: List<WeatherDto>, targetTimestamp: String?): WeatherDto? {
    if (targetTimestamp == null || forecast.isEmpty()) return null
    val exact = forecast.find { it.timestamp == targetTimestamp }
    if (exact != null) return exact

    return try {
        val targetTime = java.time.OffsetDateTime.parse(targetTimestamp).toInstant().toEpochMilli()
        forecast.minByOrNull {
            val time = java.time.OffsetDateTime.parse(it.timestamp ?: "").toInstant().toEpochMilli()
            Math.abs(time - targetTime)
        }
    } catch (e: Exception) {
        null
    }
}

private fun updateWindMarkers(
    symbolManager: SymbolManager,
    stations: List<TideStationData>,
    currentWeather: WeatherDto?,
    isLive: Boolean
) {
    Log.d("WindMapComponent", "updateWindMarkers: count=${stations.size}, live=$isLive")
    symbolManager.deleteAll()

    val targetTimestamp = currentWeather?.timestamp

    stations.forEach { station ->
        val ws: Double?
        val gs: Double?
        val wd: Int?

        if (isLive && station.windSpeed != null && station.windDirection != null) {
            // Priority: Station's own live data
            ws = station.windSpeed
            gs = station.windGustSpeed
            wd = station.windDirection
        } else {
            // Fallback to forecast if live data missing or not in live mode
            val searchTs = if (isLive) {
                java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Berlin")).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } else {
                targetTimestamp
            }
            val forecastMatch = findNearestForecast(station.weatherForecast, searchTs)
            ws = forecastMatch?.windSpeed
            gs = forecastMatch?.windGustSpeed
            wd = forecastMatch?.windDirection
        }

        // Skip stations that don't have enough data to be meaningful
        if (ws == null || wd == null) return@forEach

        val finalGs = gs ?: ws // Fallback gust to wind speed if missing
        val windSpeedKn = (ws / 1.852).toInt()
        val gustSpeedKn = (finalGs / 1.852).toInt()
        val dirText = getWindDirection16Point(wd)

        symbolManager.create(
            SymbolOptions()
                .withLatLng(LatLng(station.latitude, station.longitude))
                .withIconImage("wind-tile-$dirText")
                .withIconSize(0.6f)
                .withTextField("$windSpeedKn/$gustSpeedKn\n$dirText")
                .withTextSize(9f)
                .withTextColor(ColorUtils.colorToRgbaString(AndroidColor.WHITE))
                .withTextOffset(arrayOf(0f, 0.4f))
                .withTextAnchor("center")
                .withTextJustify("center")
        )
        Log.d("WindMapComponent", "Created marker for ${station.gaugeLabel}: $windSpeedKn kn")
    }
}

private fun createWindTileBitmap(context: android.content.Context, rotation: Float): Bitmap {
    // A tile that accommodates an arrow at the top and 2 lines of text below
    val width = 100
    val height = 150
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Dark rounded background box - slightly more transparent
    val paint = Paint().apply {
        color = AndroidColor.parseColor("#E61B2A39") // ~90% alpha
        isAntiAlias = true
    }
    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, 14f, 14f, paint)

    // Draw the white arrow at the top
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_wind_arrow)?.mutate()
    drawable?.let {
        it.setTint(AndroidColor.WHITE)
        canvas.save()
        // Position arrow in the upper third
        canvas.rotate(rotation + 180f, width / 2f, 35f)
        val arrowSize = 34
        it.setBounds(width / 2 - arrowSize / 2, 18, width / 2 + arrowSize / 2, 18 + arrowSize)
        it.draw(canvas)
        canvas.restore()
    }

    return bitmap
}

private fun getWindDirection16Point(degrees: Int): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    // Ensure the index is positive even for negative degrees
    val index = (((degrees.toDouble() + 11.25) / 22.5).toInt() % 16 + 16) % 16
    return directions[index]
}
