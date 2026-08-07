package de.konavigator.app.calculator

/**
 * Reiner, synchroner und zustandsloser Calculator fuer zwei technisch
 * vorgegebene Target-Toleranzen. Er bewertet weder Produkt, Risiko noch
 * Eignung und bildet keinen Score.
 */
object ExistingKnockoutProductTargetFitCalculator {

    fun calculate(
        input: ExistingKnockoutProductTargetFitInput
    ): ExistingKnockoutProductTargetFitResult {
        val targetDeviation = input.targetDeviation
        if (
            !targetDeviation.relativeLeverageDeviationPercent.isFinite() ||
            targetDeviation.relativeLeverageDeviationPercent < 0.0
        ) {
            return failure(
                ExistingKnockoutProductTargetFitError
                    .INVALID_RELATIVE_LEVERAGE_DEVIATION_PERCENT
            )
        }
        if (
            !targetDeviation.barrierDeviationPercentOfPlannedEntry.isFinite() ||
            targetDeviation.barrierDeviationPercentOfPlannedEntry < 0.0
        ) {
            return failure(
                ExistingKnockoutProductTargetFitError
                    .INVALID_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY
            )
        }
        if (
            !input.maxRelativeLeverageDeviationPercent.isFinite() ||
            input.maxRelativeLeverageDeviationPercent < 0.0
        ) {
            return failure(
                ExistingKnockoutProductTargetFitError
                    .INVALID_MAX_RELATIVE_LEVERAGE_DEVIATION_PERCENT
            )
        }
        if (
            !input.maxBarrierDeviationPercentOfPlannedEntry.isFinite() ||
            input.maxBarrierDeviationPercentOfPlannedEntry < 0.0
        ) {
            return failure(
                ExistingKnockoutProductTargetFitError
                    .INVALID_MAX_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY
            )
        }

        val leverageWithinTolerance =
            targetDeviation.relativeLeverageDeviationPercent <=
                input.maxRelativeLeverageDeviationPercent
        val barrierWithinTolerance =
            targetDeviation.barrierDeviationPercentOfPlannedEntry <=
                input.maxBarrierDeviationPercentOfPlannedEntry

        return ExistingKnockoutProductTargetFitResult.Success(
            leverageWithinTolerance = leverageWithinTolerance,
            barrierWithinTolerance = barrierWithinTolerance,
            withinAllTargetTolerances =
                leverageWithinTolerance && barrierWithinTolerance
        )
    }

    private fun failure(
        error: ExistingKnockoutProductTargetFitError
    ) = ExistingKnockoutProductTargetFitResult.Failure(error)
}
