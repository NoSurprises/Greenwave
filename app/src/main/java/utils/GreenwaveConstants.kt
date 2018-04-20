package utils

import android.Manifest
import com.google.android.gms.maps.model.LatLng

const val SECOND_IN_MILLIS = 1000L
const val MINUNE_IN_MILLIS = SECOND_IN_MILLIS * 60

const val STANDARD_ZOOM = 13f
const val MEDIUM_ZOOM = 10f
const val STANDARD_TILT = 50f
val STANDARD_LOCATION = LatLng(55.7604523, 37.5772471)
const val LOCATION_UPDATE_INTERVAL = 5000L
val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
val LOCATION_PERMISSION_REQUEST = 22

val SPEED_UPDATE_INTERVAL = 5 * 1000L
val MAX_LOCATIONS_IN_HISTORY = 3

val LAST_2_SPEED_MEASURMENT = 11
val ALL_MEAN_SPEED_MEASURMENT = 23

val EXTRAS_LIGHT_INFO = "extras_light_info"

val OVERPASS_QUERY_DELIMETER = "%bounds%"
val OVERPASS_QUERY = "[out:json];(node[crossing=traffic_signals]$OVERPASS_QUERY_DELIMETER);out body center qt 100;"

val NEAREST_LIGHTS_MARGIN = 0.01f // ~1.5 km
val NEAREST_LIGHT_DISTANCE = 800.0 // m
val MARKER_EPSILON = 0.00003f

val NOTABLE_DISTANCE = 100.0 //m
val LIGHTS_REFERENCE_FIREBASE = "lights"
val TIMER_NAME_GREEN = "green_timer"