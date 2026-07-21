package de.konavigator.app.presentation.tradeplanner

import de.konavigator.app.calculator.TradeCalculationError
import de.konavigator.app.calculator.TradeCalculationResult
import de.konavigator.app.domain.tradeplanning.EntryPriceRelation

/**
 * Typisiertes Presentation-Ergebnis mit rohen, ungerundeten Rechenwerten.
 *
 * Der Vertrag übernimmt keine Domain-Freitexte, Throwables oder nullable
 * Berechnungsfelder in ein erfolgreiches Ergebnis.
 */
sealed interface TradePlannerUiResult {

    data class Success(
        val relation: EntryPriceRelation,
        val certificatePrice: Double,
        val knockoutPrice: Double,
        val distanceToKnockoutAbsolute: Double,
        val distanceToKnockoutPercent: Double
    ) : TradePlannerUiResult

    data class Failure(
        val error: TradePlannerUiCalculationError
    ) : TradePlannerUiResult
}

enum class TradePlannerUiCalculationError {
    INVALID_PLANNED_ENTRY_PRICE,
    INVALID_TARGET_LEVERAGE,
    INVALID_DERIVED_KNOCKOUT_PRICE,
    INCONSISTENT_CALCULATION_RESULT
}

internal fun TradeCalculationResult.toTradePlannerUiResult(
    relation: EntryPriceRelation
): TradePlannerUiResult {
    val certificatePriceValue = certificatePrice
    val knockoutPriceValue = knockoutPrice
    val distanceToKnockoutAbsoluteValue = distanceToKnockoutAbsolute
    val distanceToKnockoutPercentValue = distanceToKnockoutPercent

    if (isValid) {
        if (
            error != null ||
            certificatePriceValue == null ||
            knockoutPriceValue == null ||
            distanceToKnockoutAbsoluteValue == null ||
            distanceToKnockoutPercentValue == null
        ) {
            return inconsistentCalculationResult()
        }

        return TradePlannerUiResult.Success(
            relation = relation,
            certificatePrice = certificatePriceValue,
            knockoutPrice = knockoutPriceValue,
            distanceToKnockoutAbsolute = distanceToKnockoutAbsoluteValue,
            distanceToKnockoutPercent = distanceToKnockoutPercentValue
        )
    }

    val hasAnyCalculation =
        certificatePriceValue != null ||
            knockoutPriceValue != null ||
            distanceToKnockoutAbsoluteValue != null ||
            distanceToKnockoutPercentValue != null
    if (error == null || hasAnyCalculation) {
        return inconsistentCalculationResult()
    }

    return TradePlannerUiResult.Failure(error.toTradePlannerUiCalculationError())
}

private fun TradeCalculationError.toTradePlannerUiCalculationError():
    TradePlannerUiCalculationError = when (this) {
    TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE ->
        TradePlannerUiCalculationError.INVALID_PLANNED_ENTRY_PRICE

    TradeCalculationError.INVALID_TARGET_LEVERAGE ->
        TradePlannerUiCalculationError.INVALID_TARGET_LEVERAGE

    TradeCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE ->
        TradePlannerUiCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE
}

private fun inconsistentCalculationResult() =
    TradePlannerUiResult.Failure(
        TradePlannerUiCalculationError.INCONSISTENT_CALCULATION_RESULT
    )
