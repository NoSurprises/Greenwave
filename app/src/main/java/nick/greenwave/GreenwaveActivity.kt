package nick.greenwave

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import nick.greenwave.data.dto.LightSettings
import nick.greenwave.settings.SettingsActivity
import utils.*
import java.util.*
import java.util.concurrent.TimeUnit

const val DEBUG = true

private val SETTINGS_ACTIVITY_REQUEST_CODE = 23

class GreenwaveActivity : AppCompatActivity(), OnMapReadyCallback, GreenwaveView {
    private val presenter: GreenwavePresenterApi = GreenwavePresenter(this)

    private val TAG = "GreenwaveActivity"
    private var locationCallback: LocationCallback? = null
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val requestNearestLights by lazy { findViewById<Button>(R.id.request_nearest) }
    private val requestClosestLight by lazy { findViewById<Button>(R.id.get_closest) }
    private val speedView by lazy { findViewById<TextView>(R.id.current_speed) }
    private val recommendedSpeed by lazy { findViewById<TextView>(R.id.recommended_speed) }
    private val recommendedLabel by lazy { findViewById<TextView>(R.id.recommended_speed_label) }
    private val recommendedUnits by lazy { findViewById<TextView>(R.id.recommended_speed_units) }
    private val remainingDistance by lazy { findViewById<TextView>(R.id.distance_remaining) }
    private val remainingDistanceLabel by lazy { findViewById<TextView>(R.id.distance_label) }
    private val remainingDistanceUnits by lazy { findViewById<TextView>(R.id.distance_units) }

    private val lightInfoView by lazy { findViewById<LinearLayout>(R.id.light_settings_view) }
    private val redCycle by lazy { lightInfoView.findViewById<TextView>(R.id.red_cycle) }
    private val greenCycle by lazy { lightInfoView.findViewById<TextView>(R.id.green_cycle) }
    private val current by lazy { lightInfoView.findViewById<TextView>(R.id.current) }
    private val select by lazy { lightInfoView.findViewById<Button>(R.id.select_current) }
    private val settings by lazy { lightInfoView.findViewById<Button>(R.id.open_settings) }
    private var lastMarkerClicked: Marker? = null

    private var followUser = true
    private var stopSelectedMarkerTimer = false

    private val timeToGreen by lazy { findViewById<TextView>(R.id.time) }
    private val timeToGreenLabel by lazy { findViewById<TextView>(R.id.time_label) }
    private val timeToGreenUnits by lazy { findViewById<TextView>(R.id.time_units) }
    private var map: GoogleMap? = null
    private var mCameraPosition: CameraPosition? = null
    private val markers = ArrayList<Marker?>()
    private var selectedMarkerCurrentLight: Disposable? = null

    override var cameraPosition: CameraPosition?
        get() = mCameraPosition
        set(value) {
            mCameraPosition = value
        }
    private val locationUpdateRequest by lazy {
        LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(SECOND_IN_MILLIS)
    }

    override fun canMoveCamera(): Boolean {
        return followUser
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.greenwave)

