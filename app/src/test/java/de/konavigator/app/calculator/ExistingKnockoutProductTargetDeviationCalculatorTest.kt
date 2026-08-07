package de.konavigator.app.calculator

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExistingKnockoutProductTargetDeviationCalculatorTest {
    @Test
    fun exactTargetMatchReturnsZeroDeviations() {
        val result = success(input(targetLeverage = 5.0, actualLeverage = 5.0, targetBarrier = 90.0, actualBarrier = 90.0))
        assertEquals(0.0, result.leverageDifference, 0.0)
        assertEquals(0.0, result.absoluteLeverageDeviation, 0.0)
        assertEquals(0.0, result.relativeLeverageDeviationPercent, 0.0)
        assertEquals(0.0, result.barrierDifference, 0.0)
        assertEquals(0.0, result.absoluteBarrierDeviation, 0.0)
        assertEquals(0.0, result.barrierDeviationPercentOfPlannedEntry, 0.0)
    }

    @Test
    fun leverageAboveTargetProducesPositiveDifference() {
        assertEquals(2.0, success(input(targetLeverage = 5.0, actualLeverage = 7.0)).leverageDifference, 0.0)
    }

    @Test
    fun leverageBelowTargetProducesNegativeDifference() {
        assertEquals(-2.0, success(input(targetLeverage = 7.0, actualLeverage = 5.0)).leverageDifference, 0.0)
    }

    @Test
    fun absoluteLeverageDeviationRemovesSign() {
        assertEquals(2.0, success(input(targetLeverage = 7.0, actualLeverage = 5.0)).absoluteLeverageDeviation, 0.0)
    }

    @Test
    fun relativeLeverageDeviationUsesTargetLeverage() {
        val input = input(targetLeverage = 8.0, actualLeverage = 5.0)
        assertEquals(abs(5.0 - 8.0) / 8.0 * 100.0, success(input).relativeLeverageDeviationPercent, 0.0)
    }

    @Test
    fun barrierAboveTargetProducesPositiveDifference() {
        assertEquals(10.0, success(input(targetBarrier = 90.0, actualBarrier = 100.0)).barrierDifference, 0.0)
    }

    @Test
    fun barrierBelowTargetProducesNegativeDifference() {
        assertEquals(-10.0, success(input(targetBarrier = 100.0, actualBarrier = 90.0)).barrierDifference, 0.0)
    }

    @Test
    fun absoluteBarrierDeviationRemovesSign() {
        assertEquals(10.0, success(input(targetBarrier = 100.0, actualBarrier = 90.0)).absoluteBarrierDeviation, 0.0)
    }

    @Test
    fun barrierDeviationPercentUsesPlannedEntryPrice() {
        val input = input(plannedEntry = 200.0, targetBarrier = 90.0, actualBarrier = 110.0)
        assertEquals(abs(110.0 - 90.0) / 200.0 * 100.0, success(input).barrierDeviationPercentOfPlannedEntry, 0.0)
    }

    @Test
    fun leverageAndBarrierMetricsRemainIndependent() {
        val base = success(input())
        val changedLeverage = success(input(targetLeverage = 8.0, actualLeverage = 6.0))
        assertEquals(base.barrierDifference, changedLeverage.barrierDifference, 0.0)
        assertEquals(base.absoluteBarrierDeviation, changedLeverage.absoluteBarrierDeviation, 0.0)
        assertEquals(base.barrierDeviationPercentOfPlannedEntry, changedLeverage.barrierDeviationPercentOfPlannedEntry, 0.0)
        val changedBarrier = success(input(targetBarrier = 80.0, actualBarrier = 110.0))
        assertEquals(base.leverageDifference, changedBarrier.leverageDifference, 0.0)
        assertEquals(base.absoluteLeverageDeviation, changedBarrier.absoluteLeverageDeviation, 0.0)
        assertEquals(base.relativeLeverageDeviationPercent, changedBarrier.relativeLeverageDeviationPercent, 0.0)
    }

    @Test
    fun invalidPlannedEntryPriceFails() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertFailure(input(plannedEntry = it), ExistingKnockoutProductTargetDeviationError.INVALID_PLANNED_ENTRY_PRICE)
        }
    }

    @Test
    fun invalidTargetLeverageFails() {
        listOf(1.0, 0.5, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertFailure(input(targetLeverage = it), ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_LEVERAGE)
        }
    }

    @Test
    fun invalidActualLeverageFails() {
        listOf(1.0, 0.5, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertFailure(input(actualLeverage = it), ExistingKnockoutProductTargetDeviationError.INVALID_ACTUAL_LEVERAGE)
        }
    }

    @Test
    fun invalidTargetKnockoutBarrierFails() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertFailure(input(targetBarrier = it), ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_KNOCKOUT_BARRIER)
        }
    }

    @Test
    fun invalidActualKnockoutBarrierFails() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertFailure(input(actualBarrier = it), ExistingKnockoutProductTargetDeviationError.INVALID_ACTUAL_KNOCKOUT_BARRIER)
        }
    }

    @Test
    fun validationOrderIsStable() {
        assertFailure(input(plannedEntry = 0.0, targetLeverage = 1.0, actualLeverage = 1.0, targetBarrier = 0.0, actualBarrier = 0.0), ExistingKnockoutProductTargetDeviationError.INVALID_PLANNED_ENTRY_PRICE)
        assertFailure(input(targetLeverage = 1.0, actualLeverage = 1.0, targetBarrier = 0.0, actualBarrier = 0.0), ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_LEVERAGE)
        assertFailure(input(actualLeverage = 1.0, targetBarrier = 0.0, actualBarrier = 0.0), ExistingKnockoutProductTargetDeviationError.INVALID_ACTUAL_LEVERAGE)
        assertFailure(input(targetBarrier = 0.0, actualBarrier = 0.0), ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_KNOCKOUT_BARRIER)
    }

    @Test
    fun nonFiniteLeverageDeviationFails() {
        assertFailure(
            input(targetLeverage = 2.0, actualLeverage = Double.MAX_VALUE),
            ExistingKnockoutProductTargetDeviationError.INVALID_LEVERAGE_DEVIATION
        )
    }

    @Test
    fun nonFiniteBarrierDeviationFails() {
        assertFailure(
            input(plannedEntry = Double.MIN_VALUE, targetBarrier = 1.0, actualBarrier = Double.MAX_VALUE),
            ExistingKnockoutProductTargetDeviationError.INVALID_BARRIER_DEVIATION
        )
    }

    @Test
    fun adjacentDoubleValuesRemainDistinctWithoutToleranceOrRounding() {
        val targetLeverage = 5.0
        val targetBarrier = 90.0
        val result = success(
            input(
                targetLeverage = targetLeverage,
                actualLeverage = Math.nextUp(targetLeverage),
                targetBarrier = targetBarrier,
                actualBarrier = Math.nextUp(targetBarrier)
            )
        )
        assertTrue(result.leverageDifference > 0.0)
        assertTrue(result.absoluteLeverageDeviation > 0.0)
        assertTrue(result.barrierDifference > 0.0)
        assertTrue(result.absoluteBarrierDeviation > 0.0)
    }

    @Test
    fun repeatedCallsShareNoStateAndContainNoLaterStageOutput() {
        val input = input(plannedEntry = 123.45, targetLeverage = 5.0, actualLeverage = 6.0, targetBarrier = 91.0, actualBarrier = 88.0)
        val first = ExistingKnockoutProductTargetDeviationCalculator.calculate(input)
        val second = ExistingKnockoutProductTargetDeviationCalculator.calculate(input)
        assertEquals(first, second)
        assertEquals(123.45, input.plannedEntryPrice, 0.0)
        assertEquals(5.0, input.targetLeverage, 0.0)
        assertEquals(6.0, input.actualLeverageAtEntry, 0.0)
        assertEquals(91.0, input.targetKnockoutBarrier, 0.0)
        assertEquals(88.0, input.actualKnockoutBarrier, 0.0)
    }

    private fun input(
        plannedEntry: Double = 120.0,
        targetLeverage: Double = 5.0,
        actualLeverage: Double = 6.0,
        targetBarrier: Double = 90.0,
        actualBarrier: Double = 100.0
    ) = ExistingKnockoutProductTargetDeviationInput(
        plannedEntry,
        targetLeverage,
        actualLeverage,
        targetBarrier,
        actualBarrier
    )

    private fun success(
        input: ExistingKnockoutProductTargetDeviationInput
    ): ExistingKnockoutProductTargetDeviationResult.Success {
        val result = ExistingKnockoutProductTargetDeviationCalculator.calculate(input)
        assertTrue(result is ExistingKnockoutProductTargetDeviationResult.Success)
        return result as ExistingKnockoutProductTargetDeviationResult.Success
    }

    private fun assertFailure(
        input: ExistingKnockoutProductTargetDeviationInput,
        error: ExistingKnockoutProductTargetDeviationError
    ) {
        val result = ExistingKnockoutProductTargetDeviationCalculator.calculate(input)
        assertTrue(result is ExistingKnockoutProductTargetDeviationResult.Failure)
        assertEquals(error, (result as ExistingKnockoutProductTargetDeviationResult.Failure).error)
    }
}
