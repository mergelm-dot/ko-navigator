package de.konavigator.app.application.productdiscovery

/**
 * Typisierter Endvertrag der Target-Selection-Orchestrierung. Die
 * Diagnosegruppen bleiben getrennt und enthalten keine Empfehlung.
 */
sealed interface KnockoutProductCandidateTargetSelectionApplicationResult {
    data class SelectedCandidates(
        val primaryCandidate: KnockoutProductCandidateWithTargetFit,
        val alternativeCandidates: List<KnockoutProductCandidateWithTargetFit>,
        val targetDeviationFailedCandidates: List<KnockoutProductCandidateWithTargetDeviation>,
        val nonMatchingCandidates: List<KnockoutProductCandidateWithTargetFit>,
        val targetFitFailedCandidates: List<KnockoutProductCandidateWithTargetFit>
    ) : KnockoutProductCandidateTargetSelectionApplicationResult

    data class NoSuccessfulTargetDeviationCandidates(
        val failedCandidates: List<KnockoutProductCandidateWithTargetDeviation>
    ) : KnockoutProductCandidateTargetSelectionApplicationResult

    data class NoCandidatesWithinTargetTolerances(
        val targetDeviationFailedCandidates: List<KnockoutProductCandidateWithTargetDeviation>,
        val nonMatchingCandidates: List<KnockoutProductCandidateWithTargetFit>,
        val targetFitFailedCandidates: List<KnockoutProductCandidateWithTargetFit>
    ) : KnockoutProductCandidateTargetSelectionApplicationResult

    data object NoInputCandidates : KnockoutProductCandidateTargetSelectionApplicationResult
}
