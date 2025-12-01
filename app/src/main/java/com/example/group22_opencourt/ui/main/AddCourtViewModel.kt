package com.example.group22_opencourt.ui.main

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddCourtViewModel : ViewModel() {

    // LiveData to hold the selected image URI
    private val _selectedImageUri = MutableLiveData<Uri?>(null)
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    // LiveData to hold the current photo URI from the camera
    private val _currentPhotoUri = MutableLiveData<Uri?>(null)

    // Function to handle photo prepared from camera
    fun onCameraPhotoPrepared(uri: Uri) {
        _currentPhotoUri.value = uri
    }

    // Function to handle photo captured from camera
    fun onCameraPhotoCaptured() {
        // When a photo is successfully captured, promote currentPhotoUri to selectedImageUri
        Log.d("photo", _currentPhotoUri.value.toString())
        _selectedImageUri.value = _currentPhotoUri.value
    }

    // Function to handle image picked from gallery
    fun onGalleryImagePicked(uri: Uri) {
        _selectedImageUri.value = uri
        _currentPhotoUri.value = null
    }

    // Function to clear the selected image
    fun clearImage() {
        _selectedImageUri.value = null
        _currentPhotoUri.value = null
    }
}

