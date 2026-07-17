
package de.konavigator.app.models

import androidx.annotation.DrawableRes

data class UnderlyingAsset(
    val id: String,
    val displayName: String,
    val name: String,
    val ticker: String,
    val wkn: String?,
    val isin: String?,
    val referenceExchange: String,
    val currency: String,
    val currentPrice: Double?,
    val priceTimestamp: Long?,
    @DrawableRes val logoResId: Int?
)
