package de.konavigator.app.application.productdiscovery

/**
 * Externe technische Target-Fit-Toleranzen fuer bereits durch das
 * Target-Deviation-Gate freigegebene Produktkandidaten.
 */
data class KnockoutProductCandidateTargetFitRequest(
    val candidates: List<KnockoutProductCandidateWithTargetDeviation>,
    val maxRelativeLeverageDeviationPercent: Double,
    val maxBarrierDeviationPercentOfPlannedEntry: Double
)
