package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult

/**
 * Reiner, synchroner und zustandsloser Ranker fuer bereits freigegebene
 * Target-Fit-Kandidaten. Er verwendet nur die beiden vorhandenen
 * Target-Deviation-Prozentwerte und trifft keine Auswahlentscheidung.
 */
class KnockoutProductCandidateTargetFitRanker {

    fun rank(
        request: KnockoutProductCandidateTargetFitRankingRequest
    ): KnockoutProductCandidateTargetFitRankingResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateTargetFitRankingResult.NoInputCandidates
        }

        val rankedCandidates = request.candidates.sortedWith { first, second ->
            val firstDeviation = targetDeviation(first)
            val secondDeviation = targetDeviation(second)
            val leverageComparison = firstDeviation.relativeLeverageDeviationPercent
                .compareTo(secondDeviation.relativeLeverageDeviationPercent)
            if (leverageComparison != 0) {
                leverageComparison
            } else {
                firstDeviation.barrierDeviationPercentOfPlannedEntry
                    .compareTo(secondDeviation.barrierDeviationPercentOfPlannedEntry)
            }
        }

        return KnockoutProductCandidateTargetFitRankingResult.RankedCandidates(
            rankedCandidates
        )
    }

    private fun targetDeviation(
        candidate: KnockoutProductCandidateWithTargetFit
    ): ExistingKnockoutProductTargetDeviationResult.Success {
        val targetDeviation = candidate.candidateWithTargetDeviation.targetDeviationResult
        require(targetDeviation is ExistingKnockoutProductTargetDeviationResult.Success)
        return targetDeviation
    }
}
