package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.model.TradeDirection

/**
 * Reine, synchrone und zustandslose Einstiegsberechnung für ein bestehendes
 * KO-Produkt. Sie trennt den inneren Wert über den festen Basispreis strikt
 * vom KO-Abstand über die feste KO-Barriere.
 *
 * Die Komponente delegiert gerichteten Abstand und inneren Wert ausschließlich
 * an [KoCalculator] sowie den Hebel an [TheoreticalLeverageCalculator]. Sie
 * erzeugt keine Zielbarriere, verwendet keine TradeCalculationEngine, führt
 * keine Kandidaten-, Ranking-, Preisprognose-, Marktdaten-, UI- oder
 * Infrastrukturverantwortung aus und rundet nicht.
 */
object ExistingKnockoutProductEntryCalculator {

    fun calculate(
        input: ExistingKnockoutProductEntryCalculationInput
    ): ExistingKnockoutProductEntryCalculationResult {
        if (!input.plannedEntryPrice.isFinite() || input.plannedEntryPrice <= 0.0) {
            return failure(ExistingKnockoutProductEntryCalculationError.INVALID_PLANNED_ENTRY_PRICE)
        }
        if (!input.basePrice.isFinite() || input.basePrice <= 0.0) {
            return failure(ExistingKnockoutProductEntryCalculationError.INVALID_BASE_PRICE)
        }
        if (!input.knockoutBarrier.isFinite() || input.knockoutBarrier <= 0.0) {
            return failure(ExistingKnockoutProductEntryCalculationError.INVALID_KNOCKOUT_BARRIER)
        }
        if (!input.ratio.isFinite() || input.ratio <= 0.0) {
            return failure(ExistingKnockoutProductEntryCalculationError.INVALID_RATIO)
        }

        val isLong = input.direction == TradeDirection.LONG
        val knockoutDistanceAbsolute = KoCalculator.calculateKnockoutDistanceAbsolute(
            underlyingPrice = input.plannedEntryPrice,
            knockoutPrice = input.knockoutBarrier,
            isLong = isLong
        )
        val knockoutDistancePercent = KoCalculator.calculateKnockoutDistancePercent(
            underlyingPrice = input.plannedEntryPrice,
            knockoutPrice = input.knockoutBarrier,
            isLong = isLong
        )
        if (
            !knockoutDistanceAbsolute.isFinite() || knockoutDistanceAbsolute <= 0.0 ||
            !knockoutDistancePercent.isFinite() || knockoutDistancePercent <= 0.0
        ) {
            return failure(ExistingKnockoutProductEntryCalculationError.INVALID_KNOCKOUT_DISTANCE)
        }

        val intrinsicValueInUnderlyingCurrency = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = input.plannedEntryPrice,
            basePrice = input.basePrice,
            ratio = input.ratio,
            isLong = isLong
        )
        if (!intrinsicValueInUnderlyingCurrency.isFinite() || intrinsicValueInUnderlyingCurrency <= 0.0) {
            return failure(ExistingKnockoutProductEntryCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE)
        }

        val theoreticalProductValue = when (val conversion = input.currencyConversion) {
            is CurrencyConversion.SameCurrency -> intrinsicValueInUnderlyingCurrency
            is CurrencyConversion.CrossCurrency -> {
                val rate = conversion.underlyingCurrencyPerProductCurrencyRate
                if (!rate.isFinite() || rate <= 0.0) {
                    return failure(ExistingKnockoutProductEntryCalculationError.INVALID_EXCHANGE_RATE)
                }
                intrinsicValueInUnderlyingCurrency / rate
            }
        }
        if (!theoreticalProductValue.isFinite() || theoreticalProductValue <= 0.0) {
            return failure(ExistingKnockoutProductEntryCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE)
        }

        return when (
            val leverageResult = TheoreticalLeverageCalculator.calculate(
                plannedEntryPrice = input.plannedEntryPrice,
                ratio = input.ratio,
                currencyConversion = input.currencyConversion,
                theoreticalProductValue = theoreticalProductValue
            )
        ) {
            is TheoreticalLeverageCalculationResult.Success -> {
                ExistingKnockoutProductEntryCalculationResult.Success(
                    intrinsicValueInUnderlyingCurrency = intrinsicValueInUnderlyingCurrency,
                    theoreticalProductValue = theoreticalProductValue,
                    knockoutDistanceAbsolute = knockoutDistanceAbsolute,
                    knockoutDistancePercent = knockoutDistancePercent,
                    underlyingExposureInProductCurrency = leverageResult.underlyingExposureInProductCurrency,
                    calculatedLeverageAtEntry = leverageResult.calculatedTheoreticalLeverageAtEntry,
                    underlyingCurrency = input.currencyConversion.underlyingCurrency,
                    productCurrency = leverageResult.productCurrency
                )
            }

            is TheoreticalLeverageCalculationResult.Failure -> failure(
                when (leverageResult.error) {
                    TheoreticalLeverageCalculationError.INVALID_PLANNED_ENTRY_PRICE ->
                        ExistingKnockoutProductEntryCalculationError.INVALID_PLANNED_ENTRY_PRICE
                    TheoreticalLeverageCalculationError.INVALID_RATIO ->
                        ExistingKnockoutProductEntryCalculationError.INVALID_RATIO
                    TheoreticalLeverageCalculationError.INVALID_EXCHANGE_RATE ->
                        ExistingKnockoutProductEntryCalculationError.INVALID_EXCHANGE_RATE
                    TheoreticalLeverageCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE ->
                        ExistingKnockoutProductEntryCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
                    TheoreticalLeverageCalculationError.INVALID_CALCULATED_LEVERAGE ->
                        ExistingKnockoutProductEntryCalculationError.INVALID_CALCULATED_LEVERAGE
                }
            )
        }
    }

    private fun failure(
        error: ExistingKnockoutProductEntryCalculationError
    ) = ExistingKnockoutProductEntryCalculationResult.Failure(error)
}