        if (DEBUG) Log.d(TAG, "onCreate: ")
        requestNearestLights.setOnClickListener {
            getDeviceLocation()?.addOnSuccessListener { presenter.requestNearestLights(it) }
        }
        requestClosestLight.setOnClickListener({ presenter.forceChooseNewClosestLight() })
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)
        hideSuggestions()
    }

    override fun onMapReady(map: GoogleMap?) {
        if (DEBUG) Log.d(TAG, "(53, GreenwaveActivity.kt) onMapReady $map")
        this.map = map

        map?.setOnMapLongClickListener { presenter.addMapMark(it) }
        map?.setOnCameraMoveStartedListener {
            presenter.onCameraMoved()
        }
        map?.setOnMarkerClickListener { showLightInfoView(it) }
        map?.setOnInfoWindowClickListener {
            Log.d(TAG, "choose light")
            presenter.chooseNewLight(it.position)
        }
        map?.setOnInfoWindowLongClickListener { presenter.openLightSettings(it) }

        map?.setOnMapClickListener {
            hideLightInfoView()
            followUser = false // ux feature
        }

        presenter.onMapReady(map)
        getDeviceLocation()?.addOnSuccessListener { presenter.requestNearestLights(it) }

    }

    override fun setDistance(distance: Double) {
        remainingDistance.text = distance.toInt().toString()
    }

    override fun setRecommendedSpeed(speed: Double) {
        recommendedSpeed.text = String.format("%.2f", speed)
    }

    override fun setActiveColorMarker(latLng: LatLng) {
        if (DEBUG) Log.d(TAG, "(125, GreenwaveActivity.kt) setActiveColorMarker: markers $markers")
        if (markers.isEmpty()) {
            return
        }
        var current: Marker? = null
        for (i in markers) {
            current = i
            if (current?.position?.equals(latLng)!!)
                break
        }
        current?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
    }

    override fun setTimeToGreen(time: Int) {
        timeToGreen.text = time.toString()
    }

    override fun resetMarkersColors() {
        if (DEBUG) Log.d(TAG, "(142, GreenwaveActivity.kt) resetMarkersColors")
        markers.forEach { it?.setIcon(BitmapDescriptorFactory.defaultMarker()) }
    }

    override fun setEmptyRecommendedFields() {
        recommendedSpeed.text = ""
        timeToGreen.text = ""
    }

    override fun removeAllMarks() {
        markers.forEach { it?.remove() }
        markers.clear()
    }

    override fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            presenter.onPermissionsGranted()
        } else {
            if (DEBUG) Log.d(TAG, "(62, GreenwaveActivity.kt) onMapReady: request location permission")
            ActivityCompat.requestPermissions(this, Array(1) { LOCATION_PERMISSION }, LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun defaultCameraSettings(): CameraPosition? {
        return CameraPosition.builder()
                .target(STANDARD_LOCATION)
                .zoom(STANDARD_ZOOM)
                .tilt(STANDARD_TILT)
                .bearing(0f)
                .build()

    }

    override fun moveCameraTo(position: CameraPosition) {
        this.cameraPosition = position
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(position))
    }


    @SuppressLint("MissingPermission")
    override fun enableMyLocationButton() {
        map?.isMyLocationEnabled = true

        map?.setOnMyLocationButtonClickListener {
            mapToDeviceLocation()
            followUser = true
            true
        }
    }

    @SuppressLint("MissingPermission")
    override fun registerLocationUpdate() {
        if (DEBUG) Log.d(TAG, "(89, GreenwaveActivity.kt) registerLocationUpdate ")
        locationCallback ?: createLocationCallback()
        val request = locationUpdateRequest
        fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
    }

    override fun unregisterLocationUpdate() {
        if (DEBUG) Log.d(TAG, "(96, GreenwaveActivity.kt) unregisterLocationUpdate")
        locationCallback ?: return
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun addMark(latLng: LatLng, openSettings: Boolean) {
        val markOptions = MarkerOptions()
                .position(latLng)
                .title("LightSettings")
                .snippet("${latLng.latitude} ${latLng.longitude}")

        val marker = map?.addMarker(markOptions)
        markers.add(marker)

        if (openSettings) {
            marker?.let { presenter.openLightSettings(marker) }
        }

    }

    override fun onReceiveSettings(light: LightSettings) {

        lightInfoView.visibility = View.VISIBLE
        greenCycle.text = light.greenCycle.toString()
        redCycle.text = light.redCycle.toString()
        current.text = ""
        lastMarkerClicked?.let { settings.setOnClickListener { presenter.openLightSettings(lastMarkerClicked!!) } }
        lastMarkerClicked?.let { select.setOnClickListener { presenter.chooseNewLight(lastMarkerClicked!!.position) } }

        stopSelectedMarkerTimer = false
        if (!light.isSet()) {
            return
        }
        selectedMarkerCurrentLight = Observable.timer(SECOND_IN_MILLIS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .repeatUntil { stopSelectedMarkerTimer }
                .subscribe({
                    val currentTime = Date().time
                    val diff = ((currentTime - light.startOfMeasurement) / 1000) % (light.redCycle + light.greenCycle)
                    if (diff < light.greenCycle) {
                        val greenText = "Green ${light.greenCycle - diff}"
                        current.text = greenText
                        current.setTextColor(Color.GREEN)
                    } else {
                        val redText = "Red ${light.redCycle - (diff - light.greenCycle)}"
                        current.text = redText
                        current.setTextColor(Color.RED)
                    }
                })
    }

    private fun hideLightInfoView() {
        stopSelectedMarkerTimer = true
        selectedMarkerCurrentLight?.dispose()
        lightInfoView.visibility = View.GONE
    }

    override fun hideSuggestions() {
        remainingDistance.visibility = View.GONE
        remainingDistanceLabel.visibility = View.GONE
        remainingDistanceUnits.visibility = View.GONE

        timeToGreen.visibility = View.GONE
        timeToGreenLabel.visibility = View.GONE
        timeToGreenUnits.visibility = View.GONE

        recommendedSpeed.visibility = View.GONE
        recommendedLabel.visibility = View.GONE
        recommendedUnits.visibility = View.GONE

    }

    override fun showSuggestions() {
        remainingDistance.visibility = View.VISIBLE
        remainingDistanceLabel.visibility = View.VISIBLE
        remainingDistanceUnits.visibility = View.VISIBLE

        timeToGreen.visibility = View.VISIBLE
        timeToGreenLabel.visibility = View.VISIBLE
        timeToGreenUnits.visibility = View.VISIBLE

        recommendedSpeed.visibility = View.VISIBLE
        recommendedLabel.visibility = View.VISIBLE
        recommendedUnits.visibility = View.VISIBLE

    }

    private fun showLightInfoView(marker: Marker): Boolean {
        lastMarkerClicked = marker
        marker.showInfoWindow()
        hideLightInfoView()
        presenter.requestSettingsFor(marker.position)
        return true
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun mapToDeviceLocation() {
        getDeviceLocation()?.addOnSuccessListener {
            if (DEBUG) Log.d(TAG, "(158, GreenwaveActivity.kt) mapToDeviceLocation: location = $it")
            it?.apply {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude, it.longitude),
                        STANDARD_ZOOM))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation(): Task<Location>? {
        return fusedLocationClient.lastLocation
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST && permissions[0] == LOCATION_PERMISSION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            presenter.onPermissionsGranted()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(loc: LocationResult?) {
                if (DEBUG) Log.d(TAG, "(101, GreenwaveActivity.kt) onLocationResult: setting new location $loc")
                if (DEBUG) Log.d(TAG, "locations: ${loc?.locations}")
                if (DEBUG) Log.d(TAG, "lastLocation: ${loc?.lastLocation}")

                loc?.let { presenter.onLocationUpdate(loc) }
            }
        }
    }


    override fun setCurrentSpeed(speed: Double) {
        speedView.text = String.format("%.2f", speed)
    }

    override fun startSettingsActivy(lightSettingsInfo: LightSettings) {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra(EXTRAS_LIGHT_INFO, lightSettingsInfo)
        startActivityForResult(intent, SETTINGS_ACTIVITY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SETTINGS_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            presenter.updateLightSettings(data?.getParcelableExtra(EXTRAS_LIGHT_INFO))
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}