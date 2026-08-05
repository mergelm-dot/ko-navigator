package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationInput
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculator

/**
 * Reiner, synchroner und zustandsloser Application-Service. Er delegiert pro
 * Kandidateneintrag genau einmal an [ExistingKnockoutProductEntryCalculator],
 * die alleinige Formel- und Validierungsquelle. Es gibt keine vorgelagerte
 * Pruefung, Fehleruebersetzung, Mutation, Auswahl, Ranking oder UI-Logik.
 */
class KnockoutProductCandidateExistingEntryCalculationApplicationService(
    private val existingKnockoutProductEntryCalculator: ExistingKnockoutProductEntryCalculator
) {

    fun execute(
        request: KnockoutProductCandidateExistingEntryCalculationRequest
    ): KnockoutProductCandidateExistingEntryCalculationResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateExistingEntryCalculationResult.NoInputCandidates
        }

        val candidatesWithExistingEntryCalculation = request.candidates.map { candidate ->
            val specification = candidate
                .input
                .candidateWithCalculation
                .candidateWithSourceEvaluation
                .candidateWithFreshness
                .candidateWithCalculationAvailability
                .candidateWithDataQuality
                .candidateWithMarketData
                .specificationSnapshot
                .specification

            val calculationResult = existingKnockoutProductEntryCalculator.calculate(
                ExistingKnockoutProductEntryCalculationInput(
                    plannedEntryPrice = request.plannedEntryPrice,
                    basePrice = specification.basePrice,
                    knockoutBarrier = specification.knockoutBarrier,
                    direction = specification.direction,
                    ratio = specification.ratio,
                    currencyConversion = candidate.input.currencyConversion
                )
            )

            KnockoutProductCandidateWithExistingEntryCalculation(
                candidateWithTargetLeveragePlan = candidate,
                existingEntryCalculationResult = calculationResult
            )
        }

        return KnockoutProductCandidateExistingEntryCalculationResult
            .CandidatesWithExistingEntryCalculation(candidatesWithExistingEntryCalculation)
    }
}
