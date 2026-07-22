package de.konavigator.app.calculator

import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeCalculationEngineReferenceTest {

    @Test
    fun longStandardReferenceCalculatesExpectedValues() {
        val result = calculate(
            underlyingPrice = 110.0,
            plannedEntryPrice = 100.0,
            targetLeverage = 4.0,
            isLong = true,
            exchangeRate = 1.0,
            ratio = 0.01
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 100.0,
            expectedTargetLeverage = 4.0,
            expectedKnockoutPrice = 75.0,
            expectedCertificatePrice = 0.25,
            expectedDistanceAbsolute = 25.0,
            expectedDistancePercent = 25.0
        )
    }

    @Test
    fun longTargetLeverageThreeReferenceCalculatesExpectedValues() {
        val result = calculate(
            underlyingPrice = 100.0,
            plannedEntryPrice = 96.0,
            targetLeverage = 3.0,
            isLong = true,
            exchangeRate = 1.0,
            ratio = 0.01
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 96.0,
            expectedTargetLeverage = 3.0,
            expectedKnockoutPrice = 64.0,
            expectedCertificatePrice = 0.32,
            expectedDistanceAbsolute = 32.0,
            expectedDistancePercent = 33.333333333333336
        )
    }

    @Test
    fun longReferenceWithNonDefaultRatioCurrentlyIgnoresExchangeRate() {
        val result = calculate(
            underlyingPrice = 120.0,
            plannedEntryPrice = 110.0,
            targetLeverage = 5.0,
            isLong = true,
            exchangeRate = 1.1,
            ratio = 0.1
        )

        // Characterizes a known deviation only: FX is currently ignored, so
        // 2.20 is returned. The documented target conversion would divide the
        // raw 2.20 by 1.1. This test does not legitimize the current behavior.
        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 110.0,
            expectedTargetLeverage = 5.0,
            expectedKnockoutPrice = 88.0,
            expectedCertificatePrice = 2.20,
            expectedDistanceAbsolute = 22.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun longCurrentCentRoundingTurnsVerySmallPositiveValueIntoZero() {
        val result = calculate(
            underlyingPrice = 10.0,
            plannedEntryPrice = 10.0,
            targetLeverage = 5.0,
            isLong = true,
            exchangeRate = 1.0,
            ratio = 0.0001
        )

        // The positive raw value 2.0 * 0.0001 = 0.0002 is currently rounded
        // inside the calculator to 0.00. This is characterization, not approval.
        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 10.0,
            expectedTargetLeverage = 5.0,
            expectedKnockoutPrice = 8.0,
            expectedCertificatePrice = 0.0,
            expectedDistanceAbsolute = 2.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun shortStandardReferenceCalculatesExpectedValues() {
        val result = calculate(
            underlyingPrice = 90.0,
            plannedEntryPrice = 100.0,
            targetLeverage = 4.0,
            isLong = false,
            exchangeRate = 1.0,
            ratio = 0.01
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 100.0,
            expectedTargetLeverage = 4.0,
            expectedKnockoutPrice = 125.0,
            expectedCertificatePrice = 0.25,
            expectedDistanceAbsolute = 25.0,
            expectedDistancePercent = 25.0
        )
    }

    @Test
    fun shortTargetLeverageThreeReferenceCalculatesExpectedValues() {
        val result = calculate(
            underlyingPrice = 100.0,
            plannedEntryPrice = 90.0,
            targetLeverage = 3.0,
            isLong = false,
            exchangeRate = 1.0,
            ratio = 0.01
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 90.0,
            expectedTargetLeverage = 3.0,
            expectedKnockoutPrice = 120.0,
            expectedCertificatePrice = 0.30,
            expectedDistanceAbsolute = 30.0,
            expectedDistancePercent = 33.333333333333336
        )
    }

    @Test
    fun shortReferenceWithNonDefaultRatioCurrentlyIgnoresExchangeRate() {
        val result = calculate(
            underlyingPrice = 100.0,
            plannedEntryPrice = 110.0,
            targetLeverage = 5.0,
            isLong = false,
            exchangeRate = 1.1,
            ratio = 0.1
        )

        // Characterizes the same known deviation as the Long reference: FX is
        // ignored and 2.20 is returned. This test does not approve that behavior.
        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 110.0,
            expectedTargetLeverage = 5.0,
            expectedKnockoutPrice = 132.0,
            expectedCertificatePrice = 2.20,
            expectedDistanceAbsolute = 22.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun shortCurrentCentRoundingTurnsVerySmallPositiveValueIntoZero() {
        val result = calculate(
            underlyingPrice = 10.0,
            plannedEntryPrice = 10.0,
            targetLeverage = 5.0,
            isLong = false,
            exchangeRate = 1.0,
            ratio = 0.0001
        )

        // The positive raw value 2.0 * 0.0001 = 0.0002 is currently rounded
        // inside the calculator to 0.00. This is characterization, not approval.
        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 10.0,
            expectedTargetLeverage = 5.0,
            expectedKnockoutPrice = 12.0,
            expectedCertificatePrice = 0.0,
            expectedDistanceAbsolute = 2.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun currentlyIgnoresExchangeRateWhenAllOtherInputsMatch() {
        val withIdentityExchangeRate = calculate(exchangeRate = 1.0)
        val withDifferentExchangeRate = calculate(exchangeRate = 1.25)

        // This equality documents an open defect in the current contract. It
        // must change deliberately when the documented FX policy is implemented.
        assertEquals(withIdentityExchangeRate, withDifferentExchangeRate)
    }

    @Test
    fun currentlyAcceptsZeroRatioAndReturnsZeroModelValue() {
        val result = calculate(
            plannedEntryPrice = 100.0,
            targetLeverage = 5.0,
            ratio = 0.0
        )

        // Zero ratio is not fachlich approved. The test only records that the
        // current engine accepts it and returns a zero transitional model value.
        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 100.0,
            expectedTargetLeverage = 5.0,
            expectedKnockoutPrice = 80.0,
            expectedCertificatePrice = 0.0,
            expectedDistanceAbsolute = 20.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun extremeFiniteLongLeverageCurrentlyRoundsKnockoutToEntry() {
        val result = calculate(
            plannedEntryPrice = 100.0,
            targetLeverage = Double.MAX_VALUE,
            isLong = true
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 100.0,
            expectedTargetLeverage = Double.MAX_VALUE,
            expectedKnockoutPrice = 100.0,
            expectedCertificatePrice = 0.0,
            expectedDistanceAbsolute = 0.0,
            expectedDistancePercent = 0.0
        )
        assertEquals(100.0, result.knockoutPrice!!, 0.0)
    }

    @Test
    fun extremeFiniteShortLeverageCurrentlyRoundsKnockoutToEntry() {
        val result = calculate(
            plannedEntryPrice = 100.0,
            targetLeverage = Double.MAX_VALUE,
            isLong = false
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 100.0,
            expectedTargetLeverage = Double.MAX_VALUE,
            expectedKnockoutPrice = 100.0,
            expectedCertificatePrice = 0.0,
            expectedDistanceAbsolute = 0.0,
            expectedDistancePercent = 0.0
        )
        assertEquals(100.0, result.knockoutPrice!!, 0.0)
    }

    @Test
    fun nextUpFromOneCharacterizesFiniteLongAndShortResults() {
        val targetLeverage = Math.nextUp(1.0)
        val longResult = calculate(targetLeverage = targetLeverage, isLong = true)
        val shortResult = calculate(targetLeverage = targetLeverage, isLong = false)

        assertSuccessfulReference(
            result = longResult,
            expectedPlannedEntryPrice = 100.0,
            expectedTargetLeverage = targetLeverage,
            expectedKnockoutPrice = 2.220446049250313E-14,
            expectedCertificatePrice = 1.0,
            expectedDistanceAbsolute = 99.99999999999997,
            expectedDistancePercent = 99.99999999999997
        )
        assertSuccessfulReference(
            result = shortResult,
            expectedPlannedEntryPrice = 100.0,
            expectedTargetLeverage = targetLeverage,
            expectedKnockoutPrice = 199.99999999999997,
            expectedCertificatePrice = 1.0,
            expectedDistanceAbsolute = 99.99999999999997,
            expectedDistancePercent = 99.99999999999997
        )
        assertTrue(longResult.knockoutPrice!! > 0.0)
        assertTrue(shortResult.knockoutPrice!! < 200.0)
        assertTrue(allCalculatedValuesAreFinite(longResult))
        assertTrue(allCalculatedValuesAreFinite(shortResult))
    }

    @Test
    fun isDeterministicAndCurrentlyIndependentOfCurrentUnderlyingPrice() {
        val firstInput = input(
            underlyingPrice = 100.0,
            plannedEntryPrice = 95.0,
            targetLeverage = 5.0,
            isLong = false,
            exchangeRate = 1.0,
            ratio = 0.1
        )
        val changedCurrentPriceInput = firstInput.copy(underlyingPrice = 1_000.0)

        val first = TradeCalculationEngine.calculateTrade(firstInput)
        val repeated = TradeCalculationEngine.calculateTrade(firstInput)
        val changedCurrentPrice =
            TradeCalculationEngine.calculateTrade(changedCurrentPriceInput)

        assertEquals(first, repeated)
        // The current underlying price is used by the relation evaluator before
        // the engine. It currently has no effect on the engine mathematics.
        assertEquals(first, changedCurrentPrice)
    }

    private fun calculate(
        underlyingPrice: Double = 110.0,
        plannedEntryPrice: Double = 100.0,
        targetLeverage: Double = 5.0,
        isLong: Boolean = true,
        exchangeRate: Double = 1.0,
        ratio: Double = 0.01
    ): TradeCalculationResult = TradeCalculationEngine.calculateTrade(
        input(
            underlyingPrice = underlyingPrice,
            plannedEntryPrice = plannedEntryPrice,
            targetLeverage = targetLeverage,
            isLong = isLong,
            exchangeRate = exchangeRate,
            ratio = ratio
        )
    )

    private fun input(
        underlyingPrice: Double,
        plannedEntryPrice: Double,
        targetLeverage: Double,
        isLong: Boolean,
        exchangeRate: Double,
        ratio: Double
    ) = TradeCalculationInput(
        underlyingPrice = underlyingPrice,
        plannedEntryPrice = plannedEntryPrice,
        leverage = targetLeverage,
        isLong = isLong,
        exchangeRate = exchangeRate,
        ratio = ratio
    )

    private fun assertSuccessfulReference(
        result: TradeCalculationResult,
        expectedPlannedEntryPrice: Double,
        expectedTargetLeverage: Double,
        expectedKnockoutPrice: Double,
        expectedCertificatePrice: Double,
        expectedDistanceAbsolute: Double,
        expectedDistancePercent: Double
    ) {
        assertTrue(result.isValid)
        assertNull(result.error)
        assertEquals(expectedPlannedEntryPrice, result.underlyingPrice, 0.0)
        assertEquals(expectedTargetLeverage, result.leverage, 0.0)
        assertNotNull(result.knockoutPrice)
        assertNotNull(result.certificatePrice)
        assertNotNull(result.distanceToKnockoutAbsolute)
        assertNotNull(result.distanceToKnockoutPercent)
        assertHybridEquals(expectedKnockoutPrice, result.knockoutPrice!!)
        assertRoundedModelValueEquals(expectedCertificatePrice, result.certificatePrice!!)
        assertHybridEquals(expectedDistanceAbsolute, result.distanceToKnockoutAbsolute!!)
        assertHybridEquals(expectedDistancePercent, result.distanceToKnockoutPercent!!)
    }

    private fun assertHybridEquals(expected: Double, actual: Double) {
        val tolerance = max(
            ABSOLUTE_TOLERANCE,
            RELATIVE_TOLERANCE * abs(expected)
        )
        assertTrue(
            "Expected $expected, actual $actual, tolerance $tolerance",
            abs(actual - expected) <= tolerance
        )
    }

    private fun assertRoundedModelValueEquals(expected: Double, actual: Double) {
        assertEquals(expected, actual, ROUNDED_MODEL_VALUE_TOLERANCE)
    }

    private fun allCalculatedValuesAreFinite(result: TradeCalculationResult) =
        result.knockoutPrice!!.isFinite() &&
            result.certificatePrice!!.isFinite() &&
            result.distanceToKnockoutAbsolute!!.isFinite() &&
            result.distanceToKnockoutPercent!!.isFinite()

    private companion object {
        const val ABSOLUTE_TOLERANCE = 1e-12
        const val RELATIVE_TOLERANCE = 1e-12
        const val ROUNDED_MODEL_VALUE_TOLERANCE = 1e-12
    }
}
