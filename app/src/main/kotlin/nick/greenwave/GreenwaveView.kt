package nick.greenwave

import com.google.android.gms.maps.model.CameraPosition

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

}