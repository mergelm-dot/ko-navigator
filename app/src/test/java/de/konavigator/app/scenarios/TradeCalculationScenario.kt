package de.konavigator.app.scenarios

import de.konavigator.app.calculator.TradeCalculationError
import de.konavigator.app.calculator.TradeCalculationInput
import de.konavigator.app.domain.currency.CurrencyCode

sealed interface TradeCalculationExpectation {

    data class Success(
        val expectedUnderlyingPrice: Double,
        val expectedKnockoutPrice: Double,
        val expectedTargetLeverage: Double,
        val expectedTheoreticalValueInUnderlyingCurrency: Double,
        val expectedTheoreticalProductValue: Double,
        val expectedUnderlyingExposureInProductCurrency: Double,
        val expectedCalculatedTheoreticalLeverageAtEntry: Double,
        val expectedUnderlyingCurrency: CurrencyCode,
        val expectedProductCurrency: CurrencyCode,
        val expectedDistanceToKnockoutAbsolute: Double,
        val expectedDistanceToKnockoutPercent: Double
    ) : TradeCalculationExpectation

    data class Failure(
        val expectedError: TradeCalculationError
    ) : TradeCalculationExpectation
}

data class TradeCalculationScenario(
    val name: String,
    val group: String,
    val input: TradeCalculationInput,
    val expectation: TradeCalculationExpectation
)
