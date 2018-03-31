package utils

import android.Manifest
import com.google.android.gms.maps.model.LatLng

const val STANDARD_ZOOM = 18f
const val MEDIUM_ZOOM = 10f
const val STANDARD_TILT = 50f
val STANDARD_LOCATION = LatLng(55.7604523, 37.5772471)
const val LOCATION_UPDATE_INTERVAL = 5000L
val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
val LOCATION_PERMISSION_REQUEST = 22
