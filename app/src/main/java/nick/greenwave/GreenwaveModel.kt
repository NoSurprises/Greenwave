package nick.greenwave

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import nick.greenwave.data.Storage
import nick.greenwave.data.TrafficLight
import nick.greenwave.data.dto.LightSettings
import utils.*
import java.util.*
import kotlin.concurrent.timer


class GreenwaveModel(val presenter: GreenwavePresenterApi) : GreenwaveModelApi {

    var context: Context? = null
    private val TAG = "GreenwaveModel"
    private var nearestLights: List<TrafficLight>? = null
    val osmService by lazy { OsmService.create() }
    private var lastQueryLightLocation: Location? = null
    private var closestLight: TrafficLight? = null

    private val userLightsStorage by lazy { Storage(presenter.getApplicationContext()) }

    override fun createIdentifierFromLatlng(latLng: LatLng): String {
        val result = "${latLng.latitude}-${latLng.longitude}"
        return result.replace('.', ';')
    }


    override fun requestSettingsForLight(identifier: String, request: Int) {
        val lightsRef = FirebaseDatabaseSingletone.getFirebaseInstance().getReference(LIGHTS_REFERENCE_FIREBASE)
        if (DEBUG) Log.d(TAG, "(35, GreenwaveModel.kt) requestSettingsForLight $lightsRef identifier=$identifier")
        // todo check if key exists

        val noSettingsTimer = timer("no_settings", false, SECOND_IN_MILLIS, SECOND_IN_MILLIS,
                {
                    replyToRequest(request, LightSettings(identifier = identifier))
                    this.cancel()
                })
        lightsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var lightSettings = LightSettings(identifier = identifier)
                if (DEBUG) Log.d(TAG, "(38, GreenwaveModel.kt) getting settings from firebase, check if key exists in $snapshot")
                if (snapshot.hasChild(identifier)) {
                    val data = snapshot.child(identifier).getValue(LightSettings::class.java)
                    lightSettings = data!!
                }
                if (DEBUG) Log.d(TAG, "(43, GreenwaveModel.kt) firebase settings: $lightSettings ")
                replyToRequest(request, lightSettings)
                noSettingsTimer.cancel()
            }

            override fun onCancelled(p0: DatabaseError?) {
                if (DEBUG) Log.d(TAG, "(44, GreenwaveModel.kt) onCancelled firebase: $p0")
            }
        })
    }

    /**
     * Firebase callback
     */
    private fun replyToRequest(request: Int, settings: LightSettings) {
        if (DEBUG) Log.d(TAG, "(70, GreenwaveModel.kt) replyToRequest: $settings")
        when (request) {
            REQUEST_FIREBASE_CLOSEST_LIGHT_SETTINGS ->
                (presenter as FirebaseLightSettingsCallback).getSettings(settings)
            REQUEST_FIREBASE_SETTINGS_TO_OPEN_SETTINGS ->
                (presenter as FirebaseLightSettingsCallback).openSettings(settings)
        }
    }

    override fun updateLightSettingsInRemoteDb(light: LightSettings) {
        val lightsRef = FirebaseDatabaseSingletone.getFirebaseInstance().getReference(LIGHTS_REFERENCE_FIREBASE)
        lightsRef.child(light.identifier).setValue(light)
    }

    private fun createQuery(lat: Float, lng: Float): String {
        val bounds = "(${lat - NEAREST_LIGHTS_MARGIN},${lng - NEAREST_LIGHTS_MARGIN}," +
                "${lat + NEAREST_LIGHTS_MARGIN},${lng + NEAREST_LIGHTS_MARGIN})"

        val result = StringBuilder()
        OVERPASS_QUERY.split(OVERPASS_QUERY_DELIMETER).joinTo(result, bounds)
        if (DEBUG) Log.d(TAG, "(88, GreenwaveModel.kt) createdQuery: $result")
        return result.toString()
    }

    override fun requestNearestLights(lat: Float, lng: Float) {
        requestNearestLightsFromApi(lat, lng)
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

    private fun onReceiveNearestLights(result: OsmQueryResult, fromDatabase: Boolean = false) {
        if (!fromDatabase) {
            // TODO: 4/5/2018 save in database
        }
        nearestLights = null
        val lights = ArrayList<TrafficLight>()
        for (element in result.elements) {
            val identifier = "${element.lat}-${element.lon}".replace('.', ';')
            val light = TrafficLight(element.lat, element.lon, LightSettings(identifier = identifier))
            if (DEBUG) Log.d(TAG, "(124, GreenwaveModel.kt) create TrafficLight ${light}")
            lights.add(light)
        }
        userLightsStorage.getAllLights().forEach({ lights.add(it) })

        nearestLights = lights
        presenter.onReceiveNearestLights(lights)
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

    override fun getDefinedClosestFrontLight(location: Location): TrafficLight? {
        closestLight ?: return null
        val movementVector = getMovementVector(location)
        // Check if user has passed the light. If not so, return
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

