package com.example.trnberechnung.mapplanning

object HarbourCatalog {
    val all: List<Harbour> =
        listOf(
            Harbour(
                id = HarbourId.BORKUM_HARBOR,
                name = "Borkum, Fischerbalje",
                subtitle = "Borkum, Fischerbalje",
                coordinate = GeoPoint(53.5606, 6.7502),
                chartDepthMeters = 3.0,
                tideStationId = "101P",
            ),
            Harbour(
                id = HarbourId.EMDEN_HARBOR,
                name = "Emden, Hafen",
                subtitle = "Emden, Große Seeschleuse",
                coordinate = GeoPoint(53.3421, 7.1852),
                chartDepthMeters = 5.0,
                tideStationId = "507P",
            ),
            Harbour(
                id = HarbourId.JUIST_HARBOR,
                name = "Juist, Hafen",
                subtitle = "Juist, Hafen",
                coordinate = GeoPoint(53.6722, 6.9982),
                chartDepthMeters = -1.2,
                tideStationId = "794P",
            ),
            Harbour(
                id = HarbourId.NORDERNEY_HARBOR,
                name = "Norderney, Hafen",
                subtitle = "Norderney, Riffgat",
                coordinate = GeoPoint(53.7024, 7.1637),
                chartDepthMeters = 1.5,
                tideStationId = "111P",
            ),
            Harbour(
                id = HarbourId.BALTRUM_HARBOR,
                name = "Baltrum, Hafen",
                subtitle = "Baltrum, Westende",
                coordinate = GeoPoint(53.7229, 7.3669),
                chartDepthMeters = -1.0,
                tideStationId = "784P",
            ),
            Harbour(
                id = HarbourId.LANGEOOG_HARBOR,
                name = "Langeoog, Hafen",
                subtitle = "Langeoog, Hafeneinfahrt",
                coordinate = GeoPoint(53.7263, 7.4968),
                chartDepthMeters = -0.5,
                tideStationId = "781P",
            ),
            Harbour(
                id = HarbourId.SPIEKEROOG_HARBOR,
                name = "Spiekeroog, Hafen",
                subtitle = "Spiekeroog",
                coordinate = GeoPoint(53.7632, 7.6955),
                chartDepthMeters = -0.8,
                tideStationId = "779P",
            ),
            Harbour(
                id = HarbourId.WANGEROOGE_HARBOR,
                name = "Wangerooge, Hafen",
                subtitle = "Wangerooge, Hafen",
                coordinate = GeoPoint(53.7755, 7.8683),
                chartDepthMeters = -0.6,
                tideStationId = "777P",
            ),
        )

    private val byId = all.associateBy(Harbour::id)

    operator fun get(id: HarbourId): Harbour =
        checkNotNull(byId[id]) { "Unbekannter Hafen: ${id.rawValue}" }
}
