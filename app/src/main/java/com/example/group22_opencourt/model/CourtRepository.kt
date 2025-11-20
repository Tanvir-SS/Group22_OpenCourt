import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.group22_opencourt.model.Court
import com.google.firebase.Firebase
import com.google.firebase.firestore.*


class CourtRepository {

    private val db = Firebase.firestore
    private val courtsCollection = db.collection("courts")

    private val _courts = MutableLiveData<List<Court>>()
    val courts: LiveData<List<Court>> = _courts

    private var listenerRegistration: ListenerRegistration? = null

    /** Start listening for courts in a city */
    fun listenCourtsByCity(city: String) {
        // Remove any previous listener
        listenerRegistration?.remove()
        listenerRegistration = courtsCollection
//            .whereEqualTo("city", city)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                // Get current value or start with empty list
                val currentCourts = _courts.value?.toMutableList() ?: mutableListOf()

                // Process incremental changes
                for (change in snapshot.documentChanges) {
                    val court = change.document.toObject(Court::class.java)
                    court.id = change.document.id

                    when (change.type) {
                        DocumentChange.Type.ADDED -> currentCourts.add(court)
                        DocumentChange.Type.MODIFIED -> {
                            val index = currentCourts.indexOfFirst { it.id == court.id }
                            if (index != -1) {
                                currentCourts[index] = court
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            currentCourts.removeAll { it.id == court.id }
                        }
                    }
                }

                // Post updated list to LiveData
                _courts.postValue(currentCourts)
            }
    }

    /** Stop listening for updates */
    fun removeListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /** Add a new court */
    fun addCourt(court: Court, onComplete: ((Boolean) -> Unit)? = null) {
        courtsCollection.add(court)
            .addOnSuccessListener { docRef ->
                court.id = docRef.id
                onComplete?.invoke(true)
            }
            .addOnFailureListener {
                onComplete?.invoke(false)
            }
    }

    /** Update an existing court */
    fun updateCourt(court: Court, onComplete: ((Boolean) -> Unit)? = null) {
        if (court.id.isNotEmpty()) {
            courtsCollection.document(court.id)
                .set(court, SetOptions.merge())
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        }
    }

    /** Delete a court */
    //Unit is void
    fun deleteCourt(court: Court, onComplete: ((Boolean) -> Unit)? = null) {
        if (court.id.isNotEmpty()) {
            courtsCollection.document(court.id)
                .delete()
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        }

    }
}
