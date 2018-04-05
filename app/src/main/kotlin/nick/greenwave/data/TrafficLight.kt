package nick.greenwave.data

import nick.greenwave.data.dto.LightSettings

class TrafficLight(val lon: Double, val lat: Double) {
    val settings = LightSettings()
    override fun toString(): String = "($lon, $lat), settings=$settings"

}