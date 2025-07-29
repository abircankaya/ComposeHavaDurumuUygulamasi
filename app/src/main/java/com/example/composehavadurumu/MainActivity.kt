package com.example.composehavadurumu

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.composehavadurumu.data.*
import com.example.composehavadurumu.ui.theme.ComposeHavaDurumuTheme
import com.example.composehavadurumu.ui.weather.WeatherUiState
import com.example.composehavadurumu.ui.weather.WeatherViewModel
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class) // ExposedDropdownMenuBox için gerekli
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeHavaDurumuTheme {
                val uiState by viewModel.uiState.collectAsState()
                val citySuggestions by viewModel.citySuggestions.collectAsState()
                var city by remember { mutableStateOf("") }
                var expanded by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted: Boolean ->
                        if (isGranted) {
                            getCurrentLocation(fusedLocationClient, { lat, lon -> viewModel.getWeatherByLocation(lat, lon) }, {})
                        } else {
                            Toast.makeText(context, "Konum izni olmadan bu özellik kullanılamaz.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                expanded = citySuggestions.isNotEmpty() && city.isNotBlank()

                WeatherScreen(
                    uiState = uiState,
                    citySuggestions = citySuggestions,
                    cityName = city,
                    onCityNameChange = { newName ->
                        city = newName
                        viewModel.onSearchQueryChanged(newName)
                    },
                    onSearchClick = {
                        if (city.isNotBlank()) {
                            viewModel.getWeatherByCity(city)
                            expanded = false
                        }
                    },
                    onSuggestionClick = { selectedCity ->
                        city = selectedCity.name
                        viewModel.clearSuggestions()
                        viewModel.getWeatherByLocation(selectedCity.lat, selectedCity.lon)
                        expanded = false
                    },
                    onLocationClick = {
                        expanded = false
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                                getCurrentLocation(fusedLocationClient, { lat, lon -> viewModel.getWeatherByLocation(lat, lon) }, {})
                            }
                            else -> {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    },
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    uiState: WeatherUiState,
    citySuggestions: List<GeocodingResponseItem>,
    cityName: String,
    onCityNameChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onLocationClick: () -> Unit,
    onSuggestionClick: (GeocodingResponseItem) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF87CEEB), Color(0xFFB0E0E6))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = onExpandedChange
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cityName,
                        onValueChange = onCityNameChange,
                        label = { Text("Şehir ara...") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSearchClick) {
                        Text("Ara")
                    }
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    citySuggestions.forEach { suggestion ->
                        val suggestionText = "${suggestion.name}, ${suggestion.country}"
                        DropdownMenuItem(
                            text = { Text(suggestionText) },
                            onClick = { onSuggestionClick(suggestion) }
                        )
                    }
                }
            }

            IconButton(onClick = onLocationClick) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Konumumu Kullan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is WeatherUiState.Idle -> Text("Lütfen bir şehir arayın veya konumunuzu kullanın.", color = Color.White, fontSize = 18.sp)
                is WeatherUiState.Loading -> CircularProgressIndicator(color = Color.White)
                is WeatherUiState.Success -> WeatherDetails(data = state.data)
                is WeatherUiState.Error -> Text(text = state.message, color = Color.Yellow, fontSize = 16.sp)
            }
        }
    }
}

private fun getCurrentLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationFetched: (Double, Double) -> Unit,
    onLocationFetchFailed: () -> Unit
) {
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationFetched(location.latitude, location.longitude)
                } else {
                    onLocationFetchFailed()
                }
            }
            .addOnFailureListener {
                onLocationFetchFailed()
            }
    } catch (e: SecurityException) {
        onLocationFetchFailed()
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun WeatherDetails(data: WeatherResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(data.name, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            val iconCode = data.weather.firstOrNull()?.icon
            if (iconCode != null) {
                AsyncImage(
                    model = "https://openweathermap.org/img/wn/${iconCode}@2x.png",
                    contentDescription = "Hava Durumu İkonu",
                    modifier = Modifier.size(120.dp)
                )
            }

            Text(text = "${String.format("%.1f", data.main.temp)}°C", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color.White)
            val description = data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Bilgi yok"
            Text(description, fontSize = 24.sp, color = Color.White)
            Text(text = "Hissedilen: ${String.format("%.1f", data.main.feelsLike)}°C", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.WaterDrop, contentDescription = "Nem İkonu", modifier = Modifier.size(32.dp), tint = Color.White)
                    Text(text = "%${data.main.humidity}", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Nem", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Air, contentDescription = "Rüzgar İkonu", modifier = Modifier.size(32.dp), tint = Color.White)
                    Text(text = "${String.format("%.1f", data.wind.speed * 3.6)} km/h", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Rüzgar", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Başarılı Durum Önizlemesi", showBackground = true)
@Composable
fun WeatherScreenSuccessPreview() {
    val fakeWeatherData = WeatherResponse(
        weather = listOf(Weather(description = "Güneşli", icon = "01d")),
        main = Main(temp = 25.0, feelsLike = 25.0, humidity = 50),
        wind = Wind(speed = 5.0),
        name = "Ankara (Önizleme)"
    )
    ComposeHavaDurumuTheme {
        WeatherScreen(
            uiState = WeatherUiState.Success(fakeWeatherData),
            citySuggestions = emptyList(),
            cityName = "Ankara",
            onCityNameChange = {},
            onSearchClick = {},
            onLocationClick = {},
            onSuggestionClick = {},
            expanded = false,
            onExpandedChange = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Yükleniyor Durumu Önizlemesi", showBackground = true)
@Composable
fun WeatherScreenLoadingPreview() {
    ComposeHavaDurumuTheme {
        WeatherScreen(
            uiState = WeatherUiState.Loading,
            citySuggestions = emptyList(),
            cityName = "",
            onCityNameChange = {},
            onSearchClick = {},
            onLocationClick = {},
            onSuggestionClick = {},
            expanded = false,
            onExpandedChange = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Hata Durumu Önizlemesi", showBackground = true)
@Composable
fun WeatherScreenErrorPreview() {
    ComposeHavaDurumuTheme {
        WeatherScreen(
            uiState = WeatherUiState.Error("Şehir bulunamadı."),
            citySuggestions = emptyList(),
            cityName = "Hatalı Şehir",
            onCityNameChange = {},
            onSearchClick = {},
            onLocationClick = {},
            onSuggestionClick = {},
            expanded = false,
            onExpandedChange = {}
        )
    }
}