package nick.greenwave

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import nick.greenwave.data.dto.LightSettings
import nick.greenwave.settings.SettingsActivity
import utils.*

val DEBUG = true

class GreenwaveActivity : AppCompatActivity(), OnMapReadyCallback, GreenwaveView {
    private val provider: GreenwaveProviderApi = GreenwaveProvider(this)

    private val TAG = "GreenwaveActivity"
    private var locationCallback: LocationCallback? = null
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }


    private val lonView by lazy { findViewById<TextView>(R.id.lon) }
    private val latView by lazy { findViewById<TextView>(R.id.lat) }
    private val requestNearestLights by lazy { findViewById<Button>(R.id.request_nearest) }
    private val speedView by lazy { findViewById<TextView>(R.id.current_speed) }
    private val speedHistoryView by lazy { findViewById<TextView>(R.id.current_speed_history) }
    private var map: GoogleMap? = null
    private var mCameraPosition: CameraPosition? = null
    private val markers = ArrayList<Marker?>()

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.greenwave)

        if (DEBUG) Log.d(TAG, "onCreate: ")
        requestNearestLights.setOnClickListener { getDeviceLocation()?.addOnSuccessListener { provider.requestNearestLights(it) } }
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap?) {
        if (DEBUG) Log.d(TAG, "(53, GreenwaveActivity.kt) onMapReady $map")
        this.map = map

        map?.setOnMapLongClickListener { provider.addMapMark(it) }
        map?.setOnCameraMoveStartedListener {
            provider.onCameraMoved()
        }

        provider.onMapReady(map)

    }

    override fun setActiveColorMarker(latLng: LatLng) {
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

    private fun isApproximatelyTheSameLight(latLng: LatLng, marker: Marker): Boolean {
        return (Math.abs(latLng.latitude - marker.position.latitude) < MARKER_EPSILON &&
                Math.abs(latLng.longitude - marker.position.longitude) < MARKER_EPSILON)
    }

    override fun resetMarkersColors() {
        for (i in markers) {
            i?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        }
    }

    override fun removeAllMarks() {
        markers.forEach { it?.remove() }
        markers.clear()
    }

    override fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            provider.onPermissionsGranted()
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

    override fun addMark(latLng: LatLng) {
        val markOptions = MarkerOptions()
                .position(latLng)
                .title("LightSettings")
                .snippet("${latLng.latitude} ${latLng.longitude}")


        markers.add(map?.addMarker(markOptions))
        map?.setOnMarkerClickListener { openLightPopup(it) }
        map?.setOnInfoWindowClickListener { provider.openLightSettings(it) }

    }

    private fun openLightPopup(marker: Marker): Boolean {
        marker.showInfoWindow()

        return true
    }

    override fun setLon(lon: Double) {
        lonView.text = lon.toString()
        if (DEBUG) Log.d(TAG, "set new longtitude: $lon to $lonView")
    }

    override fun setLat(lat: Double) {
        latView.text = lat.toString()
        if (DEBUG) Log.d(TAG, "set new latitude: $lat to $latView")
    }

    override fun onResume() {
        super.onResume()
        provider.onResume()
    }

    override fun onPause() {
        super.onPause()
        provider.onPause()
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
            provider.onPermissionsGranted()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun registerActivityRecognition() {
        val transitions = ArrayList<ActivityTransition>()

        transitions.add(
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build())

        transitions.add(
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build())

        val request = ActivityTransitionRequest(transitions)
//        val pendingIntent = PendingIntent.getForegroundService() // todo register a foreground service (or receiver), that will update the textview
//        val task =  ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request,)
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(loc: LocationResult?) {
                if (DEBUG) Log.d(TAG, "(101, GreenwaveActivity.kt) onLocationResult: setting new location $loc")
                if (DEBUG) Log.d(TAG, "locations: ${loc?.locations}")
                if (DEBUG) Log.d(TAG, "lastLocation: ${loc?.lastLocation}")

                loc?.let { provider.onLocationUpdate(loc) }
            }
        }
    }


    override fun setCurrentSpeed(speed: Double) {
        speedView.text = String.format("%.2f", speed)
    }

    override fun startSettingsActivy(lightSettingsInfo: LightSettings) {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra(EXTRAS_LIGHT_INFO, lightSettingsInfo)
        startActivity(intent)
    }
}