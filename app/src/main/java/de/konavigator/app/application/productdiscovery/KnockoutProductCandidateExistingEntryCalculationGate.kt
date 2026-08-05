package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult

/**
 * Reines, synchrones, zustandsloses und providerneutrales Gate. Es
 * klassifiziert ausschliesslich den versiegelten CalculationResult-Typ, ohne
 * Success-Werte oder Failure-Fehler zu lesen, ohne Berechnung oder Validierung
 * zu wiederholen und ohne Auswahl-, Ranking- oder UI-Verantwortung.
 */
class KnockoutProductCandidateExistingEntryCalculationGate {

    fun filter(
        request: KnockoutProductCandidateExistingEntryCalculationGateRequest
    ): KnockoutProductCandidateExistingEntryCalculationGateResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateExistingEntryCalculationGateResult.NoInputCandidates
        }

        val successfulCandidates = ArrayList<KnockoutProductCandidateWithExistingEntryCalculation>(
            request.candidates.size
        )
        val failedCandidates = ArrayList<KnockoutProductCandidateWithExistingEntryCalculation>()

        request.candidates.forEach { candidate ->
            when (candidate.existingEntryCalculationResult) {
                is ExistingKnockoutProductEntryCalculationResult.Success ->
                    successfulCandidates += candidate

                is ExistingKnockoutProductEntryCalculationResult.Failure ->
                    failedCandidates += candidate
            }
        }

        return if (successfulCandidates.isEmpty()) {
            KnockoutProductCandidateExistingEntryCalculationGateResult
                .NoSuccessfulExistingEntryCalculationCandidates(failedCandidates)
        } else {
            KnockoutProductCandidateExistingEntryCalculationGateResult
                .SuccessfulExistingEntryCalculationCandidates(successfulCandidates, failedCandidates)
        }
    }
}
