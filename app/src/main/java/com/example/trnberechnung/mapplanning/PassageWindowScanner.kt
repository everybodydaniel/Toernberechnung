package com.example.trnberechnung.mapplanning

import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class PassageCandidateAssessment(
    val expectedWaypointCount: Int,
    val waypointClearances: List<ClearanceSample>,
    val allLegsValid: Boolean,
) {
    val isSafe: Boolean
        get() =
            expectedWaypointCount > 0 &&
                waypointClearances.size == expectedWaypointCount &&
                allLegsValid &&
                waypointClearances.all { sample ->
                    sample.isValid &&
                        sample.clearanceMeters != null &&
                        sample.clearanceMeters >= 0
                }

    val bottleneck: ClearanceSample?
        get() = waypointClearances.minByOrNull { it.clearanceMeters ?: Double.MAX_VALUE }

    val worstQuality: WaterLevelQuality
        get() =
            waypointClearances.maxByOrNull { it.waterLevelQuality.qualityRank }
                ?.waterLevelQuality
                ?: WaterLevelQuality.UNAVAILABLE
}

fun interface PassageCandidateEvaluator {
    suspend fun evaluate(departure: ZonedDateTime): PassageCandidateAssessment
}

class PassageWindowScanner(
    val scanIncrement: Duration = Duration.ofMinutes(10),
    val scanBackward: Duration = Duration.ofHours(12),
    val scanForward: Duration = Duration.ofHours(24),
) {
    init {
        require(!scanIncrement.isZero && !scanIncrement.isNegative) {
            "Das Scan-Intervall muss positiv sein."
        }
        require(!scanBackward.isNegative) { "Der Rückwärtsbereich darf nicht negativ sein." }
        require(!scanForward.isNegative) { "Der Vorwärtsbereich darf nicht negativ sein." }
    }

    suspend fun findSafeWindow(
        center: ZonedDateTime,
        evaluator: PassageCandidateEvaluator,
    ): PassageWindow? {
        val berlinCenter = center.withZoneSameInstant(MAP_PLANNING_ZONE_ID)
        val scanStart = berlinCenter.minus(scanBackward)
        val scanEnd = berlinCenter.plus(scanForward)
        val windows = mutableListOf<PassageWindow>()
        var openWindow: OpenWindow? = null
        var candidate = scanStart

        while (!candidate.isAfter(scanEnd)) {
            currentCoroutineContext().ensureActive()
            val assessment = evaluator.evaluate(candidate)
            if (assessment.isSafe) {
                val safeAssessment = assessment.toSafeAssessment()
                openWindow =
                    openWindow?.apply {
                        end = candidate
                        merge(safeAssessment)
                    } ?: OpenWindow(
                        start = candidate,
                        end = candidate,
                        assessment = safeAssessment,
                    )
            } else {
                openWindow?.let { windows += it.toWindow() }
                openWindow = null
            }
            candidate = candidate.plus(scanIncrement)
        }

        openWindow?.let { windows += it.toWindow() }

        return windows.firstOrNull { it.contains(berlinCenter) }
            ?: windows.firstOrNull { it.start.toInstant() > berlinCenter.toInstant() }
    }

    private data class SafeAssessment(
        val quality: WaterLevelQuality,
        val anchoredHighWater: ZonedDateTime?,
        val bottleneckName: String?,
    )

    private data class OpenWindow(
        val start: ZonedDateTime,
        var end: ZonedDateTime,
        var assessment: SafeAssessment,
    ) {
        fun merge(candidate: SafeAssessment) {
            if (candidate.quality.qualityRank > assessment.quality.qualityRank) {
                assessment = candidate
            }
        }

        fun toWindow(): PassageWindow =
            PassageWindow(
                start = start,
                end = end,
                anchoredHighWater = assessment.anchoredHighWater,
                bottleneckName = assessment.bottleneckName,
                waterLevelQuality = assessment.quality,
                waterLevelDetail = assessment.quality.detail,
            )
    }

    private fun PassageCandidateAssessment.toSafeAssessment(): SafeAssessment {
        val bottleneck = bottleneck
        return SafeAssessment(
            quality = worstQuality,
            anchoredHighWater = bottleneck?.anchoredHighWater,
            bottleneckName = bottleneck?.waypointName,
        )
    }
}

private val WaterLevelQuality.qualityRank: Int
    get() =
        when (this) {
            WaterLevelQuality.LOCAL_OFFICIAL -> 0
            WaterLevelQuality.MANUAL -> 1
            WaterLevelQuality.CONFIRMED_COMPARISON -> 2
            WaterLevelQuality.STALE -> 3
            WaterLevelQuality.OUTSIDE_FORECAST_HORIZON -> 4
            WaterLevelQuality.UNAVAILABLE -> 5
        }

private val WaterLevelQuality.detail: String?
    get() =
        when (this) {
            WaterLevelQuality.LOCAL_OFFICIAL -> null
            WaterLevelQuality.MANUAL ->
                "Das Passagefenster verwendet eine manuelle Wasserstandskorrektur."

            WaterLevelQuality.CONFIRMED_COMPARISON ->
                "Das Passagefenster verwendet einen bestätigten Vergleichspegel."

            WaterLevelQuality.STALE ->
                "Die Wasserstandsprognose für das Passagefenster ist veraltet."

            WaterLevelQuality.OUTSIDE_FORECAST_HORIZON ->
                "Das Passagefenster basiert auf astronomischen Gezeitendaten."

            WaterLevelQuality.UNAVAILABLE ->
                "Für das Passagefenster liegt keine aktuelle lokale Wasserstandsprognose vor."
        }
