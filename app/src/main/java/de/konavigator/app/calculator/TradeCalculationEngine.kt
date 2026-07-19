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
 * Ist-Zustand: [calculateTrade] berechnet einzelne Formeln noch direkt und gibt
 * Meldungen als Benutzertext zurück. Diese bekannten Abweichungen bleiben bis
 * zu gesondert freigegebenen Konsolidierungsschritten bestehen.
 */
object TradeCalculationEngine {

    fun calculateTrade(
        input: TradeCalculationInput
    ): TradeCalculationResult {

        if (!input.plannedEntryPrice.isFinite() || input.plannedEntryPrice <= 0.0) {
            return invalidResult(
                input = input,
                error = TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE,
                message = "Der geplante Einstiegskurs muss endlich und größer als 0 sein."
            )
        }

        if (!input.leverage.isFinite() || input.leverage <= 1.0) {
            return invalidResult(
                input = input,
                error = TradeCalculationError.INVALID_TARGET_LEVERAGE,
                message = "Der Zielhebel muss endlich und größer als 1 sein."
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

        if (!knockoutPrice.isFinite() || knockoutPrice <= 0.0) {
            return invalidResult(
                input = input,
                error = TradeCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE,
                message = "Die berechnete theoretische KO-Barriere ist ungültig."
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

        val certificatePrice =
            KoCalculator.calculateCertificatePrice(
                underlyingPrice = input.plannedEntryPrice,
                knockoutPrice = knockoutPrice,
                ratio = input.ratio,
                isLong = input.isLong
            )

        return TradeCalculationResult(
            certificatePrice = certificatePrice,
            underlyingPrice = input.plannedEntryPrice,
            leverage = input.leverage,
            knockoutPrice = knockoutPrice,
            distanceToKnockoutAbsolute = distanceToKnockoutAbsolute,
            distanceToKnockoutPercent = distanceToKnockoutPercent,
            isValid = true,
            message = "Theoretische KO-Barriere erfolgreich berechnet.",
            error = null
        )
    }

    private fun invalidResult(
        input: TradeCalculationInput,
        error: TradeCalculationError,
        message: String
    ): TradeCalculationResult {
        return TradeCalculationResult(
            certificatePrice = 0.0,
            underlyingPrice = input.plannedEntryPrice,
            leverage = input.leverage,
            knockoutPrice = 0.0,
            distanceToKnockoutAbsolute = 0.0,
            distanceToKnockoutPercent = 0.0,
            isValid = false,
            message = message,
            error = error
        )
    }
}

