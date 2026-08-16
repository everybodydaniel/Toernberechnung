package com.example.trnberechnung.ui.nauti

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.dto.WeatherDto
import com.example.trnberechnung.logic.TideTimes
import com.example.trnberechnung.model.TideEvent
import com.example.trnberechnung.model.TideStationData
import com.example.trnberechnung.nauti.NautiAction
import com.example.trnberechnung.nauti.NautiStationMatcher
import com.example.trnberechnung.ui.components.TideNodeBlue
import com.example.trnberechnung.ui.components.TideNodeCyan
import com.example.trnberechnung.ui.components.TideNodeInk
import com.example.trnberechnung.ui.components.tideNodeGlass
import com.example.trnberechnung.ui.iconToEmoji
import com.example.trnberechnung.ui.translateCondition
import com.example.trnberechnung.ui.windDirectionToText
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Which set of measurements a Nauti reply wants shown inline. */
enum class NautiWidgetKind {
    WEATHER,
    TIDES,
    WATER_LEVEL,
}

/**
 * The widget an action renders, or `null` if the action is a command rather than a question.
 *
 * Nauti is forbidden from naming figures itself (see `NautiSystemPrompt`); it answers a data
 * question with an action and a one-line intro. This is where that action turns into the real
 * numbers, in the chat, instead of throwing the skipper over to the Revier tab mid-conversation.
 */
fun widgetKindOf(action: NautiAction): NautiWidgetKind? =
    when (action) {
        is NautiAction.ShowWeather -> NautiWidgetKind.WEATHER
        is NautiAction.ShowTides -> NautiWidgetKind.TIDES
        is NautiAction.ShowBshWaterLevel -> NautiWidgetKind.WATER_LEVEL
        else -> null
    }

/** The harbour a widget action refers to, or `null` when the model named no harbour. */
fun harbourIdOf(action: NautiAction): String? =
    when (action) {
        is NautiAction.ShowWeather -> action.harbourId
        is NautiAction.ShowTides -> action.harbourId
        is NautiAction.ShowBshWaterLevel -> action.harbourId
        else -> null
    }

/**
 * Live measurements for one harbour, rendered inside a chat bubble.
 *
 * Reads [stations] - the same list the Revier screen uses, already carrying weather and the matched
 * BSH tide forecast - so there is no second network path that could disagree with the Revier tab.
 * While the app is still loading, the card says so rather than showing a wall of dashes.
 */
