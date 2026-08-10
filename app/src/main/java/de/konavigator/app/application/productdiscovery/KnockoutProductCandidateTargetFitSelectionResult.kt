package de.konavigator.app.application.productdiscovery

/**
 * Technische Positionsauswahl aus einem vorgegebenen Ranking ohne
 * Investment-, Risiko- oder Orderaussage.
 */
sealed interface KnockoutProductCandidateTargetFitSelectionResult {
    data class SelectedCandidates(
        val primaryCandidate: KnockoutProductCandidateWithTargetFit,
        val alternativeCandidates: List<KnockoutProductCandidateWithTargetFit>
    ) : KnockoutProductCandidateTargetFitSelectionResult

    data object NoInputCandidates : KnockoutProductCandidateTargetFitSelectionResult
}
