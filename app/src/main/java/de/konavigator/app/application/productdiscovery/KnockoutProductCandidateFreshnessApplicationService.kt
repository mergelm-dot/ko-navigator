package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy

/**
 * Reiner synchroner, zustandsloser Application-Service, der ausschließlich an die injizierte
 * [MarketDataFreshnessPolicy] delegiert. CalculationType, Marktdateninstanz und
 * evaluationTimeEpochMillis werden unverändert weitergegeben; die Policy bleibt die einzige
 * Regelquelle. Es gibt keine Systemzeit, keine eigenen Schwellen, keine erneute Data-Quality- oder
 * Availability-Bewertung, keine Filterung und keine Freshness-, Quellen-, Berechnungs- oder
 * Rankingentscheidung außerhalb der Policy.
 */
class KnockoutProductCandidateFreshnessApplicationService(
    private val freshnessPolicy: MarketDataFreshnessPolicy
) {

    fun execute(
        request: KnockoutProductCandidateFreshnessRequest
    ): KnockoutProductCandidateFreshnessResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateFreshnessResult.NoInputCandidates
        }

        val candidatesWithFreshness = request.candidates.map { candidate ->
            val freshnessResult = freshnessPolicy.evaluate(
                calculationType = request.calculationType,
                marketData = candidate.candidateWithDataQuality.candidateWithMarketData.marketData,
                evaluationTimeEpochMillis = request.evaluationTimeEpochMillis
            )
            KnockoutProductCandidateWithFreshness(
                candidateWithCalculationAvailability = candidate,
                freshnessResult = freshnessResult
            )
        }

        return KnockoutProductCandidateFreshnessResult.CandidatesWithFreshness(
            candidates = candidatesWithFreshness
        )
    }
}
