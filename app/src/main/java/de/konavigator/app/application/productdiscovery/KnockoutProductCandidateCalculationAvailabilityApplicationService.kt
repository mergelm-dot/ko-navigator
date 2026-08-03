package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityEvaluator

/**
 * Reiner, synchroner, zustandsloser und providerneutraler Application-Service für Calculation
 * Availability bereits strukturell freigegebener Kandidaten.
 *
 * Der Service delegiert jeden Eingabelisteneintrag ausschließlich an
 * [MarketDataCalculationAvailabilityEvaluator], der damit die einzige Regelquelle bleibt.
 * CalculationType und Marktdatenobjekt werden exakt weitergegeben. Es findet keine erneute
 * Data-Quality-, Broker- oder Emittentenprüfung und kein erneutes Laden von Marktdaten statt.
 *
 * Reihenfolge, Duplikate, Eingaben und Ergebnisstatus bleiben erhalten: insbesondere werden
 * StructurallyUnavailable-Kandidaten nicht gefiltert. Der Service besitzt keinen Cache,
 * Memoization oder globalen Zustand und enthält keine Systemzeit-, Freshness-, Quellen-,
 * Berechnungs-, Spread-, Zielhebel-, Ranking- oder UI-Logik.
 */
class KnockoutProductCandidateCalculationAvailabilityApplicationService {

    fun execute(
        request: KnockoutProductCandidateCalculationAvailabilityRequest
    ): KnockoutProductCandidateCalculationAvailabilityResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateCalculationAvailabilityResult.NoInputCandidates
        }

        val candidatesWithCalculationAvailability = request.candidates.map { candidate ->
            val availabilityResult = MarketDataCalculationAvailabilityEvaluator.evaluate(
                calculationType = request.calculationType,
                marketData = candidate.candidateWithMarketData.marketData
            )
            KnockoutProductCandidateWithCalculationAvailability(
                candidateWithDataQuality = candidate,
                availabilityResult = availabilityResult
            )
        }

        return KnockoutProductCandidateCalculationAvailabilityResult
            .CandidatesWithCalculationAvailability(
                candidates = candidatesWithCalculationAvailability
            )
    }
}
