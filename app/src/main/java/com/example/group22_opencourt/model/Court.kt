package com.example.group22_opencourt.model

import com.google.android.gms.maps.model.LatLng

data class Court(
    var id: String = "",
    val city: String = "",
    val name: String = "",
    val address: String = "",
    val washroom: Boolean = false,
    val indoor : Boolean = false,
    val lights : Boolean = false,
    val latLng: LatLng = LatLng(1.0,1.0),
    val totalCourts: Int = 1,
    val photoURL: String = "",
    val courtStatus : ArrayList<CourtStatus> = ArrayList<CourtStatus>()
)