package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationType

/** Providerneutraler Application-Auftrag zur Freshness-Bewertung calculation-available Kandidaten. */
data class KnockoutProductCandidateFreshnessRequest(
    val candidates: List<KnockoutProductCandidateWithCalculationAvailability>,
    val calculationType: MarketDataCalculationType,
    val evaluationTimeEpochMillis: Long
)
