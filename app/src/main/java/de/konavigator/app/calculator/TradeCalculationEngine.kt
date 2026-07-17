package de.konavigator.app.calculator

import kotlin.math.abs

object TradeCalculationEngine {

    fun calculateTrade(
        input: TradeCalculationInput
    ): TradeCalculationResult {

        if (input.plannedEntryPrice <= 0.0) {
            return invalidResult(
                input = input,
                message = "Der geplante Einstiegskurs muss größer als 0 sein."
            )
        }

        if (input.leverage <= 1.0) {
            return invalidResult(
                input = input,
                message = "Der Hebel muss größer als 1 sein."
            )
        }

        val knockoutPrice =
            if (input.isLong) {
                input.plannedEntryPrice *
                        (1.0 - 1.0 / input.leverage)
            } else {
                input.plannedEntryPrice *
                        (1.0 + 1.0 / input.leverage)
            }

        val distanceToKnockoutPercent =
            abs(
                (input.plannedEntryPrice - knockoutPrice) /
                        input.plannedEntryPrice
            ) * 100.0

        val certificatePrice =
            KoCalculator.calculateCertificatePrice(
                underlyingPrice = input.plannedEntryPrice,
                knockoutPrice = knockoutPrice,
                ratio = input.ratio
            )

        return TradeCalculationResult(
            certificatePrice = certificatePrice,
            underlyingPrice = input.plannedEntryPrice,
            leverage = input.leverage,
            knockoutPrice = knockoutPrice,
            distanceToKnockoutPercent = distanceToKnockoutPercent,
            isValid = true,
            message = "Theoretische KO-Barriere erfolgreich berechnet."
        )
    }

    private fun invalidResult(
        input: TradeCalculationInput,
        message: String
    ): TradeCalculationResult {
        return TradeCalculationResult(
            certificatePrice = 0.0,
            underlyingPrice = input.plannedEntryPrice,
            leverage = input.leverage,
            knockoutPrice = 0.0,
            distanceToKnockoutPercent = 0.0,
            isValid = false,
            message = message
        )
    }
}

