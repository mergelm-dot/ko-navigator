package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.calculator.TradeCalculationInput
import de.konavigator.app.domain.model.TradeDirection

/**
 * Reiner, synchroner und zustandsloser Application-Service für theoretische
 * Zielhebel-Pläne. Die injizierte [TradeCalculationEngine] ist die alleinige
 * Regel- und Formelquelle; jedes Engine-Ergebnis wird unverändert transportiert.
 *
 * Es gibt keine eigene Long-/Short-, Hebel-, KO- oder Produktwertformel, keine
 * CurrencyConversion-Erzeugung, keine Validierung oder Barrierenabweichung und
 * keine erneute vorgelagerte Prüfung. Der Service filtert, sortiert, gruppiert
 * oder dedupliziert nicht und enthält keinen Cache, globalen Zustand, Ranking-
 * oder UI-Code.
 */
class KnockoutProductCandidateTargetLeverageApplicationService(
    private val tradeCalculationEngine: TradeCalculationEngine
) {

    fun execute(
        request: KnockoutProductCandidateTargetLeverageRequest
    ): KnockoutProductCandidateTargetLeverageResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateTargetLeverageResult.NoInputCandidates
        }

        val candidates = request.candidates.map { input ->
            val specification = input
                .candidateWithCalculation
                .candidateWithSourceEvaluation
                .candidateWithFreshness
                .candidateWithCalculationAvailability
                .candidateWithDataQuality
                .candidateWithMarketData
                .specificationSnapshot
                .specification
            val result = tradeCalculationEngine.calculateTrade(
                TradeCalculationInput(
                    underlyingPrice = request.underlyingPrice,
                    plannedEntryPrice = request.plannedEntryPrice,
                    targetLeverage = request.targetLeverage,
                    isLong = specification.direction == TradeDirection.LONG,
                    ratio = specification.ratio,
                    currencyConversion = input.currencyConversion
                )
            )
            KnockoutProductCandidateWithTargetLeveragePlan(input, result)
        }

        return KnockoutProductCandidateTargetLeverageResult
            .CandidatesWithTargetLeveragePlan(candidates)
    }
}
