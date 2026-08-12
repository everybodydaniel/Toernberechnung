package com.example.trnberechnung.routing.v2

enum class CellType(val cost: Double, val isBlocked: Boolean) {

    OPEN_SEA(3.0, false),

    FAIRWAY(0.8, false),

    HARBOUR(0.5, false),

    WATTFAHRWASSER(0.9, false),

    LAND(Double.MAX_VALUE, true),

    RESTRICTED(Double.MAX_VALUE, true),

    RUHEZONE(10.0, false);

    companion object {

        private val VALUES = values()

        fun fromByte(b: Byte): CellType {
            val idx = b.toInt() and 0xFF
            return if (idx < VALUES.size) VALUES[idx] else OPEN_SEA
        }
    }
}
