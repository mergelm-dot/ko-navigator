package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.source.MarketDataSourcePolicy

/**
 * Reiner, synchroner, zustandsloser und providerneutraler Application-Service
 * zur Quellenbewertung. Er delegiert jeden Eingabelisteneintrag ausschließlich
 * mit unverändertem Berechnungstyp und sourceId an die injizierte
 * [MarketDataSourcePolicy], die die alleinige Regelquelle bleibt.
 *
 * Allowed und Blocked werden beide transportiert. Der Service erzeugt keine
 * Policy-Konfiguration oder Defaults, leitet keine Capabilities ab, bewertet
 * keine vorherige Stufe erneut und nimmt keine Filterung, Mutation,
 * Normalisierung, Sortierung, Gruppierung oder Deduplizierung vor. Er enthält
 * weder Zeit-, Preis-, Hebel-, Score-, Ranking- noch Android-, Compose- oder
 * UI-Logik.
 */
class KnockoutProductCandidateSourceEvaluationApplicationService(
    private val sourcePolicy: MarketDataSourcePolicy
) {

    fun execute(
        request: KnockoutProductCandidateSourceEvaluationRequest
    ): KnockoutProductCandidateSourceEvaluationResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateSourceEvaluationResult.NoInputCandidates
        }

        val candidatesWithSourceEvaluation = request.candidates.map { candidate ->
            val sourceResult = sourcePolicy.evaluate(
                calculationType = request.calculationType,
                sourceId = candidate
                    .candidateWithCalculationAvailability
                    .candidateWithDataQuality
                    .candidateWithMarketData
                    .marketData
                    .sourceId
            )

            KnockoutProductCandidateWithSourceEvaluation(
                candidateWithFreshness = candidate,
                sourceResult = sourceResult
            )
        }

        return KnockoutProductCandidateSourceEvaluationResult
            .CandidatesWithSourceEvaluation(candidatesWithSourceEvaluation)
    }
}
