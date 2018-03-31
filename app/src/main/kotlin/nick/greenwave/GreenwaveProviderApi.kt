package nick.greenwave

import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap

interface GreenwaveProviderApi {
    fun onMapReady(map: GoogleMap?)

    fun onPermissionsGranted()

    fun onPause()

    fun onResume()

    fun onLocationUpdate(location: LocationResult?)


}