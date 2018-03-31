package nick.greenwave

import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

class GreenwaveProvider(val view: GreenwaveView) : GreenwaveProviderApi {

    override fun onMapReady(map: GoogleMap?) {
        view.requestLocationPermissions()
    }

    override fun onPermissionsGranted() {
        view.enableMyLocationButton()
        view.defaultCameraSettings()
        view.mapToDeviceLocation()
        view.registerLocationUpdate()
    }

    override fun onPause() {
        view.unregisterLocationUpdate()
    }

    override fun onResume() {
        view.registerLocationUpdate()
    }

    override fun onLocationUpdate(location: LocationResult?) {
        location ?: return
        val lastLoc = location.lastLocation

        val position = CameraPosition.builder(view.cameraPosition ?: view.defaultCameraSettings())
                .target(LatLng(lastLoc.latitude, lastLoc.longitude))
                .bearing(lastLoc.bearing)
                .build()

        view.moveCameraTo(position)

        // todo remove debug
        view.setLat(lastLoc.latitude)
        view.setLon(lastLoc.longitude)
    }
}