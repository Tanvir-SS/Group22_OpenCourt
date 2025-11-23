package com.example.group22_opencourt.model

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration

class FirestoreDocumentLiveData<T>(
    private val docRef: DocumentReference,
) : LiveData<T>() {
    private var listener: ListenerRegistration? = null

    override fun onActive() {
        super.onActive()
        listener = docRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val type = snapshot.getString("type") ?: ""
                val court: Court? = when (type) {
                    "tennis" -> snapshot.toObject(TennisCourt::class.java)?.apply { base.id = snapshot.id }
                    "basketball" -> snapshot.toObject(BasketballCourt::class.java)?.apply { base.id = snapshot.id }
                    else -> snapshot.toObject(TennisCourt::class.java)?.apply { base.id = base.id } // fallback
                }
                value = court as T?
            }

        }
    }

    override fun onInactive() {
        super.onInactive()
        listener?.remove()
        listener = null
    }
}

