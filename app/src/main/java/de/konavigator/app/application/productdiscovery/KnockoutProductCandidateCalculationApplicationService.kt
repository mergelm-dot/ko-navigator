package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.calculator.MarketDataCalculationResult
import de.konavigator.app.domain.calculator.MarketDataCalculator
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue

/**
 * Reiner, synchroner und zustandsloser Application-Service für die Berechnung
 * source-freigegebener Kandidaten. Der [MarketDataCalculator] bleibt die
 * alleinige Quelle für Spread- und Mid-Formeln und deren Fehler.
 *
 * Der Service wiederholt keine vorgelagerten Prüfungen, ruft keinen
 * Orchestrator auf und führt keine Rundung, Formatierung, Währungsumrechnung,
 * Korrektur, Filterung, Sortierung, Gruppierung oder Deduplizierung aus. Er
 * enthält keinen Cache, globalen Zustand, Repository-, Provider- oder Zeitcode.
 */
class KnockoutProductCandidateCalculationApplicationService {

    fun execute(
        request: KnockoutProductCandidateCalculationRequest
    ): KnockoutProductCandidateCalculationResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateCalculationResult.NoInputCandidates
        }

        val candidates = request.candidates.map { candidate ->
            val marketData = candidate
                .candidateWithFreshness
                .candidateWithCalculationAvailability
                .candidateWithDataQuality
                .candidateWithMarketData
                .marketData
            val outcome = when (request.calculationType) {
                MarketDataCalculationType.PURCHASE_PRICE -> {
                    KnockoutProductCandidateCalculationOutcome.Success(
                        MarketDataCalculationValue.PurchasePrice(
                            value = requireNotNull(marketData.ask) {
                                "Ask is required after successful PURCHASE_PRICE gates"
                            },
                            currency = marketData.currency
                        )
                    )
                }

                MarketDataCalculationType.SALE_PRICE -> {
                    KnockoutProductCandidateCalculationOutcome.Success(
                        MarketDataCalculationValue.SalePrice(
                            value = requireNotNull(marketData.bid) {
                                "Bid is required after successful SALE_PRICE gates"
                            },
                            currency = marketData.currency
                        )
                    )
                }

                MarketDataCalculationType.SPREAD -> calculateSpread(
                    bid = requireNotNull(marketData.bid) {
                        "Bid is required after successful SPREAD gates"
                    },
                    ask = requireNotNull(marketData.ask) {
                        "Ask is required after successful SPREAD gates"
                    },
                    currency = marketData.currency
                )

                MarketDataCalculationType.MID -> calculateMid(
                    bid = requireNotNull(marketData.bid) {
                        "Bid is required after successful MID gates"
                    },
                    ask = requireNotNull(marketData.ask) {
                        "Ask is required after successful MID gates"
                    },
                    currency = marketData.currency
                )
            }
            KnockoutProductCandidateWithCalculation(candidate, outcome)
        }

        return KnockoutProductCandidateCalculationResult.CandidatesWithCalculation(candidates)
    }

    private fun calculateSpread(
        bid: Double,
        ask: Double,
        currency: String
    ): KnockoutProductCandidateCalculationOutcome {
        val absolute = when (val result = MarketDataCalculator.calculateAbsoluteSpread(bid, ask)) {
            is MarketDataCalculationResult.Success -> result.value
            is MarketDataCalculationResult.Failure -> return KnockoutProductCandidateCalculationOutcome.Failure(result.error)
        }
        val relative = when (val result = MarketDataCalculator.calculateRelativeSpreadToAskPercent(bid, ask)) {
            is MarketDataCalculationResult.Success -> result.value
            is MarketDataCalculationResult.Failure -> return KnockoutProductCandidateCalculationOutcome.Failure(result.error)
        }
        return KnockoutProductCandidateCalculationOutcome.Success(
            MarketDataCalculationValue.Spread(absolute, relative, currency)
        )
    }

    private fun calculateMid(
        bid: Double,
        ask: Double,
        currency: String
    ): KnockoutProductCandidateCalculationOutcome = when (
        val result = MarketDataCalculator.calculateMidPrice(bid, ask)
    ) {
        is MarketDataCalculationResult.Success -> KnockoutProductCandidateCalculationOutcome.Success(
            MarketDataCalculationValue.MidPrice(result.value, currency)
        )
        is MarketDataCalculationResult.Failure -> KnockoutProductCandidateCalculationOutcome.Failure(result.error)
    }
}
