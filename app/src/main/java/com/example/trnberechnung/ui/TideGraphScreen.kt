package com.example.trnberechnung.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.model.TideEvent
import com.example.trnberechnung.ui.theme.*
import com.example.trnberechnung.viewmodel.TideViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TideGraphScreen(viewModel: TideViewModel) {
    val tideEvents by viewModel.currentTideEvents.collectAsState()
    val allStations by viewModel.allStations.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val tideLoading by viewModel.tideLoading.collectAsState()

    var stationDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (allStations.isEmpty()) {
            viewModel.loadData()
        }
    }

    val todayStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val todayEvents = tideEvents.filter { it.timestamp.startsWith(todayStr) }

    val now = LocalDateTime.now()
    val windowStart = now.minusHours(18)
    val windowEnd = now.plusHours(18)
    val windowEvents = remember(tideEvents, now.hour) {
        tideEvents.mapNotNull { event ->
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
            } catch (_: Exception) { null }
        }.filter { (_, dt) ->
            !dt.isBefore(windowStart) && !dt.isAfter(windowEnd)
        }
    }

    LaunchedEffect(allStations) {
        if (selectedStation == null && allStations.isNotEmpty()) {
            viewModel.selectStation(allStations.first())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NauticalBackground)
            .padding(16.dp)
    ) {

        Text(
            "TIDENKURVE",
            modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag("screen_header_tides"),
            style = MaterialTheme.typography.labelMedium,
            color = NauticalTextSecondary,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            OutlinedTextField(
                value = selectedStation?.gaugeLabel ?: selectedStation?.area ?: "Station wählen…",
                onValueChange = {},
                readOnly = true,
                label = { Text("Pegelstation", color = NauticalTextSecondary) },
                trailingIcon = {
                    IconButton(onClick = { stationDropdownExpanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NauticalPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { stationDropdownExpanded = true },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NauticalPrimary,
                    unfocusedBorderColor = NauticalDivider,
                    focusedLabelColor = NauticalPrimary,
                    unfocusedLabelColor = NauticalTextSecondary,
                    cursorColor = NauticalPrimary,
                    focusedTextColor = NauticalTextPrimary,
                    unfocusedTextColor = NauticalTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = false
            )

            // Clickable overlay because OutlinedTextField with enabled=false or readOnly might block clicks
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { stationDropdownExpanded = true }
            )

            DropdownMenu(
                expanded = stationDropdownExpanded,
                onDismissRequest = { stationDropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(NauticalSurface)
                    .border(1.dp, NauticalDivider, RoundedCornerShape(8.dp))
            ) {
                allStations.forEach { station ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                station.gaugeLabel ?: station.area,
                                color = NauticalTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (station == selectedStation) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            viewModel.selectStation(station)
                            stationDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(bottom = 16.dp)
                .border(1.dp, NauticalDivider, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = NauticalSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Animated Wave Background
                TideWaveAnimation(modifier = Modifier.fillMaxSize())

                if (windowEvents.size < 2) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        TideCurveCanvas(emptyList(), windowStart, windowEnd, now, selectedStation)
                        Text(
                            if (tideLoading) "Lade Daten..." else "Keine Daten – bitte Station auswählen",
                            color = NauticalTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 10.dp, start = 45.dp, end = 15.dp)) {
                        TideCurveCanvas(windowEvents.map { it.first }, windowStart, windowEnd, now, selectedStation)
                    }
                }
            }
        }

        Text(
            "HEUTE",
            style = MaterialTheme.typography.labelMedium,
            color = NauticalTextSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, NauticalDivider, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = NauticalSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (todayEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (tideLoading) "Lade Gezeitendaten..." else "Keine Gezeitendaten für heute verfügbar",
                        color = NauticalTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(todayEvents) { event ->
                        TideEventRow(event)
                        HorizontalDivider(
                            color = NauticalDivider.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
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
    now: LocalDateTime,
    station: com.example.trnberechnung.model.TideStationData?
) {
    val tickColor = NauticalTextSecondary
    val tideColor = NauticalTideBlue
    val gridColor = NauticalGridLine
    val nowColor = NauticalNowLine
    val gradientColor = NauticalTideBlue.copy(alpha = 0.15f)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val labelTextSize = with(density) { 9.sp.toPx() }
    val axisLabelTextSize = with(density) { 10.sp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val plotPaddingTop = 20f
        val plotPaddingBottom = 30f
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
                val minutesFromStart = java.time.Duration.between(windowStart, dt).toMinutes().toDouble()
                Triple(minutesFromStart, ev.value ?: 0.0, ev.type)
            } catch (_: Exception) { null }
        }.sortedBy { it.first }

        val values = pts.map { it.second }
        val maxVal = (values.maxOrNull() ?: 4.0).coerceAtLeast(3.5)
        val minVal = (values.minOrNull() ?: 0.0).coerceAtMost(0.5)
        val pad = ((maxVal - minVal) * 0.1).coerceAtLeast(0.2)
        val yMax = maxVal + pad
        val yMin = minVal - pad
        val yRange = (yMax - yMin).coerceAtLeast(1.0)

        fun yForLevel(level: Double): Float =
            plotBottomY - ((level - yMin) / yRange * plotHeight).toFloat()

        fun xForMinute(min: Double): Float =
            (min / windowMinutes * width).toFloat()

        // Draw Y-Axis Labels and Grid Lines
        val axisPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 122, 138, 158)
            textSize = axisLabelTextSize
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }

        val step = if (yRange > 5) 1.0 else 0.5
        var currentLevel = (yMin / step).toInt() * step
        while (currentLevel <= yMax) {
            if (currentLevel >= yMin) {
                val y = yForLevel(currentLevel)
                drawLine(
                    color = gridColor.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "%.1f".format(currentLevel),
                    -10f,
                    y + axisLabelTextSize / 3,
                    axisPaint
                )
            }
            currentLevel += step
        }

        // Draw MHW / MNW markers if available
        station?.meanHighWater?.let { mhw ->
            val y = yForLevel(mhw)
            drawLine(Color.Red.copy(alpha = 0.3f), Offset(0f, y), Offset(width, y), strokeWidth = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            drawContext.canvas.nativeCanvas.drawText("MHW", width + 5f, y + axisLabelTextSize / 3, axisPaint.apply { textAlign = android.graphics.Paint.Align.LEFT; color = android.graphics.Color.RED; alpha = 100 })
        }
        station?.meanLowWater?.let { mnw ->
            val y = yForLevel(mnw)
            drawLine(Color.Blue.copy(alpha = 0.3f), Offset(0f, y), Offset(width, y), strokeWidth = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            drawContext.canvas.nativeCanvas.drawText("MNW", width + 5f, y + axisLabelTextSize / 3, axisPaint.apply { textAlign = android.graphics.Paint.Align.LEFT; color = android.graphics.Color.BLUE; alpha = 100 })
        }

        val hourPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 122, 138, 158)
            textSize = labelTextSize
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        var hourTick = windowStart.withMinute(0).withSecond(0).withNano(0)
        while (!hourTick.isAfter(windowEnd)) {
            val minutes = java.time.Duration.between(windowStart, hourTick).toMinutes().toDouble()
            val x = xForMinute(minutes)
            if (hourTick.hour % 4 == 0) {
                drawLine(
                    color = gridColor.copy(alpha = 0.15f),
                    start = Offset(x, plotPaddingTop),
                    end = Offset(x, plotBottomY),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "%02d:00".format(hourTick.hour),
                    x,
                    height - 6f,
                    hourPaint
                )
            }
            hourTick = hourTick.plusHours(1)
        }

        if (pts.size >= 2) {
            val path = Path()
            val fillPath = Path()
            var started = false
            val drawStep = 2
            for (xPx in 0..width.toInt() step drawStep) {
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

            drawPath(fillPath, brush = Brush.verticalGradient(listOf(gradientColor, Color.Transparent), startY = plotPaddingTop, endY = plotBottomY))
            drawPath(
                path = path,
                color = tideColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            for ((minutes, level, type) in pts) {
                if (minutes < 0 || minutes > windowMinutes) continue
                val cx = xForMinute(minutes)
                val cy = yForLevel(level)
                drawCircle(
                    color = if (type == "HW") NauticalPrimary else NauticalSecondary,
                    radius = 6f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White,
                    radius = 2.5f,
                    center = Offset(cx, cy)
                )
            }
        }

        val nowMinutes = java.time.Duration.between(windowStart, now).toMinutes().toDouble()
        if (nowMinutes in 0.0..windowMinutes) {
            val nowX = xForMinute(nowMinutes)
            drawLine(
                color = nowColor,
                start = Offset(nowX, plotPaddingTop),
                end = Offset(nowX, plotBottomY),
                strokeWidth = 3f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
            )
            val nowPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(255, 255, 82, 82)
                textSize = labelTextSize
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText("JETZT", nowX, plotPaddingTop - 5f, nowPaint)
        }
    }
}

@Composable
fun TideWaveAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val waveColor = NauticalTideBlue.copy(alpha = 0.05f)
        val path = Path()

        val baseHeight = height * 0.7f
        val amplitude = 20f

        path.moveTo(0f, height)
        for (x in 0..width.toInt() step 5) {
            val y = baseHeight + sin(x * 0.01f + phase) * amplitude
            path.lineTo(x.toFloat(), y)
        }
        path.lineTo(width, height)
        path.close()

        drawPath(path, color = waveColor)

        // Second wave
        val path2 = Path()
        path2.moveTo(0f, height)
        for (x in 0..width.toInt() step 5) {
            val y = baseHeight + amplitude * 0.5f + sin(x * 0.015f - phase * 0.8f) * amplitude * 1.2f
            path2.lineTo(x.toFloat(), y)
        }
        path2.lineTo(width, height)
        path2.close()
        drawPath(path2, color = waveColor.copy(alpha = 0.03f))
    }
}

@Composable
fun TideEventRow(event: TideEvent) {
    val isHigh = event.type == "HW"

    val timeStr = try {
        val cleanTs = event.timestamp
            .replace(Regex("\\+\\d{2}:\\d{2}$"), "")
            .replace(Regex("\\+\\d{2}$"), "")
            .trim()
        val dt = java.time.LocalDateTime.parse(
            cleanTs,
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )
        dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        event.timestamp.substringAfter(" ").take(5)
    }

    val heightStr = event.value?.let { "%.2f m".format(it) } ?: "–"
    val typeStr = if (isHigh) "Hochwasser" else "Niedrigwasser"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (isHigh) "▲" else "▼",
            color = if (isHigh) NauticalPrimary else NauticalSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            timeStr,
            fontWeight = FontWeight.Bold,
            color = NauticalTextPrimary,
            fontSize = 16.sp,
            modifier = Modifier.width(60.dp)
        )
        Text(
            typeStr,
            color = NauticalTextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            heightStr,
            fontWeight = FontWeight.Bold,
            color = if (isHigh) NauticalPrimary else NauticalTextSecondary,
            fontSize = 16.sp
        )
    }
}
