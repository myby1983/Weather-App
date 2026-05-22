package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.R
import com.example.data.api.GeocodingResult
import com.example.data.api.WeatherResponse
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteLocation
import com.example.data.db.UserSession
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(
        val cityName: String,
        val country: String?,
        val elevation: Double,
        val temp: Double,
        val humidity: Double,
        val windSpeed: Double,
        val rainProb: Int,
        val dailyDates: List<String>,
        val dailyMaxTemp: List<Double>,
        val dailyMinTemp: List<Double>,
        val dailyPrecipProb: List<Int>,
        val activeDayIndex: Int = 0,
        val isSevereAlertActive: Boolean = false,
        val severeAlertTitle: String? = null,
        val severeAlertMessage: String? = null
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

@OptIn(FlowPreview::class)
class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = WeatherRepository(database.weatherDao())

    val userSession: StateFlow<UserSession?> = repository.activeSession
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val favorites: StateFlow<List<FavoriteLocation>> = repository.favoriteLocations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentLatitude = 37.7749
    private var currentLongitude = -122.4194
    private var currentCityName = "San Francisco"
    private var currentCountry: String? = "United States"

    private val CHANNEL_ID = "severe_weather_alerts"
    private val NOTIFICATION_ID = 4529

    init {
        createNotificationChannel()
        // Load default city (San Francisco) on startup
        loadWeatherFor(currentLatitude, currentLongitude, currentCityName, currentCountry)

        // Observe search queries and search automatically with a small debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .filter { it.isNotBlank() && it.length >= 2 }
                .distinctUntilChanged()
                .collect { query ->
                    try {
                        val results = repository.searchCity(query)
                        _searchResults.value = results
                    } catch (e: Exception) {
                        _searchResults.value = emptyList()
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
        }
    }

    fun loadWeatherFor(latitude: Double, longitude: Double, cityName: String, country: String?) {
        currentLatitude = latitude
        currentLongitude = longitude
        currentCityName = cityName
        currentCountry = country

        viewModelScope.launch {
            _weatherUiState.value = WeatherUiState.Loading
            try {
                val response = repository.fetchWeather(latitude, longitude)
                _weatherUiState.value = mapResponseToSuccess(response, cityName, country)
            } catch (e: Exception) {
                _weatherUiState.value = WeatherUiState.Error(e.localizedMessage ?: "Failed to retrieve local weather.")
            }
        }
    }

    private fun mapResponseToSuccess(response: WeatherResponse, cityName: String, country: String?): WeatherUiState.Success {
        val current = response.current
        val daily = response.daily ?: throw IllegalStateException("Daily forecast is missing from Open-Meteo response.")

        // Precipitation probability can be calculated from current hourly estimation or mapped to daily max chance
        val maxRainProb = daily.precipitationProbabilityMax?.getOrNull(0) ?: 0

        val successState = WeatherUiState.Success(
            cityName = cityName,
            country = country,
            elevation = response.elevation,
            temp = current?.temperature2m ?: 21.0,
            humidity = current?.relativeHumidity2m ?: 50.0,
            windSpeed = current?.windSpeed10m ?: 12.0,
            rainProb = maxRainProb,
            dailyDates = daily.time,
            dailyMaxTemp = daily.temperature2mMax,
            dailyMinTemp = daily.temperature2mMin,
            dailyPrecipProb = daily.precipitationProbabilityMax ?: List(daily.time.size) { 0 },
            activeDayIndex = 0
        )

        // Auto-examine weather extremes to trigger simulated push notifications for severe conditions
        checkAndWarnOfExtremes(successState)

        return successState
    }

    private fun checkAndWarnOfExtremes(state: WeatherUiState.Success) {
        viewModelScope.launch {
            if (state.temp > 38.0) {
                triggerSevereWeatherNotification(
                    "Severe Heat Wave Alert for ${state.cityName}!",
                    "Hazardous temperatures exceeding 38°C detected. Stay indoors, hydrate regularly!"
                )
            } else if (state.windSpeed > 45.0) {
                triggerSevereWeatherNotification(
                    "Gale Force Winds Warning for ${state.cityName}!",
                    "Dangerous winds averaging ${state.windSpeed} km/h recorded. Secure loose property outdoors!"
                )
            } else if (state.rainProb > 85 && (state.dailyPrecipProb.firstOrNull() ?: 0) > 85) {
                triggerSevereWeatherNotification(
                    "Flash Flood Risk in ${state.cityName}!",
                    "Extreme precipitation probability (${state.rainProb}%) detected. Extreme rain forecasted."
                )
            }
        }
    }

    fun selectActiveDayIndex(index: Int) {
        val state = _weatherUiState.value
        if (state is WeatherUiState.Success) {
            _weatherUiState.value = state.copy(activeDayIndex = index)
        }
    }

    // Google Sign-On actions
    fun handleGoogleSignIn(email: String, name: String, photoUrl: String?) {
        viewModelScope.launch {
            val session = UserSession(
                email = email,
                displayName = name,
                photoUrl = photoUrl,
                idToken = "google-mock-jwt-id-token"
            )
            repository.saveSession(session)
        }
    }

    fun handleSignOut() {
        viewModelScope.launch {
            repository.clearSession()
        }
    }

    // Favorite locations handlers
    fun toggleFavorite(cityName: String, latitude: Double, longitude: Double, country: String?, admin1: String?) {
        viewModelScope.launch {
            val currentFavorites = favorites.value
            val isFav = currentFavorites.any { it.name.equals(cityName, ignoreCase = true) }
            if (isFav) {
                repository.removeFavoriteByName(cityName)
            } else {
                repository.addFavorite(
                    FavoriteLocation(
                        name = cityName,
                        latitude = latitude,
                        longitude = longitude,
                        country = country,
                        admin1 = admin1
                    )
                )
            }
        }
    }

    // Push local severe notification directly
    fun triggerSevereWeatherNotification(title: String, message: String) {
        val state = _weatherUiState.value
        if (state is WeatherUiState.Success) {
            _weatherUiState.value = state.copy(
                isSevereAlertActive = true,
                severeAlertTitle = title,
                severeAlertMessage = message
            )
        }

        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissLocalSevereAlert() {
        val state = _weatherUiState.value
        if (state is WeatherUiState.Success) {
            _weatherUiState.value = state.copy(
                isSevereAlertActive = false,
                severeAlertTitle = null,
                severeAlertMessage = null
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Severe Weather Alerts"
            val descriptionText = "Emergency and high-priority push alerts for extreme atmospheric and regional hazards."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
