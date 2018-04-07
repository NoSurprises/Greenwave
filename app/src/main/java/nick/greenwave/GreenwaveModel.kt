package nick.greenwave

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import nick.greenwave.data.TrafficLight
import utils.*
import java.util.*


class GreenwaveModel(val provider: GreenwaveProviderApi) : GreenwaveModelApi {

    var context: Context? = null
    private val TAG = "GreenwaveModel"
    private val timer = Timer("SpeedMeasurement")
    private var nearestLights: List<TrafficLight>? = null
    val osmService
            by lazy { OsmService.create() }

    override fun requestNearestLights(lat: Float, lng: Float) {
        // TODO: 4/5/2018 get lights from db, key is a composition of lon and lat
        requestNearestLightsFromApi(lat, lng)
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

    override fun getNearestLight(location: Location): TrafficLight? {
        if (DEBUG) Log.d(TAG, "(74, GreenwaveModel.kt) nearestLights: $nearestLights")
        nearestLights ?: return null

        val latLng = LatLng(location.latitude, location.longitude)
        val movementVector = Pair(
                Math.cos(degreeToRadian(location.bearing.toDouble())),
                Math.sin(degreeToRadian(location.bearing.toDouble())))

        val closest = nearestLights!!
                .filter { isLightCloserThan(it, NEAREST_LIGHT_DISTANCE, latLng) }
                .filter { isLightInFront(it, latLng, movementVector) }
                .sortedBy { getDistance(latLng, LatLng(it.lat, it.lng)) }

        if (DEBUG) Log.d(TAG, "(79, GreenwaveModel.kt) nearest lights: $closest")

        if (closest.isNotEmpty()) {
            return closest.first()
        }
        return null
    }

    private fun degreeToRadian(deg: Double): Double {
        return deg / 180.0 * Math.PI
    }

    private fun getDistance(from: LatLng, to: LatLng): Double { //todo replace with google api
        return Math.sqrt(
                Math.pow(from.latitude - to.latitude, 2.0) +
                        Math.pow(from.longitude - to.longitude, 2.0))
    }

    private fun isLightInFront(light: TrafficLight, position: LatLng, movementVector: Pair<Double, Double>): Boolean {
        val lightVector = Pair(light.lat - position.latitude, light.lng - position.longitude)
        val cos = movementVector.scalar(lightVector)
        return cos > 0
    }

    private fun isLightCloserThan(light: TrafficLight, distance: Double, position: LatLng): Boolean {
        return getDistance(position, LatLng(light.lat, light.lng)) < distance
    }

}

private fun Pair<Double, Double>.scalar(vector: Pair<Double, Double>): Double {
    return this.first * vector.first + this.second * vector.second
}

