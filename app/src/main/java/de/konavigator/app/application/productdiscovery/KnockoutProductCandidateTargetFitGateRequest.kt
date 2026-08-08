package de.konavigator.app.application.productdiscovery

/** Providerneutraler Auftrag fuer die reine Target-Fit-Ergebnispartitionierung. */
data class KnockoutProductCandidateTargetFitGateRequest(
    val candidates: List<KnockoutProductCandidateWithTargetFit>
)
