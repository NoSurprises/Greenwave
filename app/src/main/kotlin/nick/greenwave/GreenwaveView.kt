package nick.greenwave

import android.content.Context
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import nick.greenwave.dto.Light

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

    fun setCurrentSpeed(speed: Double, history: Boolean = false)

    fun addMark(latLng: LatLng)

    fun startSettingsActivy(lightInfo: Light)

}