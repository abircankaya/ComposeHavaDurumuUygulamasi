package com.example.composehavadurumu.network

import com.example.composehavadurumu.data.GeocodingResponseItem
import com.example.composehavadurumu.data.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    @GET("weather")
    suspend fun getWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "tr"
    ): Response<WeatherResponse>

    @GET("weather")
    suspend fun getWeatherByCoordinates(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "tr"
    ): Response<WeatherResponse>

    @GET("https://api.openweathermap.org/geo/1.0/direct")
    suspend fun searchCities(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): Response<List<GeocodingResponseItem>>
}