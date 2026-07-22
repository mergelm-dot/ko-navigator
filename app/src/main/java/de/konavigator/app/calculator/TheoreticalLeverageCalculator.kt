package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyConversion

/**
 * Berechnet den theoretischen Hebel am geplanten Einstieg aus dem
 * ungerundeten Basiswert-Exposure und dem ungerundeten Produktwert.
 *
 * Die Komponente rundet nicht und kennt weder Richtung, KO-Formeln,
 * Marktdaten, Repositories, Android, Compose noch Systemzeit.
 */
object TheoreticalLeverageCalculator {

    fun calculate(
        plannedEntryPrice: Double,
        ratio: Double,
        currencyConversion: CurrencyConversion,
        theoreticalProductValue: Double
    ): TheoreticalLeverageCalculationResult {
        if (!plannedEntryPrice.isFinite() || plannedEntryPrice <= 0.0) {
            return failure(TheoreticalLeverageCalculationError.INVALID_PLANNED_ENTRY_PRICE)
        }

        if (!ratio.isFinite() || ratio <= 0.0) {
            return failure(TheoreticalLeverageCalculationError.INVALID_RATIO)
        }

        if (!theoreticalProductValue.isFinite() || theoreticalProductValue <= 0.0) {
            return failure(
                TheoreticalLeverageCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
            )
        }

        val exposureInUnderlyingCurrency = plannedEntryPrice * ratio
        if (!exposureInUnderlyingCurrency.isFinite() || exposureInUnderlyingCurrency <= 0.0) {
            return failure(TheoreticalLeverageCalculationError.INVALID_CALCULATED_LEVERAGE)
        }

        val underlyingExposureInProductCurrency = when (currencyConversion) {
            is CurrencyConversion.SameCurrency -> exposureInUnderlyingCurrency

            is CurrencyConversion.CrossCurrency -> {
                val rate = currencyConversion.underlyingCurrencyPerProductCurrencyRate
                if (!rate.isFinite() || rate <= 0.0) {
                    return failure(TheoreticalLeverageCalculationError.INVALID_EXCHANGE_RATE)
                }
                exposureInUnderlyingCurrency / rate
            }
        }

        if (
            !underlyingExposureInProductCurrency.isFinite() ||
            underlyingExposureInProductCurrency <= 0.0
        ) {
            return failure(TheoreticalLeverageCalculationError.INVALID_CALCULATED_LEVERAGE)
        }

        val calculatedTheoreticalLeverageAtEntry =
            underlyingExposureInProductCurrency / theoreticalProductValue
        if (
            !calculatedTheoreticalLeverageAtEntry.isFinite() ||
            calculatedTheoreticalLeverageAtEntry <= 1.0
        ) {
            return failure(TheoreticalLeverageCalculationError.INVALID_CALCULATED_LEVERAGE)
        }

        return TheoreticalLeverageCalculationResult.Success(
            underlyingExposureInProductCurrency = underlyingExposureInProductCurrency,
            calculatedTheoreticalLeverageAtEntry = calculatedTheoreticalLeverageAtEntry,
            productCurrency = currencyConversion.productCurrency
        )
    }

    private fun failure(
        error: TheoreticalLeverageCalculationError
    ) = TheoreticalLeverageCalculationResult.Failure(error)
}

sealed interface TheoreticalLeverageCalculationResult {
    data class Success(
        val underlyingExposureInProductCurrency: Double,
        val calculatedTheoreticalLeverageAtEntry: Double,
        val productCurrency: CurrencyCode
    ) : TheoreticalLeverageCalculationResult

    data class Failure(
        val error: TheoreticalLeverageCalculationError
    ) : TheoreticalLeverageCalculationResult
}

enum class TheoreticalLeverageCalculationError {
    INVALID_PLANNED_ENTRY_PRICE,
    INVALID_RATIO,
    INVALID_EXCHANGE_RATE,
    INVALID_THEORETICAL_PRODUCT_VALUE,
    INVALID_CALCULATED_LEVERAGE
}
