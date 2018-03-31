package nick.greenwave

import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import utils.ALL_MEAN_SPEED_MEASURMENT
import utils.LAST_2_SPEED_MEASURMENT

class GreenwaveProvider(val view: GreenwaveView) : GreenwaveProviderApi {

    val model: GreenwaveModelApi = GreenwaveModel(this)

    private val TAG = "GreenwaveProvider"
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
        model.stopTrackingSpeed()

    }

    override fun onResume() {
        view.registerLocationUpdate()
        model.startTrackingSpeed()

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

    override fun getApplicationContext(): Context {

        val applicationContext = view.getApplicationContext()
        if (DEBUG) Log.d(TAG, "(56, GreenwaveProvider.kt) getApplicationContext: $applicationContext")
        return applicationContext
    }

    override fun onSpeedChanged(newSpeed: Double, variant: Int) {
        if (DEBUG) Log.d(TAG, "(64, GreenwaveProvider.kt) onSpeedChanged, new speed $newSpeed")
        if (variant == LAST_2_SPEED_MEASURMENT)
            view.setCurrentSpeed(newSpeed)
        else if (variant == ALL_MEAN_SPEED_MEASURMENT)
            view.setCurrentSpeed(newSpeed, true)
    }
}