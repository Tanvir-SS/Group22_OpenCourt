package com.example.group22_opencourt.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "courts")
class Court(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val address: String,
    val surface: String,
    val indoor : Boolean,
    val location: LatLng,
    val totalCourts: Int,
)