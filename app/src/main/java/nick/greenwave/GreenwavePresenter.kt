package nick.greenwave

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import nick.greenwave.data.Storage
import nick.greenwave.data.TrafficLight
import nick.greenwave.data.dto.LightSettings
import utils.CameraMovementLogicHelper
import utils.MeanSpeed
import utils.SECOND_IN_MILLIS
import utils.TIMER_NAME_GREEN
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.abs

class GreenwavePresenter(val view: GreenwaveView) : GreenwavePresenterApi {

    private var lastLoc: Location = Location("")
    private var lastSpeed: Double = 0.0
    private val model: GreenwaveModelApi = GreenwaveModel(this)
    private val movementHelper = CameraMovementLogicHelper()
    private var needUpdateNearestLight = false
    private var meanSpeedHelper = MeanSpeed()

    private val TAG = "GreenwavePresenter"
    override fun onMapReady(map: GoogleMap?) {
        view.requestLocationPermissions()
    }

    override fun updateLightSettings(light: LightSettings?) {
        light?.let { model.updateLightSettingsInRemoteDb(it) }
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
        lastLoc = location.lastLocation

        onSpeedChanged(lastLoc.speed.toDouble())

        if (movementHelper.canMoveCamera()) {
            moveCameraToLastLocation()
        }

        // todo remove debug
        view.setLat(lastLoc.latitude)
        view.setLon(lastLoc.longitude)

        updateDistanceToClosestLight()
        // todo updateSpeedToClosestLight
        detectTimeToUpdateClosestLight()
        updateClosestLightIfNeeded()
    }

    private fun moveCameraToLastLocation() {
        val position = CameraPosition.builder(view.cameraPosition
                ?: view.defaultCameraSettings())
                .target(LatLng(lastLoc.latitude, lastLoc.longitude))
                .bearing(lastLoc.bearing)
                .build()
        view.moveCameraTo(position)
    }

    private fun detectTimeToUpdateClosestLight() {
        if (model.detectNotableDistanceFromLastQueryLight(lastLoc)) {
            if (DEBUG) Log.d(TAG, "(63, GreenwavePresenter.ktt) time to choose nearest light")
            forceChooseNewClosestLight()
        }
    }

    override fun forceChooseNewClosestLight()  {
        needUpdateNearestLight = true
    }

    private fun updateClosestLightIfNeeded() {
        if (needUpdateNearestLight) {
            needUpdateNearestLight = false
            val closest = model.getNearestLight(lastLoc)
            closest?.let { onReceiveOneClosestLight(closest) }
        }
    }

    private fun updateDistanceToClosestLight() {
        val closestLight = model.getValidClosestLight(lastLoc)
        closestLight?.let { setDistanceTo(closestLight) }
    }

    private fun onReceiveOneClosestLight(light: TrafficLight) {
        view.resetMarkersColors()
        view.setActiveColorMarker(LatLng(light.lat, light.lng))

        setDistanceTo(light)

        if (light.settings.isSet()) {
            val cycle = light.settings.greenCycle + light.settings.redCycle
            val diff = (Date().time - light.settings.startOfMeasurement) % cycle
            var timeToGreen = abs(diff - cycle).toInt()

            timer(TIMER_NAME_GREEN, true, 0L, SECOND_IN_MILLIS, {
                Log.i(TAG, "time to green ${timeToGreen - 1}"); view.setTimeToGreen(--timeToGreen); if (timeToGreen == -10) this.cancel()
            })

            view.setRecommendedSpeed(calculateRecommendedSpeed(light.location.distanceTo(lastLoc), timeToGreen))
        }
    }

    private fun setDistanceTo(light: TrafficLight) {
        view.setDistance(light.location.distanceTo(lastLoc).toDouble())
    }

    private fun calculateRecommendedSpeed(distance: Float, timeToGreen: Int): Double {
        return distance / timeToGreen.toDouble()
    }

    override fun onCameraMoved() {
        movementHelper.startMovement()
    }

    override fun getApplicationContext(): Context {
        val applicationContext = view.getApplicationContext()
        if (DEBUG) Log.d(TAG, "(56, GreenwavePresenterr.kt) getApplicationContext: $applicationContext")
        return applicationContext
    }

    override fun onSpeedChanged(newSpeed: Double) {
        meanSpeedHelper.addSpeed(newSpeed)
        lastSpeed = meanSpeedHelper.getMeanSpeed() * 3.6
        view.setCurrentSpeed(lastSpeed) // convert from m/s to km/h
    }
    private val userLightsStorage by lazy {  Storage(view.getApplicationContext()) }

    override fun addMapMark(latLng: LatLng) {
        // todo save in model
        userLightsStorage.saveToPreferences(TrafficLight(latLng.latitude, latLng.longitude))
        view.addMark(latLng, true)
    }

    override fun openLightSettings(marker: Marker) {
        if (DEBUG) Log.d(TAG, "(80, GreenwavePresenterr.kt) openLightSettings for ${marker.snippet}")
        // todo get data from model, maybe bound TrafficLight object in adapter of the card

        val identifier = model.createIdentifierFromLatlng(marker.position)
        model.requestSettingsForLight(identifier)
    }

    override fun onReceiveLightSettings(light: LightSettings) {
        view.startSettingsActivy(light)
    }


    override fun onReceiveNearestLights(lights: List<TrafficLight>) {
        view.removeAllMarks()
        if (DEBUG) Log.d(TAG, "(172, GreenwavePresenter.kt) onReceiveNearestLights: ")
        for (i in lights) {
            view.addMark(LatLng(i.lat, i.lng), false)
        }
    }

    override fun requestNearestLights(location: Location) {
        if (DEBUG) Log.d(TAG, "(99, GreenwavePresenterr.kt) requestNearestLights for $location")
        model.requestNearestLights(location.latitude.toFloat(), location.longitude.toFloat())
        forceChooseNewClosestLight()
    }



}