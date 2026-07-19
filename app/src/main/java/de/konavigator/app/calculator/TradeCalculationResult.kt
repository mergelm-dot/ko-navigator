package de.konavigator.app.calculator

data class TradeCalculationResult(

    val certificatePrice: Double,

    val underlyingPrice: Double,

    val leverage: Double,

    val knockoutPrice: Double,

    val distanceToKnockoutAbsolute: Double,

    val distanceToKnockoutPercent: Double,

    val isValid: Boolean,

    val message: String = "",

    val error: TradeCalculationError? = null
)
