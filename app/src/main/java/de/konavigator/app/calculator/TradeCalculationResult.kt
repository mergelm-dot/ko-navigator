package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyCode

data class TradeCalculationResult(

    val isValid: Boolean,

    val underlyingPrice: Double?,

    val targetLeverage: Double?,

    val knockoutPrice: Double?,

    val theoreticalValueInUnderlyingCurrency: Double?,

    val theoreticalProductValue: Double?,

    val underlyingExposureInProductCurrency: Double?,

    val calculatedTheoreticalLeverageAtEntry: Double?,

    val underlyingCurrency: CurrencyCode?,

    val productCurrency: CurrencyCode?,

    val distanceToKnockoutAbsolute: Double?,

    val distanceToKnockoutPercent: Double?,

    val error: TradeCalculationError? = null
)
