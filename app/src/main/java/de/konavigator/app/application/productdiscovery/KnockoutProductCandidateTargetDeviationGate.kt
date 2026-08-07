package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult

/**
 * Reines, synchrones, zustandsloses und providerneutrales Gate. Es
 * klassifiziert ausschliesslich den versiegelten Deviation-Ergebnistyp, ohne
 * Werte oder Fehler zu interpretieren, erneut zu validieren oder zu berechnen.
 */
class KnockoutProductCandidateTargetDeviationGate {

    fun filter(
        request: KnockoutProductCandidateTargetDeviationGateRequest
    ): KnockoutProductCandidateTargetDeviationGateResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateTargetDeviationGateResult.NoInputCandidates
        }

        val successfulCandidates = ArrayList<KnockoutProductCandidateWithTargetDeviation>(
            request.candidates.size
        )
        val failedCandidates = ArrayList<KnockoutProductCandidateWithTargetDeviation>()

        request.candidates.forEach { candidate ->
            when (candidate.targetDeviationResult) {
                is ExistingKnockoutProductTargetDeviationResult.Success ->
                    successfulCandidates += candidate

                is ExistingKnockoutProductTargetDeviationResult.Failure ->
                    failedCandidates += candidate
            }
        }

        return if (successfulCandidates.isEmpty()) {
            KnockoutProductCandidateTargetDeviationGateResult
                .NoSuccessfulTargetDeviationCandidates(failedCandidates)
        } else {
            KnockoutProductCandidateTargetDeviationGateResult
                .SuccessfulTargetDeviationCandidates(successfulCandidates, failedCandidates)
        }
    }
}
