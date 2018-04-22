package nick.greenwave.data.dto

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import nick.greenwave.DEBUG

val TAG = "LightSettings"
data class LightSettings(var greenCycle: Int = 0,
                         var redCycle: Int = 0,
                         var startOfMeasurement: Long = 0L,
                         var identifier: String = "1") : Parcelable {


    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt(),
            parcel.readLong(),
            parcel.readString())


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeInt(greenCycle)
        p0?.writeInt(redCycle)
        p0?.writeLong(startOfMeasurement)
        p0?.writeString(identifier)
    }

    companion object CREATOR : Parcelable.Creator<LightSettings> {
        override fun createFromParcel(parcel: Parcel): LightSettings {
            return LightSettings(parcel)
        }

        override fun newArray(size: Int): Array<LightSettings?> {
            return arrayOfNulls(size)
        }
        fun parseFromString(str: String): LightSettings {
            val result = LightSettings()
            if (DEBUG) Log.d(TAG, "(40, LightSettings.kt) parseFromString: $str")
            val tokens = str.split("-")
            result.greenCycle = tokens[0].toInt()
            result.redCycle = tokens[1].toInt()
            result.startOfMeasurement = tokens[2].toLong()
            return result
        }
    }

    fun isSet() :Boolean {
        return startOfMeasurement != 0L
    }

    override fun toString(): String {
        return "$greenCycle-$redCycle-$startOfMeasurement"
    }


}