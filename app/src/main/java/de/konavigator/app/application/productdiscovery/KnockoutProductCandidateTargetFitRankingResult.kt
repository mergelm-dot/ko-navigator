package de.konavigator.app.application.productdiscovery

/** Providerneutraler Rankingvertrag ohne endgültige Produktauswahl. */
sealed interface KnockoutProductCandidateTargetFitRankingResult {
    data class RankedCandidates(
        val candidates: List<KnockoutProductCandidateWithTargetFit>
    ) : KnockoutProductCandidateTargetFitRankingResult

    data object NoInputCandidates : KnockoutProductCandidateTargetFitRankingResult
}
