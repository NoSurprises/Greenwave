package utils

import java.util.*

val COUNT_OF_MEASUREMENTS = 3
class MeanSpeed {
    private var speeds = ArrayList<Double>()
    fun addSpeed(speed: Double) {
        if (speeds.size == COUNT_OF_MEASUREMENTS) {
            speeds.removeAt(0)
        }
        speeds.add(speed)
    }

    fun getMeanSpeed() : Double  {
        return speeds.sum() / speeds.size
    }



}