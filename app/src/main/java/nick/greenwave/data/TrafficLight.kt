package nick.greenwave.data

import android.location.Location
import nick.greenwave.data.dto.LightSettings

class TrafficLight(val lat: Double, val lng: Double) {
    var settings = LightSettings()
    override fun toString(): String = "($lat, $lng), settings=$settings"
    val location = Location("")

    init {
        location.latitude = lat
        location.longitude = lng
        val tmp = "$lat-$lng"
        settings.identifier=tmp.replace('.', ';')
    }


}