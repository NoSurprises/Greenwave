package nick.greenwave

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import io.reactivex.disposables.Disposable
import nick.greenwave.data.TrafficLight
import nick.greenwave.data.dto.LightSettings
import utils.ALL_MEAN_SPEED_MEASURMENT
import utils.LAST_2_SPEED_MEASURMENT

class GreenwaveProvider(val view: GreenwaveView) : GreenwaveProviderApi {


    val model: GreenwaveModelApi = GreenwaveModel(this)
    var disposable: Disposable? = null

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

        onSpeedChanged(lastLoc.speed.toDouble(), LAST_2_SPEED_MEASURMENT) // todo testing defalut api

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
        if (variant == LAST_2_SPEED_MEASURMENT)
            view.setCurrentSpeed(newSpeed)
        else if (variant == ALL_MEAN_SPEED_MEASURMENT)
            view.setCurrentSpeed(newSpeed, true)
    }

    override fun addMapMark(latLng: LatLng) {
        if (DEBUG) Log.d(TAG, "(73, GreenwaveProvider.kt) addMapMark: latlng=$latLng")
        // todo save in model
        view.addMark(latLng)
    }

    override fun openLightSettings(marker: Marker) {
        if (DEBUG) Log.d(TAG, "(80, GreenwaveProvider.kt) openLightSettings for ${marker.snippet}")
        // todo get data from model, maybe bound TrafficLight object in adapter of the card
        view.startSettingsActivy(LightSettings(0, 0, 22))
    }

    override fun onReceiveNearestLights(lights: List<TrafficLight>) {
        for (i in lights) {
            if (DEBUG) Log.d(TAG, "(93, GreenwaveProvider.kt) nearest: $i/.jk")
            view.addMark(LatLng(i.lat, i.lng))
        }
    }

    override fun requestNearestLights(location: Location) {
        if (DEBUG) Log.d(TAG, "(99, GreenwaveProvider.kt) requestNearestLights for $location")
        model.requestNearestLights(location.latitude.toFloat(), location.longitude.toFloat())


    }

}