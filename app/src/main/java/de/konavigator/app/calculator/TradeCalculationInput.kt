package de.konavigator.app.calculator

data class TradeCalculationInput(

    val underlyingPrice: Double,

    val plannedEntryPrice: Double,

    val leverage: Double,

    val isLong: Boolean,

    val exchangeRate: Double = 1.0,

    val ratio: Double = 0.01
)