package nick.greenwave

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import nick.greenwave.data.TrafficLight
import nick.greenwave.data.dto.LightSettings


interface GreenwaveModelApi {
    fun requestNearestLights(lat: Float, lng: Float)
    fun getNearestLight(currentLocation: Location): TrafficLight?
    fun detectNotableDistanceFromLastQueryLight(currentLocation: Location): Boolean
    fun getValidClosestLight(location: Location): TrafficLight?
    fun updateLightSettingsInRemoteDb(light: LightSettings)
    fun createIdentifierFromLatlng(latLng: LatLng) : String
    fun requestSettingsForLight(identifier: String)

}