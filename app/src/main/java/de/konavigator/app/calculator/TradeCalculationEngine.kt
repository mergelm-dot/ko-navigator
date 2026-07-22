package de.konavigator.app.calculator

/**
 * Zentraler fachlicher Einstiegspunkt für vollständige Trade-Berechnungen.
 *
 * Die Zielverantwortung umfasst die Entgegennahme und fachliche Validierung
 * zusammengehöriger Eingaben, die Festlegung der notwendigen Rechenschritte,
 * den Aufruf reiner Calculator-Komponenten sowie das Zusammenführen von
 * Ergebnissen, Status, Warnungen und strukturierten Fehlern.
 *
 * Die Engine enthält langfristig keine eigenen mathematischen Fachformeln und
 * übernimmt weder UI-State, Anzeigeformatierung, Benutzertexte noch Repository-,
 * Netzwerk-, Android- oder Compose-Verantwortung.
 *
 * [calculateTrade] validiert und orchestriert den aktiven theoretischen
 * Planungsvertrag. FX- und Ratio-Mathematik werden ausschließlich an den
 * [TheoreticalProductValueCalculator] delegiert; der Engine-Kern rundet nicht.
 */
object TradeCalculationEngine {

    fun calculateTrade(
        input: TradeCalculationInput
    ): TradeCalculationResult {

        if (!input.plannedEntryPrice.isFinite() || input.plannedEntryPrice <= 0.0) {
            return invalidResult(
                TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE
            )
        }

        if (!input.targetLeverage.isFinite() || input.targetLeverage <= 1.0) {
            return invalidResult(
                TradeCalculationError.INVALID_TARGET_LEVERAGE
            )
        }

        if (!input.ratio.isFinite() || input.ratio <= 0.0) {
            return invalidResult(
                TradeCalculationError.INVALID_RATIO
            )
        }

        val knockoutPrice =
            if (input.isLong) {
                input.plannedEntryPrice *
                        (1.0 - 1.0 / input.targetLeverage)
            } else {
                input.plannedEntryPrice *
                        (1.0 + 1.0 / input.targetLeverage)
            }

        if (!knockoutPrice.isFinite() || knockoutPrice <= 0.0) {
            return invalidResult(
                TradeCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE
            )
        }

        val distanceToKnockoutAbsolute =
            KoCalculator.calculateKnockoutDistanceAbsolute(
                underlyingPrice = input.plannedEntryPrice,
                knockoutPrice = knockoutPrice,
                isLong = input.isLong
            )

        val distanceToKnockoutPercent =
            KoCalculator.calculateKnockoutDistancePercent(
                underlyingPrice = input.plannedEntryPrice,
                knockoutPrice = knockoutPrice,
                isLong = input.isLong
            )

        if (
            !distanceToKnockoutAbsolute.isFinite() ||
            distanceToKnockoutAbsolute <= 0.0 ||
            !distanceToKnockoutPercent.isFinite() ||
            distanceToKnockoutPercent <= 0.0
        ) {
            return invalidResult(
                TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
            )
        }

        val productValueResult =
            TheoreticalProductValueCalculator.calculate(
                knockoutDistanceAbsolute = distanceToKnockoutAbsolute,
                ratio = input.ratio,
                currencyConversion = input.currencyConversion
            )

        return when (productValueResult) {
            is TheoreticalProductValueCalculationResult.Success -> {
                val leverageResult = TheoreticalLeverageCalculator.calculate(
                    plannedEntryPrice = input.plannedEntryPrice,
                    ratio = input.ratio,
                    currencyConversion = input.currencyConversion,
                    theoreticalProductValue = productValueResult.theoreticalProductValue
                )

                when (leverageResult) {
                    is TheoreticalLeverageCalculationResult.Success -> TradeCalculationResult(
                        isValid = true,
                        underlyingPrice = input.plannedEntryPrice,
                        targetLeverage = input.targetLeverage,
                        knockoutPrice = knockoutPrice,
                        theoreticalValueInUnderlyingCurrency =
                            productValueResult.theoreticalValueInUnderlyingCurrency,
                        theoreticalProductValue =
                            productValueResult.theoreticalProductValue,
                        underlyingExposureInProductCurrency =
                            leverageResult.underlyingExposureInProductCurrency,
                        calculatedTheoreticalLeverageAtEntry =
                            leverageResult.calculatedTheoreticalLeverageAtEntry,
                        underlyingCurrency = productValueResult.underlyingCurrency,
                        productCurrency = leverageResult.productCurrency,
                        distanceToKnockoutAbsolute = distanceToKnockoutAbsolute,
                        distanceToKnockoutPercent = distanceToKnockoutPercent,
                        error = null
                    )

                    is TheoreticalLeverageCalculationResult.Failure ->
                        invalidResult(leverageResult.error.toTradeCalculationError())
                }
            }

            is TheoreticalProductValueCalculationResult.Failure ->
                invalidResult(productValueResult.error.toTradeCalculationError())
        }
    }

    private fun invalidResult(
        error: TradeCalculationError
    ): TradeCalculationResult {
        return TradeCalculationResult(
            isValid = false,
            underlyingPrice = null,
            targetLeverage = null,
            knockoutPrice = null,
            theoreticalValueInUnderlyingCurrency = null,
            theoreticalProductValue = null,
            underlyingExposureInProductCurrency = null,
            calculatedTheoreticalLeverageAtEntry = null,
            underlyingCurrency = null,
            productCurrency = null,
            distanceToKnockoutAbsolute = null,
            distanceToKnockoutPercent = null,
            error = error
        )
    }

    private fun TheoreticalProductValueCalculationError.toTradeCalculationError():
        TradeCalculationError = when (this) {
        TheoreticalProductValueCalculationError.INVALID_KNOCKOUT_DISTANCE,
        TheoreticalProductValueCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE ->
            TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE

        TheoreticalProductValueCalculationError.INVALID_RATIO ->
            TradeCalculationError.INVALID_RATIO

        TheoreticalProductValueCalculationError.INVALID_EXCHANGE_RATE ->
            TradeCalculationError.INVALID_EXCHANGE_RATE
    }

    private fun TheoreticalLeverageCalculationError.toTradeCalculationError():
        TradeCalculationError = when (this) {
        TheoreticalLeverageCalculationError.INVALID_PLANNED_ENTRY_PRICE ->
            TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE

        TheoreticalLeverageCalculationError.INVALID_RATIO ->
            TradeCalculationError.INVALID_RATIO

        TheoreticalLeverageCalculationError.INVALID_EXCHANGE_RATE ->
            TradeCalculationError.INVALID_EXCHANGE_RATE

        TheoreticalLeverageCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE ->
            TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE

        TheoreticalLeverageCalculationError.INVALID_CALCULATED_LEVERAGE ->
            TradeCalculationError.INVALID_CALCULATED_LEVERAGE
    }
}

