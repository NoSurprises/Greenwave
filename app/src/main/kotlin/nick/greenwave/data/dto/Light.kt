package nick.greenwave.data.dto

import android.os.Parcel
import android.os.Parcelable

data class Light(var greenCycle: Int,
                 var redCycle: Int,
                 var startOfMeasurement: Long) : Parcelable {


    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt(),
            parcel.readLong())

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel?, p1: Int) {
        p0?.writeInt(greenCycle)
        p0?.writeInt(redCycle)
        p0?.writeLong(startOfMeasurement)
    }

    companion object CREATOR : Parcelable.Creator<Light> {
        override fun createFromParcel(parcel: Parcel): Light {
            return Light(parcel)
        }

        override fun newArray(size: Int): Array<Light?> {
            return arrayOfNulls(size)
        }
    }
}