package com.example.group22_opencourt.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class User (
    @PrimaryKey
    val uid: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val joinedAt: Long = System.currentTimeMillis(),
    val numberOfReports : Int
)