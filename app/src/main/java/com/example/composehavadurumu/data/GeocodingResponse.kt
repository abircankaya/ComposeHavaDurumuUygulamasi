package com.example.composehavadurumu.data

import com.google.gson.annotations.SerializedName

data class GeocodingResponseItem(
    @SerializedName("name") val name: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("country") val country: String,
    @SerializedName("state") val state: String?
)