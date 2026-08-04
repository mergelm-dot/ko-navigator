package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.calculator.MarketDataCalculationError
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue

/** Ein unverändertes Ergebnis der vorhandenen mathematischen Berechnung. */
sealed interface KnockoutProductCandidateCalculationOutcome {
    data class Success(
        val value: MarketDataCalculationValue
    ) : KnockoutProductCandidateCalculationOutcome

    data class Failure(
        val error: MarketDataCalculationError
    ) : KnockoutProductCandidateCalculationOutcome
}

/** Verbindet einen unveränderten Source-Evaluation-Kandidaten mit seinem Ergebnis. */
data class KnockoutProductCandidateWithCalculation(
    val candidateWithSourceEvaluation: KnockoutProductCandidateWithSourceEvaluation,
    val calculationOutcome: KnockoutProductCandidateCalculationOutcome
)

/**
 * Providerneutraler Ergebnisvertrag ohne Filterung, Ranking oder weitere
 * Produktentscheidung. Erfolgreiche und fehlgeschlagene Berechnungen bleiben
 * gemeinsam enthalten; er ist keine Kauf- oder Verkaufsempfehlung.
 */
sealed interface KnockoutProductCandidateCalculationResult {
    data class CandidatesWithCalculation(
        val candidates: List<KnockoutProductCandidateWithCalculation>
    ) : KnockoutProductCandidateCalculationResult

    data object NoInputCandidates : KnockoutProductCandidateCalculationResult
}
