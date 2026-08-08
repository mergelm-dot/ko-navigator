package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitResult

/**
 * Reines, synchrones, zustandsloses und providerneutrales Gate. Es verwendet
 * bei Success ausschliesslich den bereits berechneten Wert
 * [ExistingKnockoutProductTargetFitResult.Success.withinAllTargetTolerances].
 */
class KnockoutProductCandidateTargetFitGate {

    fun filter(
        request: KnockoutProductCandidateTargetFitGateRequest
    ): KnockoutProductCandidateTargetFitGateResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateTargetFitGateResult.NoInputCandidates
        }

        val matchingCandidates = ArrayList<KnockoutProductCandidateWithTargetFit>(
            request.candidates.size
        )
        val nonMatchingCandidates = ArrayList<KnockoutProductCandidateWithTargetFit>()
        val failedCandidates = ArrayList<KnockoutProductCandidateWithTargetFit>()

        request.candidates.forEach { candidate ->
            when (val targetFitResult = candidate.targetFitResult) {
                is ExistingKnockoutProductTargetFitResult.Failure ->
                    failedCandidates += candidate

                is ExistingKnockoutProductTargetFitResult.Success -> {
                    if (targetFitResult.withinAllTargetTolerances) {
                        matchingCandidates += candidate
                    } else {
                        nonMatchingCandidates += candidate
                    }
                }
            }
        }

        return if (matchingCandidates.isEmpty()) {
            KnockoutProductCandidateTargetFitGateResult
                .NoCandidatesWithinTargetTolerances(
                    nonMatchingCandidates,
                    failedCandidates
                )
        } else {
            KnockoutProductCandidateTargetFitGateResult
                .CandidatesWithinTargetTolerances(
                    matchingCandidates,
                    nonMatchingCandidates,
                    failedCandidates
                )
        }
    }
}
