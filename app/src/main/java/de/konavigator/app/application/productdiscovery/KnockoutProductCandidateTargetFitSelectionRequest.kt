package de.konavigator.app.application.productdiscovery

/** Providerneutraler Auftrag fuer die Positionsauswahl bereits gerankter Kandidaten. */
data class KnockoutProductCandidateTargetFitSelectionRequest(
    val rankedCandidates: List<KnockoutProductCandidateWithTargetFit>
)
