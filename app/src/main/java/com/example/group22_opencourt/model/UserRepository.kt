package com.example.group22_opencourt.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

/**
 * Repository for user documents in Firestore.
 *
 * Collection: "users" where each document ID is the FirebaseAuth UID.
 * Mirrors the structure and behavior of [CourtRepository].
 */
class UserRepository private constructor() {
    // Firebase Auth instance
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    // LiveData for the currently logged-in user
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Listener registration for Firestore updates
    private var listenerRegistration: ListenerRegistration? = null

    /** Start listening to the currently logged-in user's document (by FirebaseAuth UID). */
    fun listenCurrentUser() {
        // Remove previous listener if any
        listenerRegistration?.remove()
        listenerRegistration = null

        // Get current user's UID
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _currentUser.postValue(null)
            return
        }

        // Start listening to the user's document
        listenerRegistration = usersCollection
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _currentUser.postValue(null)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                   if ( snapshot.exists()) {
                        val user = snapshot.toObject(User::class.java)?.apply { id = snapshot.id }
                        user?.id = uid
                        _currentUser.postValue(user)
                    } else {
                        _currentUser.postValue(User(id = uid))
                   }
                }

            }
    }

    /** Stop listening for the current user's updates. */
    fun removeListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /**
     * Create or update a user document whose ID must be the FirebaseAuth UID.
     *
     * Call this with a User where user.id == FirebaseAuth.getInstance().currentUser?.uid.
     */
    fun createOrUpdateUser(user: User, onComplete: ((Boolean) -> Unit)? = null) {
        if (user.id.isEmpty()) {
            onComplete?.invoke(false)
            return
        }

        // Set the user document with merge option
        usersCollection.document(user.id)
            .set(user, SetOptions.merge())
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    /** Delete a user by their FirebaseAuth UID (stored in user.id). */
    fun deleteUser(user: User, onComplete: ((Boolean) -> Unit)? = null) {
        if (user.id.isNotEmpty()) {
            usersCollection.document(user.id)
                .delete()
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        }
    }

    companion object {
        // current instance of the repository
        val instance: UserRepository by lazy { UserRepository() }
    }
}
