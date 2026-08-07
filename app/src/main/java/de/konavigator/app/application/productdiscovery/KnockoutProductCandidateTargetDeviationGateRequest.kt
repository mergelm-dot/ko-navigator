package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Auftrag fuer das Target-Deviation-Gate. Alle Ergebnisse
 * liegen bereits vor; dieser Vertrag validiert und berechnet nichts erneut und
 * trifft keine Eignungs-, Ranking- oder Orderentscheidung.
 */
data class KnockoutProductCandidateTargetDeviationGateRequest(
    val candidates: List<KnockoutProductCandidateWithTargetDeviation>
)
