package de.konavigator.app.models

data class UnderlyingAsset(
    val id: String,
    val name: String,
    val ticker: String,
    val wkn: String?,
    val isin: String?,
    val referenceExchange: String,
    val currency: String,
    val currentPrice: Double?,
    val priceTimestamp: Long?
)