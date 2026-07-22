package com.example.trnberechnung.ui

import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.trnberechnung.dto.WeatherDto
import com.example.trnberechnung.model.TideStationData
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
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
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }

    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        AndroidView(
            factory = { ctx ->
                val frameLayout = FrameLayout(ctx)
                frameLayout.addView(mapView)

                mapView.onCreate(null)
                mapView.getMapAsync { map ->
                    map.uiSettings.isZoomGesturesEnabled = false
                    map.uiSettings.isScrollGesturesEnabled = false
                    map.uiSettings.isRotateGesturesEnabled = false
                    map.uiSettings.isTiltGesturesEnabled = false

                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(53.7, 7.5))
                        .zoom(7.5)
                        .build()

                    map.setStyle(
                        Style.Builder().fromJson(
                            """
                            {
                              "version": 8,
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
                        // Add wind arrow icon to style
                        val drawable = ContextCompat.getDrawable(ctx, R.drawable.ic_wind_arrow)
                        drawable?.let {
                            val bitmap = Bitmap.createBitmap(
                                it.intrinsicWidth,
                                it.intrinsicHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(bitmap)
                            it.setBounds(0, 0, canvas.width, canvas.height)
                            it.draw(canvas)
                            style.addImage("wind-arrow", bitmap)
                        }

                        val sm = SymbolManager(mapView, map, style)
                        sm.iconAllowOverlap = true
                        sm.textAllowOverlap = true
                        symbolManager = sm

                        updateWindMarkers(sm, stations, currentWeather)
                    }
                }
                frameLayout
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    LaunchedEffect(stations, currentWeather, symbolManager) {
        symbolManager?.let { sm ->
            updateWindMarkers(sm, stations, currentWeather)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            } catch (e: Exception) {
                Log.e("WindMapComponent", "Error during map cleanup", e)
            }
        }
    }
}

private fun updateWindMarkers(
    symbolManager: SymbolManager,
    stations: List<TideStationData>,
    currentWeather: WeatherDto?
) {
    symbolManager.deleteAll()

    stations.forEach { station ->
        val windSpeed = (currentWeather?.windSpeed?.div(1.852))?.toInt() ?: 0
        val windDirection = currentWeather?.windDirection ?: 0

        if (windSpeed > 0) {
            symbolManager.create(
                SymbolOptions()
                    .withLatLng(LatLng(station.latitude, station.longitude))
                    .withIconImage("wind-arrow")
                    .withIconRotate(windDirection.toFloat() + 180f)
                    .withIconSize(1.0f)
                    .withTextField("$windSpeed kn")
                    .withTextOffset(arrayOf(0f, 1.5f))
                    .withTextColor(ColorUtils.colorToRgbaString(AndroidColor.WHITE))
                    .withTextHaloColor(ColorUtils.colorToRgbaString(AndroidColor.parseColor("#00BFA6")))
                    .withTextHaloWidth(2f)
                    .withTextSize(12f)
            )
        }
    }
}
