package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyCode

/** Ergebnis der ungerundeten theoretischen Einstiegsberechnung eines bestehenden KO-Produkts. */
sealed interface ExistingKnockoutProductEntryCalculationResult {
    data class Success(
        val intrinsicValueInUnderlyingCurrency: Double,
        val theoreticalProductValue: Double,
        val knockoutDistanceAbsolute: Double,
        val knockoutDistancePercent: Double,
        val underlyingExposureInProductCurrency: Double,
        val calculatedLeverageAtEntry: Double,
        val underlyingCurrency: CurrencyCode,
        val productCurrency: CurrencyCode
    ) : ExistingKnockoutProductEntryCalculationResult

    data class Failure(
        val error: ExistingKnockoutProductEntryCalculationError
    ) : ExistingKnockoutProductEntryCalculationResult
}

enum class ExistingKnockoutProductEntryCalculationError {
    INVALID_PLANNED_ENTRY_PRICE,
    INVALID_BASE_PRICE,
    INVALID_KNOCKOUT_BARRIER,
    INVALID_RATIO,
    INVALID_EXCHANGE_RATE,
    INVALID_KNOCKOUT_DISTANCE,
    INVALID_THEORETICAL_PRODUCT_VALUE,
    INVALID_CALCULATED_LEVERAGE
}
