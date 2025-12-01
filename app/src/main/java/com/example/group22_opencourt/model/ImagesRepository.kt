package com.example.group22_opencourt.model

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.group22_opencourt.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

import kotlin.coroutines.suspendCoroutine


class ImagesRepository {

    // initialize Places client and Firebase Storage
    private lateinit var placesClient: PlacesClient
    private val storage = Firebase.storage
    private val rootRef = storage.reference

    // create singleton instance
    companion object {
        val instance: ImagesRepository by lazy { ImagesRepository() }
        val URI_NON_EXIST = "NO_PHOTO"
        val FAIl = "fail"
    }

    // load court photo into ImageView using Glide
    fun loadCourtPhoto(context: Context, photoUri: String?, imageView: ImageView) {
        if (photoUri.isNullOrEmpty() || photoUri == URI_NON_EXIST) {
            return
        }
        Glide.with(context)
            .load(photoUri)
            .into(imageView)
    }

    suspend fun getCourtDetailsFromAddress(context : Context, address: String) : Triple<GeoPoint?, String, String>? {
        // Initialize Places if not already initialized
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context.getApplicationContext(), BuildConfig.MAPS_API_KEY)
            placesClient = Places.createClient(context.applicationContext)
        }
        if (!(::placesClient.isInitialized)){
            placesClient = Places.createClient(context.applicationContext)
        }
        // Search for place ID
        val placeId : String? = searchPlace(address)
        if (placeId == null) {
            return null
        }
        // Fetch place details
        return fetchPlaceDetails(placeId)
    }

    suspend fun searchPlace(query: String) : String? {
        // Use Places SDK to search for place ID
        return suspendCoroutine { cont ->
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build()

            // Perform the autocomplete request
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    if (response.autocompletePredictions.isEmpty()) {
                        cont.resume(null)
                        return@addOnSuccessListener
                    }

                    // Take the first result
                    val prediction = response.autocompletePredictions[0]
                    val placeId = prediction.placeId

                    cont.resume(placeId)
                }
                .addOnFailureListener {
                    cont.resume(null)
                }

        }
    }

    private suspend fun fetchPlaceDetails(placeId: String) : Triple<GeoPoint?, String, String>? {
        // Use Places SDK to fetch place details
        return suspendCoroutine { cont ->
            val fields = listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
            )
            // Create a request object
            val request = FetchPlaceRequest.newInstance(placeId, fields)
            // Fetch place details
            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val placeId = place.id ?: ""
                    val address = place.address ?: ""
                    val lat = place.latLng?.latitude ?: 0.0
                    val lng = place.latLng?.longitude ?: 0.0
                    var geoPoint : GeoPoint? = GeoPoint(lat, lng)
                    if (lat == 0.0 && lng == 0.0) {
                        geoPoint = null
                    }
                    cont.resume(Triple(geoPoint, placeId, address))
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }

    }

    suspend fun ensurePhotoForPlace(context : Context, court: Court) {
        // Initialize Places if not already initialized
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context.getApplicationContext(), BuildConfig.MAPS_API_KEY)
            placesClient = Places.createClient(context.applicationContext)
        }
        if (!(::placesClient.isInitialized)){
            placesClient = Places.createClient(context.applicationContext)
        }
        // If already has local/remote photoUri → done
        if (!court.base.photoUri.isEmpty()) return

        Log.d("fetching", "court ${court.base.name}")
        // Fetch from Places
        val uri = fetchPlacePhotoUri(context, court.base.placesId)
        var uriStr = uri.toString()
        if (uri == null) {
            uriStr = ImagesRepository.URI_NON_EXIST
        }
        // Update Firestore
        updatePhotoUriInFirestore(court, uriStr)
        return
    }

    private suspend fun fetchPlacePhotoUri(context: Context, placeId: String): Uri? =
        suspendCoroutine { cont ->

            // fetch place to get photo metadata
            val placeRequest = FetchPlaceRequest.builder(
                placeId,
                listOf(Place.Field.PHOTO_METADATAS)
            ).build()

            placesClient.fetchPlace(placeRequest)
                .addOnSuccessListener { response ->
                    val metadata = response.place.photoMetadatas?.firstOrNull()
                    if (metadata == null) {
                        cont.resume(null)
                        return@addOnSuccessListener
                    }

                    // build photo request with max dimensions
                    val photoRequest = FetchResolvedPhotoUriRequest.builder(metadata)
                        .setMaxWidth(400)
                        .setMaxHeight(400)
                        .build()

                    // fetch resolved photo URI
                    placesClient.fetchResolvedPhotoUri(photoRequest)
                        .addOnSuccessListener {response: FetchResolvedPhotoUriResponse ->
                            val uri: Uri? = response.uri   // <-- extract the URI
                            cont.resume(uri)
                        }
                        .addOnFailureListener {
                            cont.resume(null)
                        }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }

    private suspend fun updatePhotoUriInFirestore(court: Court, uri: String): Boolean =
        suspendCoroutine { cont ->
            // update court's photoUri in Firestore
            court.base.photoUri = uri
            CourtRepository.instance.updateCourt(court) {
                if (it) {
                    cont.resume(true)
                } else {
                    cont.resume(false)
                }
            }
        }


    suspend fun uploadPhotoToFirebase(prefix: String, photoUri : Uri): String {
        try {
            // upload photo to Firebase Storage and return download URL
            val photosRef = rootRef.child("user_photos/${prefix}${System.currentTimeMillis()}.jpg")
            photosRef.putFile(photoUri).await()
            return photosRef.downloadUrl.await().toString()
        } catch (e : Exception) {
            // return fail if upload fails
            return FAIl
        }
    }

}