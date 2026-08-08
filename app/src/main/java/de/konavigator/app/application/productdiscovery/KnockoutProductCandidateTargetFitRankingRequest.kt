package de.konavigator.app.application.productdiscovery

/** Providerneutraler Auftrag zum Ranking bereits durch das Target-Fit-Gate freigegebener Kandidaten. */
data class KnockoutProductCandidateTargetFitRankingRequest(
    val candidates: List<KnockoutProductCandidateWithTargetFit>
)
