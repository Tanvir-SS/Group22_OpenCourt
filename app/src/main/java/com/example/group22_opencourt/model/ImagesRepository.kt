package com.example.group22_opencourt.model

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.group22_opencourt.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse
import com.google.android.libraries.places.api.net.PlacesClient
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

import kotlin.coroutines.suspendCoroutine


class ImagesRepository {

    private lateinit var placesClient: PlacesClient
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
                        .setMaxWidth(800)
                        .setMaxHeight(800)
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


//    private suspend fun uploadPhotoToFirebase(placeId: String, bitmap: Bitmap): Uri? =
//        suspendCoroutine { cont ->
//
//            val storageRef = FirebaseStorage.getInstance()
//                .reference
//                .child("place_photos/$placeId.jpg")
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
}