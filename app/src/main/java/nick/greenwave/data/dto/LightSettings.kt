package nick.greenwave.data.dto

import android.os.Parcel
import android.os.Parcelable

data class LightSettings(var greenCycle: Int = 0,
                         var redCycle: Int = 0,
                         var startOfMeasurement: Long = 0L,
                         var identifier: String = "") : Parcelable {


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
    }
}