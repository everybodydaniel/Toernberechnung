package com.example.trnberechnung.nauti

import io.kotest.matchers.shouldBe
import org.junit.Test

class NautiActionValidatorTest {
    @Test
    fun `rejects unknown duplicate and endpoint-colliding harbours`() {
        NautiActionValidator.validate(
            NautiAction.PlanTrip("emden_harbor", "unknown"),
        ).isValid shouldBe false

        NautiActionValidator.validate(
            NautiAction.PlanTrip(
                "emden_harbor",
                "juist_harbor",
                listOf("emden_harbor"),
            ),
        ).isValid shouldBe false

        NautiActionValidator.validate(
            NautiAction.PlanTrip(
                "emden_harbor",
                "juist_harbor",
                listOf("norderney_harbor"),
            ),
        ).isValid shouldBe true
    }

    @Test
    fun `validates optional harbour parameters`() {
        NautiActionValidator.validate(NautiAction.ShowWeather(null)).isValid shouldBe true
        NautiActionValidator.validate(
            NautiAction.ShowTides("wangerooge_harbor"),
        ).isValid shouldBe true
        NautiActionValidator.validate(
            NautiAction.ShowBshWaterLevel("not-a-harbour"),
        ).isValid shouldBe false
    }
}
