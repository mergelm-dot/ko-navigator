package de.konavigator.app.calculator

/**
 * Extern vorgegebene technische Toleranzgrenzen fuer ein bereits erfolgreich
 * berechnetes Target-Deviation-Ergebnis.
 */
data class ExistingKnockoutProductTargetFitInput(
    val targetDeviation: ExistingKnockoutProductTargetDeviationResult.Success,
    val maxRelativeLeverageDeviationPercent: Double,
    val maxBarrierDeviationPercentOfPlannedEntry: Double
)
