package de.konavigator.app.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExistingKnockoutProductTargetFitCalculatorTest {
    @Test
    fun zeroDeviationsAndZeroTolerancesAreWithinAllTolerances() {
        assertSuccess(input(relativeLeverageDeviationPercent = 0.0, barrierDeviationPercent = 0.0, maxLeverageDeviationPercent = 0.0, maxBarrierDeviationPercent = 0.0), true, true, true)
    }

    @Test
    fun deviationsBelowTolerancesAreWithinAllTolerances() {
        assertSuccess(input(relativeLeverageDeviationPercent = 2.0, barrierDeviationPercent = 3.0, maxLeverageDeviationPercent = 2.1, maxBarrierDeviationPercent = 3.1), true, true, true)
    }

    @Test
    fun leverageDeviationEqualToToleranceIsWithinTolerance() {
        assertSuccess(input(relativeLeverageDeviationPercent = 5.0, maxLeverageDeviationPercent = 5.0), true, true, true)
    }

    @Test
    fun barrierDeviationEqualToToleranceIsWithinTolerance() {
        assertSuccess(input(barrierDeviationPercent = 5.0, maxBarrierDeviationPercent = 5.0), true, true, true)
    }

    @Test
    fun leverageDeviationAboveToleranceIsOutsideAllTolerances() {
        assertSuccess(input(relativeLeverageDeviationPercent = 5.0000001, maxLeverageDeviationPercent = 5.0), false, true, false)
    }

    @Test
    fun barrierDeviationAboveToleranceIsOutsideAllTolerances() {
        assertSuccess(input(barrierDeviationPercent = 5.0000001, maxBarrierDeviationPercent = 5.0), true, false, false)
    }

    @Test
    fun bothDeviationsAboveToleranceAreOutsideAllTolerances() {
        assertSuccess(input(relativeLeverageDeviationPercent = 6.0, barrierDeviationPercent = 7.0, maxLeverageDeviationPercent = 5.0, maxBarrierDeviationPercent = 5.0), false, false, false)
    }

    @Test
    fun leverageWithinAndBarrierOutsideProducesFalseOverallResult() {
        assertSuccess(input(relativeLeverageDeviationPercent = 4.0, barrierDeviationPercent = 6.0, maxLeverageDeviationPercent = 5.0, maxBarrierDeviationPercent = 5.0), true, false, false)
    }

    @Test
    fun leverageOutsideAndBarrierWithinProducesFalseOverallResult() {
        assertSuccess(input(relativeLeverageDeviationPercent = 6.0, barrierDeviationPercent = 4.0, maxLeverageDeviationPercent = 5.0, maxBarrierDeviationPercent = 5.0), false, true, false)
    }

    @Test
    fun invalidRelativeLeverageDeviationFails() {
        listOf(-0.1, Double.NaN, Double.POSITIVE_INFINITY).forEach { value ->
            assertFailure(input(relativeLeverageDeviationPercent = value), ExistingKnockoutProductTargetFitError.INVALID_RELATIVE_LEVERAGE_DEVIATION_PERCENT)
        }
    }

    @Test
    fun invalidBarrierDeviationFails() {
        listOf(-0.1, Double.NaN, Double.POSITIVE_INFINITY).forEach { value ->
            assertFailure(input(barrierDeviationPercent = value), ExistingKnockoutProductTargetFitError.INVALID_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY)
        }
    }

    @Test
    fun invalidMaximumLeverageDeviationFails() {
        listOf(-0.1, Double.NaN, Double.POSITIVE_INFINITY).forEach { value ->
            assertFailure(input(maxLeverageDeviationPercent = value), ExistingKnockoutProductTargetFitError.INVALID_MAX_RELATIVE_LEVERAGE_DEVIATION_PERCENT)
        }
    }

    @Test
    fun invalidMaximumBarrierDeviationFails() {
        listOf(-0.1, Double.NaN, Double.POSITIVE_INFINITY).forEach { value ->
            assertFailure(input(maxBarrierDeviationPercent = value), ExistingKnockoutProductTargetFitError.INVALID_MAX_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY)
        }
    }

    @Test
    fun validationOrderFollowsDeviationValuesBeforeToleranceValues() {
        assertFailure(
            input(
                relativeLeverageDeviationPercent = Double.NaN,
                barrierDeviationPercent = Double.NaN,
                maxLeverageDeviationPercent = Double.NaN,
                maxBarrierDeviationPercent = Double.NaN
            ),
            ExistingKnockoutProductTargetFitError.INVALID_RELATIVE_LEVERAGE_DEVIATION_PERCENT
        )
        assertFailure(
            input(
                barrierDeviationPercent = Double.NaN,
                maxLeverageDeviationPercent = Double.NaN,
                maxBarrierDeviationPercent = Double.NaN
            ),
            ExistingKnockoutProductTargetFitError.INVALID_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY
        )
        assertFailure(
            input(
                maxLeverageDeviationPercent = Double.NaN,
                maxBarrierDeviationPercent = Double.NaN
            ),
            ExistingKnockoutProductTargetFitError.INVALID_MAX_RELATIVE_LEVERAGE_DEVIATION_PERCENT
        )
    }

    @Test
    fun largeFiniteToleranceRemainsValid() {
        assertSuccess(
            input(maxLeverageDeviationPercent = Double.MAX_VALUE, maxBarrierDeviationPercent = Double.MAX_VALUE),
            true,
            true,
            true
        )
    }

    @Test
    fun differenceSignsDoNotAffectTargetFitWhenUsedPercentagesMatch() {
        val positiveDifferences = input(
            relativeLeverageDeviationPercent = 2.0,
            barrierDeviationPercent = 3.0,
            leverageDifference = 10.0,
            barrierDifference = 20.0
        )
        val negativeDifferences = input(
            relativeLeverageDeviationPercent = 2.0,
            barrierDeviationPercent = 3.0,
            leverageDifference = -10.0,
            barrierDifference = -20.0
        )

        assertEquals(
            ExistingKnockoutProductTargetFitCalculator.calculate(positiveDifferences),
            ExistingKnockoutProductTargetFitCalculator.calculate(negativeDifferences)
        )
    }

    private fun input(
        relativeLeverageDeviationPercent: Double = 4.0,
        barrierDeviationPercent: Double = 4.0,
        maxLeverageDeviationPercent: Double = 5.0,
        maxBarrierDeviationPercent: Double = 5.0,
        leverageDifference: Double = 1.0,
        barrierDifference: Double = 1.0
    ) = ExistingKnockoutProductTargetFitInput(
        targetDeviation = ExistingKnockoutProductTargetDeviationResult.Success(
            leverageDifference = leverageDifference,
            absoluteLeverageDeviation = 1.0,
            relativeLeverageDeviationPercent = relativeLeverageDeviationPercent,
            barrierDifference = barrierDifference,
            absoluteBarrierDeviation = 1.0,
            barrierDeviationPercentOfPlannedEntry = barrierDeviationPercent
        ),
        maxRelativeLeverageDeviationPercent = maxLeverageDeviationPercent,
        maxBarrierDeviationPercentOfPlannedEntry = maxBarrierDeviationPercent
    )

    private fun assertSuccess(
        input: ExistingKnockoutProductTargetFitInput,
        expectedLeverageWithinTolerance: Boolean,
        expectedBarrierWithinTolerance: Boolean,
        expectedWithinAllTargetTolerances: Boolean
    ) {
        val result = ExistingKnockoutProductTargetFitCalculator.calculate(input)
        assertTrue(result is ExistingKnockoutProductTargetFitResult.Success)
        result as ExistingKnockoutProductTargetFitResult.Success
        assertEquals(expectedLeverageWithinTolerance, result.leverageWithinTolerance)
        assertEquals(expectedBarrierWithinTolerance, result.barrierWithinTolerance)
        assertEquals(expectedWithinAllTargetTolerances, result.withinAllTargetTolerances)
    }

    private fun assertFailure(
        input: ExistingKnockoutProductTargetFitInput,
        expectedError: ExistingKnockoutProductTargetFitError
    ) {
        val result = ExistingKnockoutProductTargetFitCalculator.calculate(input)
        assertTrue(result is ExistingKnockoutProductTargetFitResult.Failure)
        assertEquals(
            expectedError,
            (result as ExistingKnockoutProductTargetFitResult.Failure).error
        )
    }
}
