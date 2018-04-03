package nick.greenwave

import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import nick.greenwave.dto.Light
import utils.ALL_MEAN_SPEED_MEASURMENT
import utils.LAST_2_SPEED_MEASURMENT
import utils.OsmService

class GreenwaveProvider(val view: GreenwaveView) : GreenwaveProviderApi {

    val osmService by lazy {
        OsmService.create()
    }
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


        val bounds = "(55.79,37.52,55.81,37.55)"
        val query = "[out:json];(node[crossing=traffic_signals]$bounds);out body center qt 100;"

        if (DEBUG) Log.d(TAG, "(92, GreenwaveProvider.kt) addMapMark: $query")
        osmService.fetchChunkData(query)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->
                            Log.d(TAG, "result = ${result.elements}")
                        },
                        { error ->
                            Log.d(TAG, "error: $error")
                        }
                )



    }

    override fun openLightSettings(marker: Marker) {
        if (DEBUG) Log.d(TAG, "(80, GreenwaveProvider.kt) openLightSettings for ${marker.snippet}")
        // todo get data from model

        view.startSettingsActivy(Light(0, 0, 22))
    }
}