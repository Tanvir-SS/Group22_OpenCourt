package com.example.group22_opencourt.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.group22_opencourt.model.Court

class CourtDetailViewModel(documentId: String) : ViewModel() {
    val courtLiveData: LiveData<Court?> = CourtRepository.instance.getCourtLiveData(documentId)
}

