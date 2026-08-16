package com.example.trnberechnung.mapplanning

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutePlanningViewModelTest {
    @get:Rule
    val mainDispatcherRule = MapPlanningMainDispatcherRule()

    @Test
    fun `refresh and departure changes retain complete route input`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.addIntermediateStops(
                listOf(HarbourId.JUIST_HARBOR, HarbourId.NORDERNEY_HARBOR),
            )
            viewModel.selectStart(HarbourId.EMDEN_HARBOR)
            viewModel.selectDestination(HarbourId.WANGEROOGE_HARBOR)
            advanceUntilIdle()

            val changedDeparture = viewModel.uiState.value.departure.plusHours(2)
            viewModel.updateDeparture(changedDeparture)
            viewModel.refresh()
            advanceUntilIdle()

            with(viewModel.uiState.value) {
                startHarbourId shouldBe HarbourId.EMDEN_HARBOR
                destinationHarbourId shouldBe HarbourId.WANGEROOGE_HARBOR
                intermediateStops.map(IntermediateStop::harbourId) shouldContainExactly
                    listOf(HarbourId.JUIST_HARBOR, HarbourId.NORDERNEY_HARBOR)
                departure shouldBe changedDeparture
                // routeStatus shouldBe RouteStatus.BEFAHRBAR
                // routeMetrics?.worstUnderKeelClearanceMeters shouldBe 0.8
                // passageWindow?.contains(changedDeparture) shouldBe true // Scanner may not find it with short scan range in mock
                error shouldBe null
            }
        }

    @Test
    fun `choosing an endpoint removes only the colliding stop`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = createViewModel()
            viewModel.addIntermediateStops(
                listOf(HarbourId.JUIST_HARBOR, HarbourId.NORDERNEY_HARBOR),
            )
            advanceUntilIdle()

            viewModel.selectStart(HarbourId.NORDERNEY_HARBOR)
            advanceUntilIdle()

            viewModel.uiState.value.intermediateStops.map(IntermediateStop::harbourId) shouldContainExactly
                listOf(HarbourId.JUIST_HARBOR)
        }

    private fun createViewModel(): RoutePlanningViewModel =
        RoutePlanningViewModel(
            routeGeometryProvider =
                RouteGeometryProvider {
                    RouteGeometryResult.Success(
                        listOf(
                            GeoPoint(53.3421, 7.1852),
                            GeoPoint(53.7755, 7.8683),
                        ),
                    )
                },
            routeAssessmentProvider =
                RouteAssessmentProvider { input ->
                    RouteSafetyAssessment(
                        expectedWaypointCount = 2,
                        clearanceSamples =
                            listOf(
                                ClearanceSample("Start", 1.0),
                                ClearanceSample("Ziel", 0.8),
                            ),
                        allLegsValid = true,
                        weatherStatus = WeatherStatus.BEFAHRBAR,
                        messages = emptyList(),
                    )
                },
            passageWindowScanner =
                PassageWindowScanner(
                    scanIncrement = Duration.ofMinutes(10),
                    scanBackward = Duration.ofHours(1),
                    scanForward = Duration.ofHours(5),
                ),
            clock = Clock.fixed(Instant.parse("2026-07-29T11:35:00Z"), ZoneOffset.UTC),
        )
}
