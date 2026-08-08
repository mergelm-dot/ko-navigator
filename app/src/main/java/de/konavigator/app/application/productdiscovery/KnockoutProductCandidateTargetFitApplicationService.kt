package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitCalculator
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitInput

/**
 * Reiner, synchroner und zustandsloser Application-Service. Der injizierte
 * Target-Fit-Calculator bleibt die alleinige Formel- und Validierungsquelle.
 * Dieser Service filtert, bewertet, rankt oder waehlt keine Kandidaten aus.
 */
class KnockoutProductCandidateTargetFitApplicationService(
    private val existingKnockoutProductTargetFitCalculator:
        ExistingKnockoutProductTargetFitCalculator
) {

    fun execute(
        request: KnockoutProductCandidateTargetFitRequest
    ): KnockoutProductCandidateTargetFitResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateTargetFitResult.NoInputCandidates
        }

        val candidates = request.candidates.map { candidate ->
            val targetDeviation = candidate.targetDeviationResult
            require(targetDeviation is ExistingKnockoutProductTargetDeviationResult.Success)

            val targetFitResult = existingKnockoutProductTargetFitCalculator.calculate(
                ExistingKnockoutProductTargetFitInput(
                    targetDeviation = targetDeviation,
                    maxRelativeLeverageDeviationPercent =
                        request.maxRelativeLeverageDeviationPercent,
                    maxBarrierDeviationPercentOfPlannedEntry =
                        request.maxBarrierDeviationPercentOfPlannedEntry
                )
            )

            KnockoutProductCandidateWithTargetFit(candidate, targetFitResult)
        }

        return KnockoutProductCandidateTargetFitResult.CandidatesWithTargetFit(candidates)
    }
}
