package nick.greenwave

import android.content.Context
import android.location.Location

import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import nick.greenwave.data.TrafficLight
import nick.greenwave.data.dto.LightSettings

interface GreenwavePresenterApi {

    fun onMapReady(map: GoogleMap?)

    fun onPermissionsGranted()

    fun onPause()

    fun onResume()

    fun onLocationUpdate(location: LocationResult)

    fun onSpeedChanged(newSpeed: Double)

    fun getApplicationContext(): Context

    fun addMapMark(latLng: LatLng)

    fun openLightSettings(marker: Marker)
    fun onReceiveNearestLights(lights: List<TrafficLight>)
    fun requestNearestLights(location: Location)
    fun onCameraMoved()
    fun updateLightSettings(light: LightSettings?)
    fun onReceiveLightSettingsOpenSettings(light: LightSettings)
    fun forceChooseNewClosestLight()

}