@Composable
fun NautiDataWidget(
    kind: NautiWidgetKind,
    harbourId: String?,
    stations: List<TideStationData>,
    onOpenRevier: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Remembered because this scans every station: the widget lives inside a chat list that
    // recomposes for reasons unrelated to the data behind it.
    val station = remember(harbourId, stations) {
        NautiStationMatcher.nearestStation(harbourId, stations)
    }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val titleColor = if (isDark) Color(0xFFF8FAFC) else TideNodeInk
    val labelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF62666C)
    val accent = if (isDark) TideNodeCyan else TideNodeBlue

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .tideNodeGlass(cornerRadius = 20.dp, elevation = 4.dp, alpha = 0.72f)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("nauti_widget_${kind.name.lowercase()}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = station?.gaugeLabel ?: station?.area ?: "Revier unbekannt",
                color = titleColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text =
                    when (kind) {
                        NautiWidgetKind.WEATHER -> "WETTER"
                        NautiWidgetKind.TIDES -> "GEZEITEN"
                        NautiWidgetKind.WATER_LEVEL -> "BSH-PEGEL"
                    },
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
            )
        }

        Spacer(Modifier.height(10.dp))

        when (kind) {
            NautiWidgetKind.WEATHER -> WeatherBody(station, titleColor, labelColor)
            NautiWidgetKind.TIDES -> TideBody(station, titleColor, labelColor)
            NautiWidgetKind.WATER_LEVEL -> WaterLevelBody(station, titleColor, labelColor)
        }

        TextButton(
            onClick = { onOpenRevier(harbourId) },
            modifier = Modifier.align(Alignment.End).testTag("nauti_widget_open_revier"),
        ) {
            Text("Im Revier öffnen", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun WeatherBody(
    station: TideStationData?,
    titleColor: Color,
    labelColor: Color,
) {
    // The station's forecast starts at midnight, so the first entry would describe last night's
    // sky. Pick the hour closest to now instead.
    val current = station?.weatherForecast?.nearestToNow()
    // The observation endpoint routinely reports a temperature but leaves wind fields null, so
    // every value falls back to that forecast hour - the same gap-filling the Revier screen does in
    // TideViewModel.mergeWithForecast. Without it the card showed "–" for wind while the Revier tab
    // showed real knots for the same harbour.
    val temperature = station?.temperature ?: current?.temperature
    val windSpeed = station?.windSpeed ?: current?.windSpeed
    val gustSpeed = station?.windGustSpeed ?: current?.windGustSpeed
    val windDirection = station?.windDirection ?: current?.windDirection
    if (station == null || temperature == null) {
        LoadingHint(labelColor)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(iconToEmoji(current?.icon ?: current?.condition), fontSize = 34.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "${temperature.toInt()}°",
                color = titleColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                translateCondition(current?.icon ?: current?.condition),
                color = labelColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    MeasurementRow("Grundwind", windSpeed.toKnotsLabel(), titleColor, labelColor)
    MeasurementRow("Böen", gustSpeed.toKnotsLabel(), titleColor, labelColor)
    MeasurementRow(
        label = "Richtung",
        value = windDirection?.let { "${windDirectionToText(it)} · $it°" } ?: "–",
        titleColor = titleColor,
        labelColor = labelColor,
    )
}

@Composable
private fun TideBody(
    station: TideStationData?,
    titleColor: Color,
    labelColor: Color,
) {
    val eventsWithTime = remember(station) { TideTimes.sortedByTime(station?.events.orEmpty()) }
    if (eventsWithTime.isEmpty()) {
        LoadingHint(labelColor)
        return
    }
    val now = LocalDateTime.now()
    val next = eventsWithTime.firstOrNull { it.second.isAfter(now) }
    val isRising = next?.first?.type == "HW"

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isRising) Icons.Default.ArrowOutward else Icons.Default.SouthEast,
            contentDescription = null,
            tint = titleColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                if (isRising) "Steigendes Wasser" else "Fallendes Wasser",
                color = titleColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            next?.let {
                Text(
                    countdownLabel(Duration.between(now, it.second).toMinutes()),
                    color = labelColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    MeasurementRow(
        label = "Nächstes Hochwasser",
        value = eventsWithTime.nextTimeLabel(now, "HW"),
        titleColor = titleColor,
        labelColor = labelColor,
    )
    MeasurementRow(
        label = "Nächstes Niedrigwasser",
        value = eventsWithTime.nextTimeLabel(now, "NW"),
        titleColor = titleColor,
        labelColor = labelColor,
    )
}

@Composable
private fun WaterLevelBody(
    station: TideStationData?,
    titleColor: Color,
    labelColor: Color,
) {
    if (station == null) {
        LoadingHint(labelColor)
        return
    }
    val level = station.waterLevel
    if (level != null) {
        Text(
            "%.2f m".format(level),
            color = titleColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
    }
    MeasurementRow("MHW", station.meanHighWater.toMetreLabel(), titleColor, labelColor)
    MeasurementRow("MNW", station.meanLowWater.toMetreLabel(), titleColor, labelColor)
    if (level == null && station.meanHighWater == null && station.meanLowWater == null) {
        LoadingHint(labelColor)
    }
}

@Composable
private fun MeasurementRow(
    label: String,
    value: String,
    titleColor: Color,
    labelColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoadingHint(labelColor: Color) =
    Text(
        "Daten werden geladen …",
        color = labelColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )

private val WIDGET_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

private fun List<Pair<TideEvent, LocalDateTime>>.nextTimeLabel(
    now: LocalDateTime,
    type: String,
): String =
    firstOrNull { it.first.type == type && it.second.isAfter(now) }
        ?.second
        ?.format(WIDGET_TIME_FORMAT)
        ?.plus(" Uhr")
        ?: "–"

private fun countdownLabel(minutes: Long): String =
    when {
        minutes <= 0 -> "jetzt"
        minutes < 60 -> "in $minutes Min."
        else -> "in ${minutes / 60} Std. ${minutes % 60} Min."
    }

/**
 * The forecast hour closest to now.
 *
 * Reuses [TideTimes.parse], which already tolerates every timestamp flavour these endpoints emit
 * (`T` separator or space, with or without a trailing `Z` / offset).
 */
private fun List<WeatherDto>.nearestToNow(): WeatherDto? {
    val now = LocalDateTime.now()
    return mapNotNull { entry -> TideTimes.parse(entry.timestamp)?.let { entry to it } }
        .minByOrNull { kotlin.math.abs(Duration.between(now, it.second).toMinutes()) }
        ?.first
        ?: firstOrNull()
}

/** Bright Sky reports km/h; the bridge speaks knots. */
private fun Double?.toKnotsLabel(): String =
    this?.let { "${(it / 1.852).toInt()} kn" } ?: "–"

private fun Double?.toMetreLabel(): String =
    this?.let { "%.2f m".format(it) } ?: "–"
