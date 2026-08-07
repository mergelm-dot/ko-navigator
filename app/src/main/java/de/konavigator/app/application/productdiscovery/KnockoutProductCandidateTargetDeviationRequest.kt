package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Auftrag fuer rein mathematische Zielabweichungen bereits
 * erfolgreich freigegebener bestehender Produktkandidaten. Er validiert und
 * normalisiert nichts und trifft keine Eignungs-, Ranking- oder Orderaussage.
 */
data class KnockoutProductCandidateTargetDeviationRequest(
    val candidates: List<KnockoutProductCandidateWithExistingEntryCalculation>,
    val plannedEntryPrice: Double
)
