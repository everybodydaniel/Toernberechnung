package com.example.trnberechnung.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import androidx.core.content.ContextCompat
import com.example.trnberechnung.R
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.utils.ColorUtils
import android.graphics.Color as AndroidColor

@Composable
fun WindMapComponent(
    modifier: Modifier = Modifier,
    stations: List<TideStationData>,
    currentWeather: WeatherDto?
) {
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }

    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    onCreate(null)
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

                            val drawable = ContextCompat.getDrawable(ctx, R.drawable.ic_wind_arrow)
                            drawable?.let {
                                val bitmap = Bitmap.createBitmap(
                                    48, 48,
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = Canvas(bitmap)
                                it.setBounds(0, 0, 48, 48)
                                it.draw(canvas)
                                style.addImage("wind-arrow", bitmap)
                                Log.d("WindMapComponent", "wind-arrow icon added to style")
                            }

                            val sm = SymbolManager(this, map, style)
                            sm.iconAllowOverlap = true
                            sm.iconIgnorePlacement = true
                            sm.textAllowOverlap = true
                            sm.textIgnorePlacement = true

                            symbolManager = sm

                            updateWindMarkers(sm, stations, currentWeather)
                        }
                    }
                }
            },
            update = { _ ->
                // Basic view updates handled by LaunchedEffect
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { view ->
                try {
                    view.onPause()
                    view.onStop()
                    view.onDestroy()
                } catch (e: Exception) {
                    Log.e("WindMapComponent", "Error during map cleanup", e)
                }
            }
        )
    }

    LaunchedEffect(stations, currentWeather, symbolManager) {
        symbolManager?.let { sm ->
            updateWindMarkers(sm, stations, currentWeather)
        }
    }
}

private fun updateWindMarkers(
    symbolManager: SymbolManager,
    stations: List<TideStationData>,
    currentWeather: WeatherDto?
) {
    Log.d("WindMapComponent", "updateWindMarkers: count=${stations.size}")
    symbolManager.deleteAll()

    stations.forEach { station ->
        val isLive = currentWeather?.timestamp.isNullOrBlank()

        val ws: Double?
        val gs: Double?
        val wd: Int?

        if (isLive) {
            // Priority for Live: Station's own live data -> fallback to global selected station
            ws = station.windSpeed ?: currentWeather?.windSpeed
            gs = station.windGustSpeed ?: currentWeather?.windGustSpeed
            wd = station.windDirection ?: currentWeather?.windDirection ?: 0
        } else {
            // Priority for Forecast: Station's matching timestamp -> fallback to global selected station's forecast hour
            val forecastMatch = station.weatherForecast.find { it.timestamp == currentWeather?.timestamp }
            ws = forecastMatch?.windSpeed ?: currentWeather?.windSpeed
            gs = forecastMatch?.windGustSpeed ?: currentWeather?.windGustSpeed
            wd = forecastMatch?.windDirection ?: currentWeather?.windDirection ?: 0
        }

        val windSpeedKn = (ws?.div(1.852))?.toInt() ?: 0
        val gustSpeedKn = (gs?.div(1.852))?.toInt() ?: 0
        val dirText = getWindDirection16Point(wd)

        symbolManager.create(
            SymbolOptions()
                .withLatLng(LatLng(station.latitude, station.longitude))
                .withIconImage("wind-arrow")
                .withIconRotate(wd.toFloat() + 180f)
                .withIconSize(0.85f)
                .withTextField("$windSpeedKn/$gustSpeedKn\n$dirText")
                .withTextSize(12.5f)
                .withTextFont(arrayOf("Open Sans Regular", "Arial Unicode MS Regular"))
                .withTextColor(ColorUtils.colorToRgbaString(AndroidColor.WHITE))
                .withTextHaloColor(ColorUtils.colorToRgbaString(AndroidColor.BLACK))
                .withTextHaloWidth(3.0f)
                .withTextOffset(arrayOf(0f, 1.6f))
                .withTextJustify("center")
        )
    }
}

private fun getWindDirection16Point(degrees: Int): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = ((degrees + 11.25) / 22.5).toInt() % 16
    return directions[index]
}
