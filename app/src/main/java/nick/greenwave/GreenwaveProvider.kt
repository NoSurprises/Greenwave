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
import utils.CameraMovementLogicHelper

class GreenwaveProvider(val view: GreenwaveView) : GreenwaveProviderApi {


    val model: GreenwaveModelApi = GreenwaveModel(this)
    var disposable: Disposable? = null
    private val movementHelper = CameraMovementLogicHelper()
    private var needUpdateNearestLight = false

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
    }

    override fun onResume() {
        view.registerLocationUpdate()
    }

    override fun onLocationUpdate(location: LocationResult) {
        val lastLoc = location.lastLocation

        onSpeedChanged(lastLoc.speed.toDouble()) // todo testing defalut api

        if (movementHelper.canMoveCamera()) {
            val position = CameraPosition.builder(view.cameraPosition
                    ?: view.defaultCameraSettings())
                    .target(LatLng(lastLoc.latitude, lastLoc.longitude))
                    .bearing(lastLoc.bearing)
                    .build()
            view.moveCameraTo(position)
        }

        // todo remove debug
        view.setLat(lastLoc.latitude)
        view.setLon(lastLoc.longitude)

        if (model.detectNotableDistanceFromLastQueryLight(lastLoc)) {
            if (DEBUG) Log.d(TAG, "(63, GreenwaveProvider.kt) time to choose nearest light")
            needUpdateNearestLight = true
        }

        if (needUpdateNearestLight) {
            needUpdateNearestLight = false
            val closest = model.getNearestLight(lastLoc)
            view.resetMarkersColors()
            closest?.let { view.setActiveColorMarker(LatLng(closest.lat, closest.lng)) }
        }
    }

    override fun onCameraMoved() {
        movementHelper.startMovement()
    }

    override fun getApplicationContext(): Context {
        val applicationContext = view.getApplicationContext()
        if (DEBUG) Log.d(TAG, "(56, GreenwaveProvider.kt) getApplicationContext: $applicationContext")
        return applicationContext
    }

    override fun onSpeedChanged(newSpeed: Double) {
        view.setCurrentSpeed(newSpeed * 3.6) // convert from m/s to km/h
    }

    override fun addMapMark(latLng: LatLng) {
        // todo save in model
        view.addMark(latLng)
    }

    override fun openLightSettings(marker: Marker) {
        if (DEBUG) Log.d(TAG, "(80, GreenwaveProvider.kt) openLightSettings for ${marker.snippet}")
        // todo get data from model, maybe bound TrafficLight object in adapter of the card
        view.startSettingsActivy(LightSettings(0, 0, 22))
    }

    override fun onReceiveNearestLights(lights: List<TrafficLight>) {
        view.removeAllMarks()
        for (i in lights) {
            view.addMark(LatLng(i.lat, i.lng))
        }
    }

    override fun requestNearestLights(location: Location) {
        if (DEBUG) Log.d(TAG, "(99, GreenwaveProvider.kt) requestNearestLights for $location")
        model.requestNearestLights(location.latitude.toFloat(), location.longitude.toFloat())
    }



}