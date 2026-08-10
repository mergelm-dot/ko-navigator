package de.konavigator.app.application.productdiscovery

/**
 * Reiner, synchroner und zustandsloser Selector. Er übernimmt ausschließlich
 * die ersten drei Positionen eines bereits verbindlich gerankten Eingangs.
 */
class KnockoutProductCandidateTargetFitSelector {

    fun select(
        request: KnockoutProductCandidateTargetFitSelectionRequest
    ): KnockoutProductCandidateTargetFitSelectionResult {
        if (request.rankedCandidates.isEmpty()) {
            return KnockoutProductCandidateTargetFitSelectionResult.NoInputCandidates
        }

        return KnockoutProductCandidateTargetFitSelectionResult.SelectedCandidates(
            primaryCandidate = request.rankedCandidates[0],
            alternativeCandidates = request.rankedCandidates.drop(1).take(2)
        )
    }
}
