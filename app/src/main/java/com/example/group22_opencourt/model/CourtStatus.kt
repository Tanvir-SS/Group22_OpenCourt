package com.example.group22_opencourt.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "court_status")
class CourtStatus(
    @PrimaryKey
    val courtId: Int,
    val courtsInUse: Int,
    val updatedBy: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val photoURL: String
)

