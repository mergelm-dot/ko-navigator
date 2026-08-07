package de.konavigator.app.calculator

import kotlin.math.abs

/**
 * Reiner, synchroner und zustandsloser Calculator fuer sechs ungerundete,
 * ausschliesslich numerische Zielabweichungen. Er interpretiert weder das
 * Barrierevorzeichen noch bewertet, gewichtet, rankt oder empfiehlt er Werte.
 */
object ExistingKnockoutProductTargetDeviationCalculator {

    fun calculate(
        input: ExistingKnockoutProductTargetDeviationInput
    ): ExistingKnockoutProductTargetDeviationResult {
        if (!input.plannedEntryPrice.isFinite() || input.plannedEntryPrice <= 0.0) {
            return failure(ExistingKnockoutProductTargetDeviationError.INVALID_PLANNED_ENTRY_PRICE)
        }
        if (!input.targetLeverage.isFinite() || input.targetLeverage <= 1.0) {
            return failure(ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_LEVERAGE)
        }
        if (!input.actualLeverageAtEntry.isFinite() || input.actualLeverageAtEntry <= 1.0) {
            return failure(ExistingKnockoutProductTargetDeviationError.INVALID_ACTUAL_LEVERAGE)
        }
        if (!input.targetKnockoutBarrier.isFinite() || input.targetKnockoutBarrier <= 0.0) {
            return failure(ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_KNOCKOUT_BARRIER)
        }
        if (!input.actualKnockoutBarrier.isFinite() || input.actualKnockoutBarrier <= 0.0) {
            return failure(ExistingKnockoutProductTargetDeviationError.INVALID_ACTUAL_KNOCKOUT_BARRIER)
        }

        val leverageDifference = input.actualLeverageAtEntry - input.targetLeverage
        val absoluteLeverageDeviation = abs(leverageDifference)
        val relativeLeverageDeviationPercent = absoluteLeverageDeviation / input.targetLeverage * 100.0
        if (
            !leverageDifference.isFinite() ||
            !absoluteLeverageDeviation.isFinite() || absoluteLeverageDeviation < 0.0 ||
            !relativeLeverageDeviationPercent.isFinite() || relativeLeverageDeviationPercent < 0.0
        ) {
            return failure(ExistingKnockoutProductTargetDeviationError.INVALID_LEVERAGE_DEVIATION)
        }

        val barrierDifference = input.actualKnockoutBarrier - input.targetKnockoutBarrier
        val absoluteBarrierDeviation = abs(barrierDifference)
        val barrierDeviationPercentOfPlannedEntry = absoluteBarrierDeviation / input.plannedEntryPrice * 100.0
        if (
            !barrierDifference.isFinite() ||
            !absoluteBarrierDeviation.isFinite() || absoluteBarrierDeviation < 0.0 ||
            !barrierDeviationPercentOfPlannedEntry.isFinite() || barrierDeviationPercentOfPlannedEntry < 0.0
        ) {
            return failure(ExistingKnockoutProductTargetDeviationError.INVALID_BARRIER_DEVIATION)
        }

        return ExistingKnockoutProductTargetDeviationResult.Success(
            leverageDifference = leverageDifference,
            absoluteLeverageDeviation = absoluteLeverageDeviation,
            relativeLeverageDeviationPercent = relativeLeverageDeviationPercent,
            barrierDifference = barrierDifference,
            absoluteBarrierDeviation = absoluteBarrierDeviation,
            barrierDeviationPercentOfPlannedEntry = barrierDeviationPercentOfPlannedEntry
        )
    }

    private fun failure(
        error: ExistingKnockoutProductTargetDeviationError
    ) = ExistingKnockoutProductTargetDeviationResult.Failure(error)
}
