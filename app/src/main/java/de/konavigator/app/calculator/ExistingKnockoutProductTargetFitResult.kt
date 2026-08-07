package de.konavigator.app.calculator

/**
 * Rein technische Target-Fit-Klassifizierung ohne Eignungs-, Ranking- oder
 * Orderaussage.
 */
sealed interface ExistingKnockoutProductTargetFitResult {
    data class Success(
        val leverageWithinTolerance: Boolean,
        val barrierWithinTolerance: Boolean,
        val withinAllTargetTolerances: Boolean
    ) : ExistingKnockoutProductTargetFitResult

    data class Failure(
        val error: ExistingKnockoutProductTargetFitError
    ) : ExistingKnockoutProductTargetFitResult
}

enum class ExistingKnockoutProductTargetFitError {
    INVALID_RELATIVE_LEVERAGE_DEVIATION_PERCENT,
    INVALID_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY,
    INVALID_MAX_RELATIVE_LEVERAGE_DEVIATION_PERCENT,
    INVALID_MAX_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY
}
