package nick.greenwave

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import nick.greenwave.data.TrafficLight
import nick.greenwave.data.dto.LightSettings
import utils.*
import java.util.*


class GreenwaveModel(val provider: GreenwaveProviderApi) : GreenwaveModelApi {

    var context: Context? = null
    private val TAG = "GreenwaveModel"
    private val timer = Timer("SpeedMeasurement")
    private var nearestLights: List<TrafficLight>? = null
    val osmService
            by lazy { OsmService.create() }
    private var lastQueryLightLocation: Location? = null
    private var closestLight: TrafficLight? = null

    override fun createIdentifierFromLatlng(latLng: LatLng) : String {
        return latLng.toString()
    }

    override fun requestSettingsForLight(identifier: String) {
        val lightsRef = FirebaseDatabaseSingletone.getFirebaseInstance().getReference(LIGHTS_REFERENCE_FIREBASE)
        // todo check if key exists

        provider.onReceiveLightSettings(LightSettings()) // todo it's a callback
    }

    override fun requestNearestLights(lat: Float, lng: Float) {
        requestNearestLightsFromApi(lat, lng)
    }

    override fun updateLightSettingsInRemoteDb(light: LightSettings) {
        val lightsRef = FirebaseDatabaseSingletone.getFirebaseInstance().getReference(LIGHTS_REFERENCE_FIREBASE)
        lightsRef.child(light.identifier).setValue(light)
    }

    private fun requestNearestLightsFromSharedPreferences() {
        // todo get nearest lights from Firebase
    }

    private fun requestNearestLightsFromApi(lat: Float, lng: Float) {
        osmService.fetchChunkData(createQuery(lat, lng))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->
                            Log.d(TAG, "result = ${result.elements}")
                            onReceiveNearestLights(result)
                        },
                        { error ->
                            Log.d(TAG, "error: $error")
                        }
                )
    }

    private fun createQuery(lat: Float, lng: Float): String {
        val bounds = "(${lat - NEAREST_LIGHTS_MARGIN},${lng - NEAREST_LIGHTS_MARGIN}," +
                "${lat + NEAREST_LIGHTS_MARGIN},${lng + NEAREST_LIGHTS_MARGIN})"

        val result = StringBuilder()
        OVERPASS_QUERY.split(OVERPASS_QUERY_DELIMETER).joinTo(result, bounds)
        if (DEBUG) Log.d(TAG, "(88, GreenwaveModel.kt) createdQuery: $result")
        return result.toString()
    }

    private fun onReceiveNearestLights(result: OsmQueryResult, fromDatabase: Boolean = false) {
        if (!fromDatabase) {
            // TODO: 4/5/2018 save in database
        }

        nearestLights = null
        val lights = ArrayList<TrafficLight>()
        for (element in result.elements) {
            lights.add(TrafficLight(element.lat, element.lon))
        }
        nearestLights = lights
        provider.onReceiveNearestLights(lights)
    }


    override fun getNearestLight(currentLocation: Location): TrafficLight? {
        nearestLights ?: return null
        lastQueryLightLocation = currentLocation

        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        val movementVector = getMovementVector(currentLocation)

        val closestLights = nearestLights!!
                .filter { isLightCloserThan(it, NEAREST_LIGHT_DISTANCE, currentLocation) }
                .filter { isLightInFront(it, latLng, movementVector) }
                .sortedBy { getDistance(latLng, LatLng(it.lat, it.lng)) }

        if (DEBUG) Log.d(TAG, "(79, GreenwaveModel.kt) nearest lights: $closestLights")

        if (closestLights.isNotEmpty()) {
            closestLight = closestLights.first()
            return closestLight
        }
        return null
    }

    private fun getMovementVector(currentLocation: Location): Pair<Double, Double> {
        return Pair(
                Math.cos(degreeToRadian(currentLocation.bearing.toDouble())),
                Math.sin(degreeToRadian(currentLocation.bearing.toDouble())))
    }

    override fun getValidClosestLight(location: Location): TrafficLight? {
        closestLight ?: return null
        val movementVector = getMovementVector(location)
        if (isLightInFront(closestLight!!, LatLng(location.latitude, location.longitude), movementVector))
            return closestLight
        return null
    }

    override fun detectNotableDistanceFromLastQueryLight(currentLocation: Location): Boolean {
        lastQueryLightLocation ?: return true
        return lastQueryLightLocation?.distanceTo(currentLocation)!! > NOTABLE_DISTANCE
    }

    private fun degreeToRadian(deg: Double): Double {
        return deg / 180.0 * Math.PI
    }

    private fun getDistance(from: LatLng, to: LatLng): Double {
        return Math.sqrt(
                Math.pow(from.latitude - to.latitude, 2.0) +
                        Math.pow(from.longitude - to.longitude, 2.0))
    }

    private fun isLightInFront(light: TrafficLight, position: LatLng, movementVector: Pair<Double, Double>): Boolean {
        val lightVector = Pair(light.lng - position.longitude, light.lat - position.latitude)
        val cos = movementVector.scalar(lightVector)
        return cos > 0
    }

    private fun isLightCloserThan(light: TrafficLight, distance: Double, location: Location): Boolean {
        return location.distanceTo(light.location) < distance
    }

}

private fun Pair<Double, Double>.scalar(vector: Pair<Double, Double>): Double {
    return this.first * vector.first + this.second * vector.second
}

