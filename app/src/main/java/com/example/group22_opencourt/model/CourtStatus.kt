package com.example.group22_opencourt.model

// class to represent the status of a court
data class CourtStatus(
    var courtAvailable : Boolean = true,
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val photoURL: String = ""
)

