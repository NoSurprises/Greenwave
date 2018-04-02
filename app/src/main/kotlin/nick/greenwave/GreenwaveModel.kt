package nick.greenwave

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import nick.greenwave.receiver.SpeedListener
import utils.ALL_MEAN_SPEED_MEASURMENT
import utils.LAST_2_SPEED_MEASURMENT
import utils.PreviousLocations
import java.util.*


class GreenwaveModel(val provider: GreenwaveProviderApi) : GreenwaveModelApi, SpeedListener {

    val lastLocations = PreviousLocations()
    var context: Context? = null
    private val TAG = "GreenwaveModel"
    private val timer = Timer("SpeedMeasurement")

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


}

