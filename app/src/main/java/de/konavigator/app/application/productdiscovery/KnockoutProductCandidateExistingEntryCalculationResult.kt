package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult

/**
 * Verbindet einen unveraenderten Target-Leverage-Kandidaten mit seiner
 * theoretischen Einstiegsberechnung fuer das bestehende Produkt. Das Ergebnis
 * ist keine reale Emittentenpreisgarantie und trifft keine Aussage ueber
 * Zielhebelgenauigkeit, Barrierenuebereinstimmung oder eine Order.
 */
data class KnockoutProductCandidateWithExistingEntryCalculation(
    val candidateWithTargetLeveragePlan: KnockoutProductCandidateWithTargetLeveragePlan,
    val existingEntryCalculationResult: ExistingKnockoutProductEntryCalculationResult
)

/**
 * Providerneutraler Ergebnisvertrag ohne Filterung, Gate, Ranking oder
 * UI-Texte. Success- und Failure-Ergebnisse der Berechnung bleiben gemeinsam
 * und unveraendert transportiert.
 */
sealed interface KnockoutProductCandidateExistingEntryCalculationResult {
    data class CandidatesWithExistingEntryCalculation(
        val candidates: List<KnockoutProductCandidateWithExistingEntryCalculation>
    ) : KnockoutProductCandidateExistingEntryCalculationResult

    data object NoInputCandidates : KnockoutProductCandidateExistingEntryCalculationResult
}
