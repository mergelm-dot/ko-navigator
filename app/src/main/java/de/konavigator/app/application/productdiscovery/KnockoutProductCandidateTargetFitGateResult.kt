package de.konavigator.app.application.productdiscovery

/**
 * Typisierter Ergebnisvertrag der reinen technischen Target-Fit-Partitionierung
 * ohne Eignungs-, Ranking-, Auswahl- oder Orderaussage.
 */
sealed interface KnockoutProductCandidateTargetFitGateResult {
    data class CandidatesWithinTargetTolerances(
        val matchingCandidates: List<KnockoutProductCandidateWithTargetFit>,
        val nonMatchingCandidates: List<KnockoutProductCandidateWithTargetFit>,
        val failedCandidates: List<KnockoutProductCandidateWithTargetFit>
    ) : KnockoutProductCandidateTargetFitGateResult

    data class NoCandidatesWithinTargetTolerances(
        val nonMatchingCandidates: List<KnockoutProductCandidateWithTargetFit>,
        val failedCandidates: List<KnockoutProductCandidateWithTargetFit>
    ) : KnockoutProductCandidateTargetFitGateResult

    data object NoInputCandidates : KnockoutProductCandidateTargetFitGateResult
}
