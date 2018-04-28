package nick.greenwave

import android.content.Context
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import nick.greenwave.data.dto.LightSettings

interface GreenwaveView {
    var cameraPosition: CameraPosition?

    fun requestLocationPermissions()

    fun enableMyLocationButton()

    fun defaultCameraSettings(): CameraPosition?

    fun mapToDeviceLocation()

    fun registerLocationUpdate()

    fun unregisterLocationUpdate()

    fun moveCameraTo(position: CameraPosition)

    fun setLon(lon: Double)
    fun setLat(lat: Double)

    fun getApplicationContext(): Context
    fun addMark(latLng: LatLng, openSettings: Boolean)

    fun startSettingsActivy(lightSettingsInfo: LightSettings)
    fun setCurrentSpeed(speed: Double)
    fun setActiveColorMarker(latLng: LatLng)
    fun resetMarkersColors()
    fun removeAllMarks()
    fun setDistance(distance: Double)
    fun setRecommendedSpeed(speed: Double)
    fun setTimeToGreen(time: Int)
    fun setEmptyRecommendedFields()

}