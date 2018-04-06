package utils

import com.google.gson.annotations.SerializedName
import java.util.*

class OsmQueryResult {
    @SerializedName("elements")
    var elements: List<Element> = ArrayList()

    class Element {
        @SerializedName("type")
        var type: String? = null

        @SerializedName("id")
        var id: Long = 0

        @SerializedName("lat")
        var lat: Double = 0.toDouble()

        @SerializedName("lon")
        var lon: Double = 0.toDouble()

        @SerializedName("tags")
        var tags = Tags()

        class Tags {
            @SerializedName("type")
            var type: String? = null

            @SerializedName("amenity")
            var amenity: String? = null

            @SerializedName("name")
            var name: String? = null

            @SerializedName("phone")
            var phone: String? = null

            @SerializedName("contact:email")
            var contactEmail: String? = null

            @SerializedName("website")
            var website: String? = null

            @SerializedName("addr:city")
            var addressCity: String? = null

            @SerializedName("addr:postcode")
            var addressPostCode: String? = null

            @SerializedName("addr:street")
            var addressStreet: String? = null

            @SerializedName("addr:housenumber")
            var addressHouseNumber: String? = null

            @SerializedName("wheelchair")
            var wheelchair: String? = null

            @SerializedName("wheelchair:description")
            var wheelchairDescription: String? = null

            @SerializedName("opening_hours")
            var openingHours: String? = null

            @SerializedName("internet_access")
            var internetAccess: String? = null

            @SerializedName("fee")
            var fee: String? = null

            @SerializedName("operator")
            var operator: String? = null

        }
    }
}


