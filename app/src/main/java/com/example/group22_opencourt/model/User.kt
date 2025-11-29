package com.example.group22_opencourt.model

/**
 * Basic Firestore-mappable user model.
 * Assumes document ID == [id] (typically FirebaseAuth UID).
 */
data class User(
    var id: String = "",
    var email: String = "",
    var favourites : ArrayList<String> = ArrayList()
)
