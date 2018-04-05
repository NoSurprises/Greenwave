package nick.greenwave

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import nick.greenwave.data.TrafficLight
import nick.greenwave.receiver.SpeedListener
import utils.*
import java.util.*


class GreenwaveModel(val provider: GreenwaveProviderApi) : GreenwaveModelApi, SpeedListener {

    val lastLocations = PreviousLocations()
    var context: Context? = null
    private val TAG = "GreenwaveModel"
    private val timer = Timer("SpeedMeasurement")

    val osmService
            by lazy { OsmService.create() }
    val locationClient: FusedLocationProviderClient
            by lazy { LocationServices.getFusedLocationProviderClient(context!!) }
    val locationRequest: LocationRequest
            by lazy { LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) }


    override fun startTrackingSpeed() {
        context = provider.getApplicationContext()

//        setTimerToMeasureSpeed() // todo testing with default speed, remove this if default works
    }

    private fun setTimerToMeasureSpeed() {
//        timer.schedule(SPEED_UPDATE_INTERVAL) { timeToMeasureSpeed() }
    }


    @SuppressLint("MissingPermission")
    override fun timeToMeasureSpeed() {
        if (DEBUG) Log.d(TAG, "(38, GreenwaveModel.kt) timeToMeasureSpeed ")

        context?.let { locationClient.lastLocation.addOnSuccessListener { calculateSpeed(it) } }
        setTimerToMeasureSpeed()
    }

    override fun stopTrackingSpeed() {
//        timer.cancel()
    }

    private fun calculateSpeed(location: Location) {
        if (DEBUG) Log.d(TAG, "(63, GreenwaveModel.kt) calculateSpeed, locations: ${lastLocations.getLastLocations()}")
        lastLocations.addLocation(location)
        provider.onSpeedChanged(lastLocations.calculateLastMeanDistance(), LAST_2_SPEED_MEASURMENT)
        provider.onSpeedChanged(lastLocations.calculateMeanHistoryDistance(), ALL_MEAN_SPEED_MEASURMENT)

    }


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

        val lights = ArrayList<TrafficLight>()
        for (element in result.elements) {
            lights.add(TrafficLight(element.lat, element.lon))
        }
        provider.onReceiveNearestLights(lights)
    }


}

