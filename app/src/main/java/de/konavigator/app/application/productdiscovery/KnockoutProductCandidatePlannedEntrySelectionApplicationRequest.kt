package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Auftrag ab erfolgreich berechneten Kandidaten mit ihrer
 * extern bereitgestellten CurrencyConversion bis zur technischen Auswahl.
 */
data class KnockoutProductCandidatePlannedEntrySelectionApplicationRequest(
    val candidates: List<KnockoutProductCandidateTargetLeverageInput>,
    val underlyingPrice: Double,
    val plannedEntryPrice: Double,
    val targetLeverage: Double,
    val maxRelativeLeverageDeviationPercent: Double,
    val maxBarrierDeviationPercentOfPlannedEntry: Double
)
