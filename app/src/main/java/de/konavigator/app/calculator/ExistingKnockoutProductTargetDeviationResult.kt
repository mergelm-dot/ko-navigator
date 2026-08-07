package de.konavigator.app.calculator

/** Reines, ungerundetes Ergebnis numerischer Zielabweichungen ohne Bewertung oder Empfehlung. */
sealed interface ExistingKnockoutProductTargetDeviationResult {
    data class Success(
        val leverageDifference: Double,
        val absoluteLeverageDeviation: Double,
        val relativeLeverageDeviationPercent: Double,
        val barrierDifference: Double,
        val absoluteBarrierDeviation: Double,
        val barrierDeviationPercentOfPlannedEntry: Double
    ) : ExistingKnockoutProductTargetDeviationResult

    data class Failure(
        val error: ExistingKnockoutProductTargetDeviationError
    ) : ExistingKnockoutProductTargetDeviationResult
}

enum class ExistingKnockoutProductTargetDeviationError {
    INVALID_PLANNED_ENTRY_PRICE,
    INVALID_TARGET_LEVERAGE,
    INVALID_ACTUAL_LEVERAGE,
    INVALID_TARGET_KNOCKOUT_BARRIER,
    INVALID_ACTUAL_KNOCKOUT_BARRIER,
    INVALID_LEVERAGE_DEVIATION,
    INVALID_BARRIER_DEVIATION
}
