package com.example.group22_opencourt.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.group22_opencourt.model.Court

//added for the weather display
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.group22_opencourt.model.CurrentWeather
import com.example.group22_opencourt.model.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class WeatherUiState {
    data object Idle : WeatherUiState()
    data object Loading : WeatherUiState()
    data class Ready(val weather: CurrentWeather) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class CourtDetailViewModel(documentId: String) : ViewModel() {
    val courtLiveData: LiveData<Court?> = CourtRepository.instance.getCourtLiveData(documentId)

    //added for the weather
    private val _weather = MutableLiveData<WeatherUiState>(WeatherUiState.Idle)
    val weather: LiveData<WeatherUiState> = _weather

    private var lastKey: String? = null
    private var lastFetchMs: Long = 0L
    private val minRefreshMs = 30 * 60 * 1000L // 30 minutes

    fun setWeatherLoading() {
        _weather.value = WeatherUiState.Loading
    }

    fun setWeatherError(message: String) {
        _weather.value = WeatherUiState.Error(message)
    }

    fun loadWeather(lat: Double, lon: Double, force: Boolean = false) {
        val key = "${"%.4f".format(lat)},${"%.4f".format(lon)}"
        val now = System.currentTimeMillis()

        if (!force && key == lastKey && (now - lastFetchMs) < minRefreshMs) return
        lastKey = key
        lastFetchMs = now

        _weather.value = WeatherUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val w = WeatherRepository.fetchCurrent(lat, lon)
                withContext(Dispatchers.Main) {
                    _weather.value = WeatherUiState.Ready(w)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _weather.value = WeatherUiState.Error("Weather unavailable")
                }
            }
        }
    }
}

