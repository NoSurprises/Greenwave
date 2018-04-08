package nick.greenwave.data

import android.location.Location
import nick.greenwave.data.dto.LightSettings

class TrafficLight(val lat: Double, val lng: Double) {
    val settings = LightSettings()
    override fun toString(): String = "($lat, $lng), settings=$settings"
    val location = Location("")

    init {
        location.latitude = lat
        location.longitude = lng
    }


}