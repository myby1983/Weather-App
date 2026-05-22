package com.example.data.repository

import com.example.data.api.GeocodingResult
import com.example.data.api.RetrofitClient
import com.example.data.api.WeatherResponse
import com.example.data.db.FavoriteLocation
import com.example.data.db.UserSession
import com.example.data.db.WeatherDao
import kotlinx.coroutines.flow.Flow

class WeatherRepository(private val weatherDao: WeatherDao) {

    val activeSession: Flow<UserSession?> = weatherDao.getActiveSession()
    val favoriteLocations: Flow<List<FavoriteLocation>> = weatherDao.getFavoriteLocations()

    suspend fun saveSession(session: UserSession) {
        weatherDao.saveSession(session)
    }

    suspend fun clearSession() {
        weatherDao.clearSession()
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherResponse {
        return RetrofitClient.weatherApi.getForecast(latitude, longitude)
    }

    suspend fun searchCity(name: String): List<GeocodingResult> {
        val result = RetrofitClient.geocodingApi.searchCity(name)
        return result.results ?: emptyList()
    }

    suspend fun addFavorite(location: FavoriteLocation) {
        weatherDao.addFavoriteLocation(location)
    }

    suspend fun removeFavoriteByName(name: String) {
        weatherDao.deleteFavoriteLocationByName(name)
    }
}
