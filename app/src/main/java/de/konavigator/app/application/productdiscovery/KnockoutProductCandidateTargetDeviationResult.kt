package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult

/**
 * Verbindet einen unveraenderten Existing-Entry-erfolgreichen Kandidaten mit
 * seinem unveraenderten rein mathematischen Zielabweichungsergebnis.
 */
data class KnockoutProductCandidateWithTargetDeviation(
    val candidateWithExistingEntryCalculation: KnockoutProductCandidateWithExistingEntryCalculation,
    val targetDeviationResult: ExistingKnockoutProductTargetDeviationResult
)

/** Providerneutraler Ergebnisvertrag ohne Filterung, Eignung, Ranking oder UI. */
sealed interface KnockoutProductCandidateTargetDeviationResult {
    data class CandidatesWithTargetDeviation(
        val candidates: List<KnockoutProductCandidateWithTargetDeviation>
    ) : KnockoutProductCandidateTargetDeviationResult

    data object NoInputCandidates : KnockoutProductCandidateTargetDeviationResult
}
