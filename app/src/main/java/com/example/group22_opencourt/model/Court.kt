package com.example.group22_opencourt.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

data class CourtBase(
    var id: String = "",
    var city: String = "",
    var name: String = "",
    var address: String = "",
    var washroom: Boolean? = null,
    var indoor: Boolean? = null,
    var lights: Boolean? = null,
    var accessibility : Boolean? = null,
    var totalCourts: Int = 1,
    var courtsAvailable: Int = 1,
    var placesId : String = "",
    var photoUri: String = "",
    var photoURL : String = "",
    var courtStatus: ArrayList<CourtStatus> = ArrayList(),
    var lastUpdate : Long = 0,
    var geoPoint: GeoPoint? = null
)

sealed interface Court {
    var base: CourtBase
    var type: String
}

data class TennisCourt(
    override var base: CourtBase = CourtBase(),
    override var type: String = "tennis",
    var practiceWall: Boolean? = null
) : Court

data class BasketballCourt(
    override var base: CourtBase = CourtBase(),
    override var type: String = "basketball",
    var nets: Boolean? = null
) : Court
