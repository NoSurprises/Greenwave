package nick.greenwave.data

import android.content.Context
import android.content.SharedPreferences

class Storage(val context: Context) {

    val preferences: SharedPreferences by lazy { context.getSharedPreferences("lights", Context.MODE_PRIVATE) }

    fun saveToPreferences(light: TrafficLight) {
        preferences.edit().putString(createKey(light), createValue(light)).apply()
    }

    fun getFromPreferences(key: String): TrafficLight? {
        if (!preferences.contains(key)) return null;
        return parseValue(key, preferences.getString(key, ""))
    }


    fun createKey(light: TrafficLight): String {
        return "${light.lat}-${light.lng}"
    }

    private fun createValue(light: TrafficLight): String {
        return "${light.settings.greenCycle}-${light.settings.redCycle}-${light.settings.startOfMeasurement}-"
    }

    fun parseValue(key: String, value: String): TrafficLight {
        val pair = parseKey(key)
        val result = TrafficLight(pair.first, pair.second)
        val values = value.split("-")
        result.settings.greenCycle = values[0].toInt()
        result.settings.redCycle = values[0].toInt()
        result.settings.startOfMeasurement = values[0].toLong()
        return result
    }

    fun parseKey(key: String): Pair<Double, Double> {
        val pair = key.split("-")
        return Pair(pair[0].toDouble(), pair[1].toDouble())
    }
}