package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationCalculator
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationInput

/**
 * Reiner, synchroner und zustandsloser Application-Service. Der injizierte
 * [ExistingKnockoutProductTargetDeviationCalculator] bleibt die alleinige
 * Formel- und Validierungsquelle. Vorgelagerte erfolgreiche Gates sind eine
 * Invariante; der Service bewertet, filtert, rankt oder waehlt nicht aus.
 */
class KnockoutProductCandidateTargetDeviationApplicationService(
    private val existingKnockoutProductTargetDeviationCalculator:
        ExistingKnockoutProductTargetDeviationCalculator
) {

    fun execute(
        request: KnockoutProductCandidateTargetDeviationRequest
    ): KnockoutProductCandidateTargetDeviationResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateTargetDeviationResult.NoInputCandidates
        }

        val candidates = request.candidates.map { candidate ->
            val targetPlan = candidate.candidateWithTargetLeveragePlan
            val tradeCalculationResult = targetPlan.tradeCalculationResult
            require(tradeCalculationResult.isValid)
            val existingEntryCalculationResult = candidate.existingEntryCalculationResult
            require(existingEntryCalculationResult is ExistingKnockoutProductEntryCalculationResult.Success)
            val specification = targetPlan.input
                .candidateWithCalculation
                .candidateWithSourceEvaluation
                .candidateWithFreshness
                .candidateWithCalculationAvailability
                .candidateWithDataQuality
                .candidateWithMarketData
                .specificationSnapshot
                .specification

            val targetDeviationResult = existingKnockoutProductTargetDeviationCalculator.calculate(
                ExistingKnockoutProductTargetDeviationInput(
                    plannedEntryPrice = request.plannedEntryPrice,
                    targetLeverage = requireNotNull(tradeCalculationResult.targetLeverage),
                    actualLeverageAtEntry = existingEntryCalculationResult.calculatedLeverageAtEntry,
                    targetKnockoutBarrier = requireNotNull(tradeCalculationResult.knockoutPrice),
                    actualKnockoutBarrier = specification.knockoutBarrier
                )
            )

            KnockoutProductCandidateWithTargetDeviation(candidate, targetDeviationResult)
        }

        return KnockoutProductCandidateTargetDeviationResult.CandidatesWithTargetDeviation(candidates)
    }
}
