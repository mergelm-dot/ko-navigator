package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyConversion

data class TradeCalculationInput(

    val underlyingPrice: Double,

    val plannedEntryPrice: Double,

    val targetLeverage: Double,

    val isLong: Boolean,

    val ratio: Double,

    val currencyConversion: CurrencyConversion
)
