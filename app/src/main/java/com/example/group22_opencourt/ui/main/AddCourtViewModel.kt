package com.example.group22_opencourt.ui.main

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddCourtViewModel : ViewModel() {

    private val _selectedImageUri = MutableLiveData<Uri?>(null)
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    private val _currentPhotoUri = MutableLiveData<Uri?>(null)
//    val currentPhotoUri: LiveData<Uri?> = _currentPhotoUri

    fun onCameraPhotoPrepared(uri: Uri) {
        _currentPhotoUri.value = uri
    }

    fun onCameraPhotoCaptured() {
        // When a photo is successfully captured, promote currentPhotoUri to selectedImageUri
        Log.d("photo", _currentPhotoUri.value.toString())
        _selectedImageUri.value = _currentPhotoUri.value
    }

    fun onGalleryImagePicked(uri: Uri) {
        _selectedImageUri.value = uri
        _currentPhotoUri.value = null
    }

    fun clearImage() {
        _selectedImageUri.value = null
        _currentPhotoUri.value = null
    }
}

