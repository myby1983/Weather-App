package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "country") val country: String?,
    @Json(name = "admin1") val admin1: String?
)

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "elevation") val elevation: Double,
    @Json(name = "current") val current: WeatherCurrent?,
    @Json(name = "hourly") val hourly: WeatherHourly?,
    @Json(name = "daily") val daily: WeatherDaily?
)

@JsonClass(generateAdapter = true)
data class WeatherCurrent(
    @Json(name = "time") val time: String,
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @Json(name = "precipitation") val precipitation: Double
)

@JsonClass(generateAdapter = true)
data class WeatherHourly(
    @Json(name = "time") val time: List<String>,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>?
)

@JsonClass(generateAdapter = true)
data class WeatherDaily(
    @Json(name = "time") val time: List<String>,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>?
)

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,wind_speed_10m,precipitation",
        @Query("hourly") hourly: String = "precipitation_probability",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}

interface GeocodingApiService {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") cityName: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val weatherApi: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    val geocodingApi: GeocodingApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeocodingApiService::class.java)
    }
}
