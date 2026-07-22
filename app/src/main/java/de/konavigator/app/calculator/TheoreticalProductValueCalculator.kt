package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyConversion

/**
 * Berechnet einen ungerundeten theoretischen Produktwert aus positivem
 * KO-Abstand, Bezugsverhältnis und typisiertem Währungskontext.
 *
 * Der Calculator ist noch nicht an die bestehende [TradeCalculationEngine]
 * angebunden. Er kennt weder Richtung, UI, Marktdaten, Repositories,
 * Kursquellen, Systemzeit noch handelbare Produktpreise.
 */
object TheoreticalProductValueCalculator {

    fun calculate(
        knockoutDistanceAbsolute: Double,
        ratio: Double,
        currencyConversion: CurrencyConversion
    ): TheoreticalProductValueCalculationResult {
        if (!knockoutDistanceAbsolute.isFinite() || knockoutDistanceAbsolute <= 0.0) {
            return failure(TheoreticalProductValueCalculationError.INVALID_KNOCKOUT_DISTANCE)
        }

        if (!ratio.isFinite() || ratio <= 0.0) {
            return failure(TheoreticalProductValueCalculationError.INVALID_RATIO)
        }

        val theoreticalValueInUnderlyingCurrency = knockoutDistanceAbsolute * ratio
        if (
            !theoreticalValueInUnderlyingCurrency.isFinite() ||
            theoreticalValueInUnderlyingCurrency <= 0.0
        ) {
            return failure(
                TheoreticalProductValueCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
            )
        }

        val theoreticalProductValue = when (currencyConversion) {
            is CurrencyConversion.SameCurrency ->
                theoreticalValueInUnderlyingCurrency

            is CurrencyConversion.CrossCurrency -> {
                val rate = currencyConversion.underlyingCurrencyPerProductCurrencyRate
                if (!rate.isFinite() || rate <= 0.0) {
                    return failure(
                        TheoreticalProductValueCalculationError.INVALID_EXCHANGE_RATE
                    )
                }
                theoreticalValueInUnderlyingCurrency / rate
            }
        }

        if (!theoreticalProductValue.isFinite() || theoreticalProductValue <= 0.0) {
            return failure(
                TheoreticalProductValueCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
            )
        }

        return TheoreticalProductValueCalculationResult.Success(
            theoreticalValueInUnderlyingCurrency = theoreticalValueInUnderlyingCurrency,
            theoreticalProductValue = theoreticalProductValue,
            underlyingCurrency = currencyConversion.underlyingCurrency,
            productCurrency = currencyConversion.productCurrency,
            ratio = ratio,
            currencyConversion = currencyConversion
        )
    }

    private fun failure(
        error: TheoreticalProductValueCalculationError
    ) = TheoreticalProductValueCalculationResult.Failure(error)
}

sealed interface TheoreticalProductValueCalculationResult {
    data class Success(
        val theoreticalValueInUnderlyingCurrency: Double,
        val theoreticalProductValue: Double,
        val underlyingCurrency: CurrencyCode,
        val productCurrency: CurrencyCode,
        val ratio: Double,
        val currencyConversion: CurrencyConversion
    ) : TheoreticalProductValueCalculationResult

    data class Failure(
        val error: TheoreticalProductValueCalculationError
    ) : TheoreticalProductValueCalculationResult
}

enum class TheoreticalProductValueCalculationError {
    INVALID_KNOCKOUT_DISTANCE,
    INVALID_RATIO,
    INVALID_EXCHANGE_RATE,
    INVALID_THEORETICAL_PRODUCT_VALUE
}
