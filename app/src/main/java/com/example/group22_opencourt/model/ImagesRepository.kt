package com.example.group22_opencourt.model

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

import kotlin.coroutines.suspendCoroutine


class ImagesRepository {
    companion object {
        val instance: ImagesRepository by lazy { ImagesRepository() }
    }




    suspend fun ensurePhotoForPlace(context : Context, court: Court) {
        // If already has local/remote photoUri → done
        if (!court.base.photoURL.isEmpty()) return

        // Fetch from Places
        val bitmap = fetchPlacePhoto(context, court.base.placesId)
        if (bitmap == null) return

        // Upload to Firebase
        val uri = uploadPhotoToFirebase(court.base.placesId, bitmap)
        if (uri == null) return

        // Update Firestore
        updatePhotoUriInFirestore(court, uri)

        return
    }

    private suspend fun fetchPlacePhoto(context: Context, placeId: String): Bitmap? = suspendCoroutine { cont ->
        val placesClient = Places.createClient(context)

        val photoRequest = FetchPlaceRequest.builder(
            placeId,
            listOf(Place.Field.PHOTO_METADATAS)
        ).build()

        placesClient.fetchPlace(photoRequest)
            .addOnSuccessListener { response ->
                val metadata = response.place.photoMetadatas?.firstOrNull()
                if (metadata == null) {
                    cont.resume(null)
                    return@addOnSuccessListener
                }

                val photoRequest2 = FetchPhotoRequest.builder(metadata)
                    .setMaxWidth(800)      // <-- prevent huge images
                    .setMaxHeight(800)
                    .build()

                placesClient.fetchPhoto(photoRequest2)
                    .addOnSuccessListener { photoResponse ->
                        cont.resume(photoResponse.bitmap)
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
            .addOnFailureListener { cont.resume(null) }
    }

    private suspend fun uploadPhotoToFirebase(placeId: String, bitmap: Bitmap): Uri? =
        suspendCoroutine { cont ->

            val storageRef = FirebaseStorage.getInstance()
                .reference
                .child("place_photos/$placeId.jpg")

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val data = baos.toByteArray()

            storageRef.putBytes(data)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload error")
                    storageRef.downloadUrl
                }
                .addOnSuccessListener { uri -> cont.resume(uri) }
                .addOnFailureListener { cont.resume(null) }
        }
    private suspend fun updatePhotoUriInFirestore(court: Court, uri: Uri): Boolean =
        suspendCoroutine { cont ->
            court.base.photoURL = uri.toString()
            CourtRepository.instance.updateCourt(court) {
                if (it) {
                    cont.resume(true)
                } else {
                    cont.resume(false)
                }
            }
        }
}