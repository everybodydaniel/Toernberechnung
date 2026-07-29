package com.example.trnberechnung.mapplanning

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class HarbourCatalogTest {
    @Test
    fun `catalog contains the eight canonical iOS harbours in display order`() {
        HarbourCatalog.all.map(Harbour::id) shouldContainExactly
            listOf(
                HarbourId.BORKUM_HARBOR,
                HarbourId.EMDEN_HARBOR,
                HarbourId.JUIST_HARBOR,
                HarbourId.NORDERNEY_HARBOR,
                HarbourId.BALTRUM_HARBOR,
                HarbourId.LANGEOOG_HARBOR,
                HarbourId.SPIEKEROOG_HARBOR,
                HarbourId.WANGEROOGE_HARBOR,
            )
        HarbourCatalog[HarbourId.EMDEN_HARBOR].subtitle shouldBe
            "Emden, Große Seeschleuse"
        HarbourId.fromRawValue("wangerooge_harbor") shouldBe HarbourId.WANGEROOGE_HARBOR
        HarbourId.fromRawValue("unbekannt") shouldBe null
    }
}
