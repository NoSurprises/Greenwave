package nick.greenwave


interface GreenwaveModelApi {
    fun startTrackingSpeed()
    fun stopTrackingSpeed()
    fun requestNearestLights(lat: Float, lng: Float)

}