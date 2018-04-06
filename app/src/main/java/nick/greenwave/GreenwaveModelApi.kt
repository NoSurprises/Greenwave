package nick.greenwave

import android.location.Location
import nick.greenwave.data.TrafficLight


interface GreenwaveModelApi {
    fun requestNearestLights(lat: Float, lng: Float)
    fun getNearestLight(location: Location): TrafficLight?

}