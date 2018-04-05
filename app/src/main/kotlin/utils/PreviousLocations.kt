package utils

import android.location.Location
import java.util.*

class PreviousLocations {

    private val locations = LinkedList<Location>()

    fun addLocation(location: Location) {
        if (locations.size >= MAX_LOCATIONS_IN_HISTORY) {
            locations.removeFirst()
        }
        locations.addLast(location)
    }

    fun calculateLastMeanDistance(): Double {

        return -1.0
    }

    fun calculateMeanHistoryDistance(): Double {

        return -2.0
    }

    fun getLastLocations(): List<Location> {
        return locations
    }
}