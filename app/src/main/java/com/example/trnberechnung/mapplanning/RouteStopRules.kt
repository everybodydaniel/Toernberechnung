package com.example.trnberechnung.mapplanning

import java.util.UUID

object RouteStopRules {
    fun add(
        existing: List<IntermediateStop>,
        candidates: Iterable<HarbourId>,
        start: HarbourId?,
        destination: HarbourId?,
        idFactory: () -> UUID = UUID::randomUUID,
    ): List<IntermediateStop> {
        val unavailable =
            buildSet {
                addAll(existing.map(IntermediateStop::harbourId))
                start?.let(::add)
                destination?.let(::add)
            }.toMutableSet()
        val additions =
            candidates.mapNotNull { harbourId ->
                if (unavailable.add(harbourId)) {
                    IntermediateStop(idFactory(), harbourId)
                } else {
                    null
                }
            }
        return existing + additions
    }

    fun removeEndpointCollisions(
        existing: List<IntermediateStop>,
        start: HarbourId?,
        destination: HarbourId?,
    ): List<IntermediateStop> {
        val endpoints = setOfNotNull(start, destination)
        return existing.filterNot { it.harbourId in endpoints }
    }

    fun update(
        existing: List<IntermediateStop>,
        stopId: UUID,
        harbourId: HarbourId,
        start: HarbourId?,
        destination: HarbourId?,
    ): List<IntermediateStop>? {
        if (harbourId == start || harbourId == destination) return null
        if (existing.any { it.id != stopId && it.harbourId == harbourId }) return null
        if (existing.none { it.id == stopId }) return null
        return existing.map { stop ->
            if (stop.id == stopId) stop.copy(harbourId = harbourId) else stop
        }
    }
}
