package com.example.group22_opencourt.model

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// class to represent current weather data
data class CurrentWeather(
    val tempC: Double,
    val weatherCode: Int,
    val description: String,
    val windKmh: Double,
)

object WeatherRepository {
    // fetch current weather data from open-meteo.com API
    suspend fun fetchCurrent(lat: Double, lon: Double): CurrentWeather {
        // build URL for API request
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,wind_speed_10m" +
                    "&temperature_unit=celsius&wind_speed_unit=kmh" +
                    "&timezone=auto"
        )

        // open connection and set timeouts
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        // read response body
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val current = json.getJSONObject("current")

        // parse relevant fields
        val temp = current.getDouble("temperature_2m")
        val code = current.getInt("weather_code")
        val wind = current.getDouble("wind_speed_10m")

        // return CurrentWeather object
        return CurrentWeather(
            tempC = temp,
            weatherCode = code,
            description = weatherCodeToText(code),
            windKmh = wind
        )
    }

    // website uses WMO weather codes; keep this simple for UI
    private fun weatherCodeToText(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Unknown"
    }
}
