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
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

import kotlin.coroutines.suspendCoroutine


class ImagesRepository {

    private lateinit var placesClient: PlacesClient

    private val storage = Firebase.storage
    val rootRef = storage.reference
    companion object {
        val instance: ImagesRepository by lazy { ImagesRepository() }
        val URI_NON_EXIST = "NO_PHOTO"

    }
    fun loadCourtPhoto(context: Context, photoUri: String?, imageView: ImageView) {
        if (photoUri.isNullOrEmpty() || photoUri == URI_NON_EXIST) {
            return
        }
        Glide.with(context)
            .load(photoUri)
            .into(imageView)
    }

    suspend fun getCourtDetailsFromAddress(context : Context, address: String) : Triple<GeoPoint?, String, String>? {
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context.getApplicationContext(), BuildConfig.MAPS_API_KEY)
            placesClient = Places.createClient(context.applicationContext)
        }
        if (!(::placesClient.isInitialized)){
            placesClient = Places.createClient(context.applicationContext)
        }
        val placeId : String? = searchPlace(address)
        if (placeId == null) {
            return null
        }
        return fetchPlaceDetails(placeId)
    }

    suspend fun searchPlace(query: String) : String? {
        return suspendCoroutine { cont ->
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build()

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
        return suspendCoroutine { cont ->
            val fields = listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
            )

            val request = FetchPlaceRequest.newInstance(placeId, fields)

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

            // Step 1: Fetch Place with PHOTO_METADATAS
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

                    // Step 2: Build FetchPhotoRequest
                    val photoRequest = FetchResolvedPhotoUriRequest.builder(metadata)
                        .setMaxWidth(400)
                        .setMaxHeight(400)
                        .build()

// Step 3: Fetch resolved URI
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
            court.base.photoUri = uri
            CourtRepository.instance.updateCourt(court) {
                if (it) {
                    cont.resume(true)
                } else {
                    cont.resume(false)
                }
            }
        }


//        private suspend fun uploadPhotoToFirebase(courtId: String, bitmap: Bitmap): Uri? =
//        suspendCoroutine { cont ->
//            val imagesRef = rootRef.child("images/$courtId.jpg")
//
//
//            val baos = ByteArrayOutputStream()
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
//            val data = baos.toByteArray()
//
//            storageRef.putBytes(data)
//                .continueWithTask { task ->
//                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload error")
//                    storageRef.downloadUrl
//                }
//                .addOnSuccessListener { uri -> cont.resume(uri) }
//                .addOnFailureListener { cont.resume(null) }
//        }
}