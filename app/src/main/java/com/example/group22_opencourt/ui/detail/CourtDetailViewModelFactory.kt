package com.example.group22_opencourt.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CourtDetailViewModelFactory(private val documentId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourtDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourtDetailViewModel(documentId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

