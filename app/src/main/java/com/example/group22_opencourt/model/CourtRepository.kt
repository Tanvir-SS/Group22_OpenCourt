import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.example.group22_opencourt.BuildConfig
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.TennisCourt
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.FirestoreDocumentLiveData
import com.google.firebase.Firebase
import com.google.firebase.firestore.*

class CourtRepository private constructor() {

    private val db = Firebase.firestore
    private val courtsCollection = db.collection("courts")

    private val _courts = MutableLiveData<List<Court>>()
    val courts: LiveData<List<Court>> = _courts

    private var listenerRegistration: ListenerRegistration? = null

    /** Start listening for courts in a city */
    fun listenCourtsByCity(city: String) {
        listenerRegistration?.remove()
        listenerRegistration = courtsCollection
//            .whereEqualTo("city", city)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val currentCourts = _courts.value?.toMutableList() ?: mutableListOf()

                for (change in snapshot.documentChanges) {
                    val doc = change.document
                    val type = doc.getString("type") ?: ""
                    val court: Court = when (type) {
                        "tennis" -> doc.toObject(TennisCourt::class.java).apply { base.id = doc.id }
                        "basketball" -> doc.toObject(BasketballCourt::class.java).apply { base.id = doc.id }
                        else -> doc.toObject(TennisCourt::class.java).apply { base.id = doc.id } // fallback
                    }

                    when (change.type) {
                        DocumentChange.Type.ADDED -> currentCourts.add(court)
                        DocumentChange.Type.MODIFIED -> {
                            val index = currentCourts.indexOfFirst { it.base.id == court.base.id }
                            if (index != -1) {
                                currentCourts[index] = court
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            currentCourts.removeAll { it.base.id == court.base.id }
                        }
                    }
                }
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
                court.base.id = docRef.id
                onComplete?.invoke(true)
            }
            .addOnFailureListener {
                onComplete?.invoke(false)
            }
    }

    /** Update an existing court */
    fun updateCourt(court: Court, onComplete: ((Boolean) -> Unit)? = null) {
        if (court.base.id.isNotEmpty()) {
            courtsCollection.document(court.base.id)
                .set(court, SetOptions.merge())
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        }
    }

    /** Delete a court */
    fun deleteCourt(court: Court, onComplete: ((Boolean) -> Unit)? = null) {
        if (court.base.id.isNotEmpty()) {
            courtsCollection.document(court.base.id)
                .delete()
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        }
    }

    /** Get LiveData for a single court document by ID */
    fun getCourtLiveData(id: String): FirestoreDocumentLiveData<Court?> {
        return FirestoreDocumentLiveData(courtsCollection.document(id))
    }

    companion object {
        val instance: CourtRepository by lazy { CourtRepository() }

        fun loadPhoto(court : Court, imageView : ImageView) {
            if (court.base.photoURL.isEmpty()) {
                return
            }
            val photoUrl =
                "https://maps.googleapis.com/maps/api/place/photo" +
                        "?maxwidth=400" +
                        "&photoreference=${court.base.photoURL}" +
                        "&key=${BuildConfig.MAPS_API_KEY}"
            Glide.with(imageView.context)
                .load(photoUrl)
                .into(imageView)
        }

        fun loadMapPhoto(court : Court, imageView : ImageView) {
            val geoPoint = court.base.geoPoint ?: return
            val lat = geoPoint.latitude
            val lng = geoPoint.longitude

            val mapUrl =
                "https://maps.googleapis.com/maps/api/staticmap" +
                        "?center=$lat,$lng" +
                        "&zoom=16" +
                        "&size=600x400" +
                        "&markers=color:red%7C$lat,$lng" +
                        "&key=${BuildConfig.MAPS_API_KEY}"

            Glide.with(imageView.context)
                .load(mapUrl)
                .into(imageView)
        }
    }
}
