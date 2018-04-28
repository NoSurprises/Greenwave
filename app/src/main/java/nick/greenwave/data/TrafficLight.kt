package nick.greenwave.data

import android.location.Location
import nick.greenwave.data.dto.LightSettings

class TrafficLight(
        val lat: Double,
        val lng: Double,
        var settings: LightSettings =
                LightSettings(identifier = "$lat-$lng".replace('.', ';'))) {
    override fun toString(): String = "($lat, $lng), settings=$settings"
    val location = Location("")

    init {
        location.latitude = lat
        location.longitude = lng
    }

    companion object {
        fun createFromSettings(settings: LightSettings): TrafficLight {
            val latLng = Storage.parseKey(settings.identifier)
            return TrafficLight(latLng.first, latLng.second, settings)
        }
    }


}