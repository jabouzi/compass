package com.skanderjabouzi.newcompass

// Using an enum class for cardinal directions for better type safety
enum class CardinalDirection(val abbr: String) {
    NORTH("N"),
    NORTH_EAST("NE"),
    EAST("E"),
    SOUTH_EAST("SE"),
    SOUTH("S"),
    SOUTH_WEST("SW"),
    WEST("W"),
    NORTH_WEST("NW");

    companion object {
        const val TRUE_NORTH = "T";
        const val MAGNETIC_NORTH = "M";

        /**
         * Converts a bearing (angle in degrees) into a cardinal direction string.
         * Handles negative bearings by converting them to a positive equivalent.
         */
        fun fromBearing(bearing: Float): CardinalDirection {
            var positiveBearing = bearing
            if (positiveBearing < 0) {
                positiveBearing += 360f
            }
            return fromPositiveBearing(positiveBearing)
        }

        /**
         * Converts a positive bearing (0-360 degrees) into a cardinal direction.
         */
        fun fromPositiveBearing(bearing: Float): CardinalDirection {
            return when {
                bearing >= 0f && bearing < 22.5f -> NORTH
                bearing >= 22.5f && bearing < 67.5f -> NORTH_EAST
                bearing >= 67.5f && bearing < 112.5f -> EAST
                bearing >= 112.5f && bearing < 157.5f -> SOUTH_EAST
                bearing >= 157.5f && bearing < 202.5f -> SOUTH
                bearing >= 202.5f && bearing < 247.5f -> SOUTH_WEST
                bearing >= 247.5f && bearing < 292.5f -> WEST
                bearing >= 292.5f && bearing < 337.5f -> NORTH_WEST
                bearing >= 337.5f && bearing < 360f -> NORTH
                else -> NORTH // Fallback, though ideally shouldn't happen with proper input
            }
        }

        /**
         * Converts a boolean indicating true north usage to its corresponding string representation.
         */
        fun convertUseTrueNorth(useTrueNorth: Boolean): String {
            return if (useTrueNorth) TRUE_NORTH else MAGNETIC_NORTH
        }
    }
}