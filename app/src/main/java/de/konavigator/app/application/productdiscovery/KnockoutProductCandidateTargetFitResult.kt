package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitResult

/** Verbindet den Originalkandidaten mit seinem unveränderten Target-Fit-Ergebnis. */
data class KnockoutProductCandidateWithTargetFit(
    val candidateWithTargetDeviation: KnockoutProductCandidateWithTargetDeviation,
    val targetFitResult: ExistingKnockoutProductTargetFitResult
)

/** Providerneutraler Ergebnisvertrag ohne Filterung, Auswahl, Ranking oder UI. */
sealed interface KnockoutProductCandidateTargetFitResult {
    data class CandidatesWithTargetFit(
        val candidates: List<KnockoutProductCandidateWithTargetFit>
    ) : KnockoutProductCandidateTargetFitResult

    data object NoInputCandidates : KnockoutProductCandidateTargetFitResult
}
