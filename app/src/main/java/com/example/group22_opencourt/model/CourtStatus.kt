package com.example.group22_opencourt.model

data class CourtStatus(
    val courtAvailable : Boolean = true,
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val photoURL: String = ""
)

