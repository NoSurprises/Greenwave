package nick.greenwave

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task


class GreenwaveActivity : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = "GreenwaveActivity"
    private val DEBUG: Boolean = BuildConfig.DEBUG
    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private val PERMISSION_REQUEST = 22
    private val STANDARD_ZOOM = 18f
    private val STANDARD_TILT = 50f
    private val STANDARD_LOCATION = LatLng(55.7604523, 37.5772471)

    private var map: GoogleMap? = null
    private var userActivity = ""
    private var lon: Double = 0.0
    private var lat: Double = 0.0
    private var locationCallback: LocationCallback? = null
    private val fusedLocationClient by lazy {  LocationServices.getFusedLocationProviderClient(this) }
    private val lonView by lazy { findViewById<TextView>(R.id.lon) }
    private val latView by lazy { findViewById<TextView>(R.id.lat) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.greenwave)

        if (DEBUG) Log.d(TAG, "onCreate: ")
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)
    }


    override fun onMapReady(map: GoogleMap?) {
        if (DEBUG) Log.d(TAG, "map is ready: $map")
        this.map = map

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            defaultCameraSettings()
            mapToDeviceLocation()
            registerLocationUpdate()
        } else {
            if (DEBUG) android.util.Log.d(TAG, "location permission not granted, requesting")
            ActivityCompat.requestPermissions(this, Array(1) { LOCATION_PERMISSION }, PERMISSION_REQUEST)
        }
    }

    private fun registerForActivityRecognition() {
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
//        val pendingIntent = PendingIntent.getForegroundService() // todo register a foreground service, that will update the textview
//        val task =  ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request,)
    }

    @SuppressLint("MissingPermission")
    private fun registerLocationUpdate() {
        locationCallback ?: createLocationCallback()
        fusedLocationClient.requestLocationUpdates(LocationRequest.create(), locationCallback, null)
    }
    private fun unregisterLocationUpdate() {
        locationCallback ?: return
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    private fun createLocationCallback() {

        locationCallback = object: LocationCallback() {
            override fun onLocationResult(loc: LocationResult?) {
                loc ?: return
                if (DEBUG) Log.d(TAG, "locations: ${loc.locations}")
                if (DEBUG) Log.d(TAG, "lastLocation: ${loc.lastLocation}")
                loc.lastLocation ?: return
                setLon(loc.lastLocation.longitude)
                setLat(loc.lastLocation.latitude)
            }
        }
    }
    private fun setLon(lon: Double) {
        this.lon = lon
        lonView.setText(lon.toString())
        if (DEBUG) Log.d(TAG, "set new longtitude: $lon to $lonView");
    }
    private fun setLat(lat: Double) {
        this.lat = lat
        latView.setText(lat.toString())
        if (DEBUG) Log.d(TAG, "set new latitude: $lat to $latView");
    }

    private fun defaultCameraSettings() {
        val position = CameraPosition.builder()
                .target(STANDARD_LOCATION)
                .zoom(STANDARD_ZOOM)
                .tilt(STANDARD_TILT)
                .bearing(0f)
                .build()
        map?.moveCamera(CameraUpdateFactory.newCameraPosition(position))

        enableMyLocationButton()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationButton() {
        map?.isMyLocationEnabled = true

        map?.setOnMyLocationButtonClickListener {
            mapToDeviceLocation()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        map ?: return
        registerLocationUpdate()
    }

    override fun onPause() {
        super.onPause()
        map?: return
        unregisterLocationUpdate()
    }


    private fun mapToDeviceLocation() {
        getDeviceLocation()?.addOnSuccessListener {
            map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude), STANDARD_ZOOM))
        }
    }

    private fun getDeviceLocation(): Task<Location>? {
        return fusedLocationClient.lastLocation
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST && permissions[0] == LOCATION_PERMISSION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationButton()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}