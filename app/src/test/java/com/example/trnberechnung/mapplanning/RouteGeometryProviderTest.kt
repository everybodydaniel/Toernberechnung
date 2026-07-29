package com.example.trnberechnung.mapplanning

import com.example.trnberechnung.routing.v2.NauticalRouterV2
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RouteGeometryProviderTest {
    @Test
    fun `missing A star path is exposed as incomplete without straight fallback`() =
        runTest {
            val provider =
                NauticalRouterV2GeometryProvider { _, _ ->
                    NauticalRouterV2.RouteResult.Incomplete(
                        reason = NauticalRouterV2.FailureReason.NO_SEA_PATH,
                        message = "Kein Seeweg",
                    )
                }

            val result =
                provider.calculate(
                    listOf(
                        HarbourCatalog[HarbourId.EMDEN_HARBOR],
                        HarbourCatalog[HarbourId.JUIST_HARBOR],
                    ),
                )

            result shouldBe
                RouteGeometryResult.Incomplete(
                    reason = "Emden, Hafen → Juist, Hafen: Kein Seeweg",
                    partialPoints = emptyList(),
                )
        }
}
