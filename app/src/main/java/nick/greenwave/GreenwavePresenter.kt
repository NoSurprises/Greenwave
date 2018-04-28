package nick.greenwave

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import nick.greenwave.data.Storage
import nick.greenwave.data.TrafficLight
import nick.greenwave.data.dto.LightSettings
import utils.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

interface FirebaseLightSettingsCallback {
    fun openSettings(light: LightSettings)
    fun getSettings(light: LightSettings)
    fun onReceiveJustSettings(light: LightSettings)
}

class GreenwavePresenter(val view: GreenwaveView) : GreenwavePresenterApi, FirebaseLightSettingsCallback {

    private var lastLoc: Location = Location("")
    private var lastSpeed: Double = 0.0
    private val model: GreenwaveModelApi = GreenwaveModel(this)
    private val movementHelper = CameraMovementLogicHelper()
    private var needUpdateNearestLight = false
    private var meanSpeedHelper = MeanSpeed()
    private var subscription: Disposable? = null


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

        updateDistanceToClosestLight()
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
            if (DEBUG) Log.d(TAG, "(63, GreenwavePresenter.ktt) time to update nearest lights")
            requestNearestLights(lastLoc)
        }
    }

    override fun forceChooseNewClosestLight() {
        needUpdateNearestLight = true
    }

    private fun updateClosestLightIfNeeded() {
        if (needUpdateNearestLight) {
            needUpdateNearestLight = false
            val closest = model.getNearestLight(lastLoc)
            closest?.let { model.requestSettingsForLight(closest.settings.identifier, REQUEST_FIREBASE_CLOSEST_LIGHT_SETTINGS) }
        }
    }

    private fun updateDistanceToClosestLight() {
        val closestLight = model.getDefinedClosestFrontLight(lastLoc)
        closestLight?.let { setDistanceTo(closestLight) }
    }

    private fun onReceiveOneClosestLight(light: TrafficLight) {
        view.resetMarkersColors()
        view.setActiveColorMarker(LatLng(light.lat, light.lng))

        setDistanceTo(light)

        subscription?.dispose()
        view.setEmptyRecommendedFields()

        if (!light.settings.isSet()) {
            return
        }

        val cycle = light.settings.greenCycle + light.settings.redCycle
        val diff = ((Date().time - light.settings.startOfMeasurement) / 1000) % cycle
        var timeToGreen = abs(diff - cycle).toInt() // todo equation
        if (DEBUG) Log.d(TAG, "(114, GreenwavePresenter.kt) light has settings, time to green $timeToGreen")


        subscription = Observable.timer(SECOND_IN_MILLIS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .repeat(timeToGreen.toLong())
                .subscribe({
                    Log.i(TAG, "time to green ${timeToGreen - 1}")
                    view.setTimeToGreen(--timeToGreen)
                    view.setRecommendedSpeed(calculateRecommendedSpeed(light.location.distanceTo(lastLoc), timeToGreen))
                })



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

    private val userLightsStorage by lazy { Storage(view.getApplicationContext()) }

    override fun addMapMark(latLng: LatLng) {
        userLightsStorage.saveToPreferences(TrafficLight(latLng.latitude, latLng.longitude))
        view.addMark(latLng, true)
    }

    override fun chooseNewLight(position: LatLng) {
        model.setNewClosestLight(TrafficLight(position.latitude, position.longitude))
        model.requestSettingsForLight(model.createIdentifierFromLatlng(position), REQUEST_FIREBASE_CLOSEST_LIGHT_SETTINGS)
    }

    override fun openLightSettings(marker: Marker) {
        if (DEBUG) Log.d(TAG, "(80, GreenwavePresenterr.kt) openLightSettings for ${marker.snippet}")

        val identifier = model.createIdentifierFromLatlng(marker.position)
        model.requestSettingsForLight(identifier, REQUEST_FIREBASE_SETTINGS_TO_OPEN_SETTINGS)
    }

    override fun openLightSettings(position: LatLng) {
        val identifier = model.createIdentifierFromLatlng(position)
        model.requestSettingsForLight(identifier, REQUEST_FIREBASE_SETTINGS_TO_OPEN_SETTINGS)
    }

    override fun getSettings(light: LightSettings) {
        if (DEBUG) Log.d(TAG, "(176, GreenwavePresenter.kt) getSettings: $light")
        onReceiveOneClosestLight(TrafficLight.createFromSettings(light))
    }

    override fun openSettings(light: LightSettings) {
        onReceiveLightSettingsOpenSettings(light)
    }

    override fun onReceiveLightSettingsOpenSettings(light: LightSettings) {
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

    override fun requestSettingsFor(latLng: LatLng) {
        model.requestSettingsForLight(model.createIdentifierFromLatlng(latLng), REQUEST_FIREBASE_JUST_SETTINGS)
    }

    override fun onReceiveJustSettings(light: LightSettings) {
        view.onReceiveSettings(light)
    }


}