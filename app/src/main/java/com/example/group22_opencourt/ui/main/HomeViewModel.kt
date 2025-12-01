package com.example.group22_opencourt.ui.main

import CourtRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.group22_opencourt.model.Court


class HomeViewModel : ViewModel() {
    // get the current list of courts from the repository
    private val repository = CourtRepository.instance
    val courts: LiveData<List<Court>> = repository.courts
}
