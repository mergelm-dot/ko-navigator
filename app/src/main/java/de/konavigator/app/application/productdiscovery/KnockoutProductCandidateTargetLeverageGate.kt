package de.konavigator.app.application.productdiscovery

/**
 * Reines, synchrones, zustandsloses und providerneutrales Gate. Es
 * partitioniert ausschließlich anhand von `tradeCalculationResult.isValid`.
 * Fehler und nullable Ergebnisfelder werden weder gelesen noch korrigiert.
 *
 * Das Gate ruft keine Engine oder Calculator-Komponente auf, führt keine
 * Berechnung oder Validierung erneut aus und mutiert keine Eingaben. Es enthält
 * keinen Cache, keine Sortierung, kein Ranking und keine UI-Logik.
 */
class KnockoutProductCandidateTargetLeverageGate {

    fun filter(
        request: KnockoutProductCandidateTargetLeverageGateRequest
    ): KnockoutProductCandidateTargetLeverageGateResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateTargetLeverageGateResult.NoInputCandidates
        }

        val validCandidates =
            ArrayList<KnockoutProductCandidateWithTargetLeveragePlan>(request.candidates.size)
        val invalidCandidates = ArrayList<KnockoutProductCandidateWithTargetLeveragePlan>()

        request.candidates.forEach { candidate ->
            if (candidate.tradeCalculationResult.isValid) {
                validCandidates += candidate
            } else {
                invalidCandidates += candidate
            }
        }

        return if (validCandidates.isEmpty()) {
            KnockoutProductCandidateTargetLeverageGateResult.NoValidTargetLeveragePlanCandidates(
                invalidCandidates = invalidCandidates
            )
        } else {
            KnockoutProductCandidateTargetLeverageGateResult.ValidTargetLeveragePlanCandidates(
                validCandidates = validCandidates,
                invalidCandidates = invalidCandidates
            )
        }
    }
}
