package nick.greenwave.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import nick.greenwave.DEBUG
import nick.greenwave.data.dto.LightSettings

const val TAG = "Storage"

class Storage(private val context: Context) {

    private val preferences: SharedPreferences
            by lazy { context.getSharedPreferences("lights", Context.MODE_PRIVATE) }

    fun saveToPreferences(light: TrafficLight) {
        preferences.edit().putString(createKey(light), createValue(light)).apply()
    }

    private fun createKey(light: TrafficLight): String {
        return "${light.lat}-${light.lng}".replace('.',';')
    }

    private fun createValue(light: TrafficLight): String {
        return "${light.settings.greenCycle}-" +
                "${light.settings.redCycle}-" +
                "${light.settings.startOfMeasurement}"
    }

    private fun parseValue(key: String, value: String): TrafficLight {
        if (DEBUG) Log.d(TAG, "(29, Storage.kt) parseValue: $value key $key")
        val pair = parseKey(key)
        val result = TrafficLight(pair.first, pair.second)
        result.settings = LightSettings.parseFromString(value)
        result.settings.identifier = key
        if (DEBUG) Log.d(TAG, "(29, Storage.kt) parsed: $result")

        return result
    }

    companion object {
        fun parseKey(key: String): Pair<Double, Double> {
            val pair = key.split("-")
            return Pair(pair[0].replace(';', '.').toDouble(),
                    pair[1].replace(';', '.').toDouble())
        }
    }

    fun getAllLights() : List<TrafficLight> {
        val userLights = ArrayList<TrafficLight> ()
        preferences.all.forEach({userLights.add(parseValue(it.key, it.value as String))})
        return  userLights
    }
}