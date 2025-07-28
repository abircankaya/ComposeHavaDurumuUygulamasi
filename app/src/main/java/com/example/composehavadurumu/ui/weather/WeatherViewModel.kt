package com.example.composehavadurumu.ui.weather

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composehavadurumu.data.GeocodingResponseItem
import com.example.composehavadurumu.data.WeatherResponse
import com.example.composehavadurumu.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    data object Idle : WeatherUiState
    data object Loading : WeatherUiState
    data class Success(val data: WeatherResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<GeocodingResponseItem>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResponseItem>> = _citySuggestions.asStateFlow()

    private var searchJob: Job? = null
    private val apiKey = "311f1a36ebdfbf38f4d170a16e2f466b"

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        Log.d("CitySearchDebug", "1. Fonksiyon tetiklendi. Sorgu: '$query'")

        if (query.length < 3) {
            _citySuggestions.value = emptyList()
            Log.d("CitySearchDebug", "2. Sorgu çok kısa, işlem durduruldu ve öneriler temizlendi.")
            return
        }

        searchJob = viewModelScope.launch {
            Log.d("CitySearchDebug", "3. Yeni coroutine başlatıldı. 500ms bekleniyor (Debounce)...")
            delay(500L)

            Log.d("CitySearchDebug", "4. Bekleme bitti. API isteği gönderiliyor: '$query'")
            try {
                val response = RetrofitClient.api.searchCities(query = query, apiKey = apiKey)

                if (response.isSuccessful && response.body() != null) {
                    val suggestions = response.body()!!
                    _citySuggestions.value = suggestions
                    Log.d("CitySearchDebug", "5. BAŞARILI: API'den cevap geldi. Bulunan öneri sayısı: ${suggestions.size}")
                } else {
                    _citySuggestions.value = emptyList()
                    Log.e("CitySearchDebug", "5. BAŞARISIZ: API isteği başarılı değil! Kod: ${response.code()}, Mesaj: ${response.message()}")
                }
            } catch (e: Exception) {
                _citySuggestions.value = emptyList()
                Log.e("CitySearchDebug", "5. KRİTİK HATA: API isteği sırasında bir Exception oluştu!", e)
            }
        }
    }

    fun clearSuggestions() {
        _citySuggestions.value = emptyList()
    }

    fun getWeatherByCity(city: String) {
        clearSuggestions()
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = RetrofitClient.api.getWeatherByCity(city = city, apiKey = apiKey)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = WeatherUiState.Success(response.body()!!)
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Geçersiz API Anahtarı."
                        404 -> "'$city' şehri bulunamadı."
                        else -> "Bir hata oluştu: ${response.code()}"
                    }
                    _uiState.value = WeatherUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Ağ bağlantısı hatası: ${e.message}")
            }
        }
    }

    fun getWeatherByLocation(latitude: Double, longitude: Double) {
        clearSuggestions()
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = RetrofitClient.api.getWeatherByCoordinates(latitude, longitude, apiKey)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = WeatherUiState.Success(response.body()!!)
                } else {
                    _uiState.value = WeatherUiState.Error("Konumla hava durumu alınamadı: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Ağ bağlantısı hatası: ${e.message}")
            }
        }
    }
}