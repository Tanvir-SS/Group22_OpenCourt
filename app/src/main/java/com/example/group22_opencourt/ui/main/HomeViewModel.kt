package com.example.group22_opencourt.ui.main

import CourtRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.group22_opencourt.model.Court


class HomeViewModel : ViewModel() {
    private val repository = CourtRepository.instance
    val courts: LiveData<List<Court>> = repository.courts

    init {
        // Listen for all courts (city filter can be added if needed)
        repository.listenCourtsByCity("")
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeListener()
    }
}
