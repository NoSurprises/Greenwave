package nick.greenwave

import android.content.Context

import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

interface GreenwaveProviderApi {

    fun onMapReady(map: GoogleMap?)

    fun onPermissionsGranted()

    fun onPause()

    fun onResume()

    fun onLocationUpdate(location: LocationResult?)

    fun onSpeedChanged(newSpeed: Double, variant: Int)

    fun getApplicationContext(): Context

    fun addMapMark(latLng: LatLng)

    fun openLightSettings(marker: Marker)


}