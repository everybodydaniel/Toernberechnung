package com.example.trnberechnung.mapplanning

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.junit.Test

class RouteStopRulesTest {
    private val firstId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val secondId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    @Test
    fun `stops can be prepared before endpoints and keep selection order`() {
        val ids = ArrayDeque(listOf(firstId, secondId))

        val stops =
            RouteStopRules.add(
                existing = emptyList(),
                candidates =
                    listOf(
                        HarbourId.JUIST_HARBOR,
                        HarbourId.JUIST_HARBOR,
                        HarbourId.NORDERNEY_HARBOR,
                    ),
                start = null,
                destination = null,
                idFactory = ids::removeFirst,
            )

        stops.map(IntermediateStop::id) shouldContainExactly listOf(firstId, secondId)
        stops.map(IntermediateStop::harbourId) shouldContainExactly
            listOf(HarbourId.JUIST_HARBOR, HarbourId.NORDERNEY_HARBOR)
    }

    @Test
    fun `endpoint collisions are removed without reordering surviving stops`() {
        val stops =
            listOf(
                IntermediateStop(firstId, HarbourId.JUIST_HARBOR),
                IntermediateStop(secondId, HarbourId.NORDERNEY_HARBOR),
            )

        val sanitized =
            RouteStopRules.removeEndpointCollisions(
                existing = stops,
                start = HarbourId.NORDERNEY_HARBOR,
                destination = HarbourId.EMDEN_HARBOR,
            )

        sanitized shouldContainExactly listOf(stops.first())
    }

    @Test
    fun `update keeps stable id and rejects duplicate or endpoint`() {
        val stops =
            listOf(
                IntermediateStop(firstId, HarbourId.JUIST_HARBOR),
                IntermediateStop(secondId, HarbourId.NORDERNEY_HARBOR),
            )

        RouteStopRules.update(
            existing = stops,
            stopId = firstId,
            harbourId = HarbourId.BALTRUM_HARBOR,
            start = HarbourId.EMDEN_HARBOR,
            destination = HarbourId.WANGEROOGE_HARBOR,
        ) shouldContainExactly
            listOf(
                IntermediateStop(firstId, HarbourId.BALTRUM_HARBOR),
                stops[1],
            )

        RouteStopRules.update(
            existing = stops,
            stopId = firstId,
            harbourId = HarbourId.NORDERNEY_HARBOR,
            start = HarbourId.EMDEN_HARBOR,
            destination = HarbourId.WANGEROOGE_HARBOR,
        ) shouldBe null

        RouteStopRules.update(
            existing = stops,
            stopId = firstId,
            harbourId = HarbourId.EMDEN_HARBOR,
            start = HarbourId.EMDEN_HARBOR,
            destination = HarbourId.WANGEROOGE_HARBOR,
        ) shouldBe null
    }
}
