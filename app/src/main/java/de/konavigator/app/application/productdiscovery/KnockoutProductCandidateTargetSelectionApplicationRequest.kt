package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Auftrag ab bereits erfolgreich freigegebenen
 * Existing-Entry-Kandidaten bis zur technischen Target-Fit-Auswahl.
 */
data class KnockoutProductCandidateTargetSelectionApplicationRequest(
    val candidates: List<KnockoutProductCandidateWithExistingEntryCalculation>,
    val plannedEntryPrice: Double,
    val maxRelativeLeverageDeviationPercent: Double,
    val maxBarrierDeviationPercentOfPlannedEntry: Double
)
