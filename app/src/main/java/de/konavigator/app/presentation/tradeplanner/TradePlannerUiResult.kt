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
        val theoreticalProductValue: Double,
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
    INVALID_RATIO,
    INVALID_DERIVED_KNOCKOUT_PRICE,
    INVALID_EXCHANGE_RATE,
    INVALID_THEORETICAL_PRODUCT_VALUE,
    INCONSISTENT_CALCULATION_RESULT
}

internal fun TradeCalculationResult.toTradePlannerUiResult(
    relation: EntryPriceRelation
): TradePlannerUiResult {
    val underlyingPriceValue = underlyingPrice
    val knockoutPriceValue = knockoutPrice
    val theoreticalValueInUnderlyingCurrencyValue =
        theoreticalValueInUnderlyingCurrency
    val theoreticalProductValueValue = theoreticalProductValue
    val underlyingCurrencyValue = underlyingCurrency
    val productCurrencyValue = productCurrency
    val distanceToKnockoutAbsoluteValue = distanceToKnockoutAbsolute
    val distanceToKnockoutPercentValue = distanceToKnockoutPercent

    if (isValid) {
        if (
            error != null ||
            underlyingPriceValue == null ||
            knockoutPriceValue == null ||
            theoreticalValueInUnderlyingCurrencyValue == null ||
            theoreticalProductValueValue == null ||
            underlyingCurrencyValue == null ||
            productCurrencyValue == null ||
            distanceToKnockoutAbsoluteValue == null ||
            distanceToKnockoutPercentValue == null
        ) {
            return inconsistentCalculationResult()
        }

        return TradePlannerUiResult.Success(
            relation = relation,
            theoreticalProductValue = theoreticalProductValueValue,
            knockoutPrice = knockoutPriceValue,
            distanceToKnockoutAbsolute = distanceToKnockoutAbsoluteValue,
            distanceToKnockoutPercent = distanceToKnockoutPercentValue
        )
    }

    val hasAnyCalculation =
        underlyingPriceValue != null ||
            knockoutPriceValue != null ||
            theoreticalValueInUnderlyingCurrencyValue != null ||
            theoreticalProductValueValue != null ||
            underlyingCurrencyValue != null ||
            productCurrencyValue != null ||
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

    TradeCalculationError.INVALID_RATIO ->
        TradePlannerUiCalculationError.INVALID_RATIO

    TradeCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE ->
        TradePlannerUiCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE

    TradeCalculationError.INVALID_EXCHANGE_RATE ->
        TradePlannerUiCalculationError.INVALID_EXCHANGE_RATE

    TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE ->
        TradePlannerUiCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
}

private fun inconsistentCalculationResult() =
    TradePlannerUiResult.Failure(
        TradePlannerUiCalculationError.INCONSISTENT_CALCULATION_RESULT
    )
