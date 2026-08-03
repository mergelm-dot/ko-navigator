package de.konavigator.app.application.productdiscovery

/** Providerneutraler Auftrag für das Gate bereits vollständig bewerteter Freshness-Kandidaten. */
data class KnockoutProductCandidateFreshnessGateRequest(
    val candidates: List<KnockoutProductCandidateWithFreshness>
)
