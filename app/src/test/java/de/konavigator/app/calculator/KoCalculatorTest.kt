package de.konavigator.app.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KoCalculatorTest {

    @Test
    fun longStandardCaseCalculatesDirectedDistances() {
        val absoluteDistance = KoCalculator.calculateKnockoutDistanceAbsolute(
            underlyingPrice = 100.0,
            knockoutPrice = 80.0,
            isLong = true
        )
        val percentDistance = KoCalculator.calculateKnockoutDistancePercent(
            underlyingPrice = 100.0,
            knockoutPrice = 80.0,
            isLong = true
        )

        assertEquals(20.0, absoluteDistance, TOLERANCE)
        assertEquals(20.0, percentDistance, TOLERANCE)
    }

    @Test
    fun shortStandardCaseCalculatesDirectedDistances() {
        val absoluteDistance = KoCalculator.calculateKnockoutDistanceAbsolute(
            underlyingPrice = 100.0,
            knockoutPrice = 120.0,
            isLong = false
        )
        val percentDistance = KoCalculator.calculateKnockoutDistancePercent(
            underlyingPrice = 100.0,
            knockoutPrice = 120.0,
            isLong = false
        )

        assertEquals(20.0, absoluteDistance, TOLERANCE)
        assertEquals(20.0, percentDistance, TOLERANCE)
    }

    @Test
    fun longExactlyAtKnockoutReturnsZeroDistances() {
        val absoluteDistance = KoCalculator.calculateKnockoutDistanceAbsolute(
            underlyingPrice = 100.0,
            knockoutPrice = 100.0,
            isLong = true
        )
        val percentDistance = KoCalculator.calculateKnockoutDistancePercent(
            underlyingPrice = 100.0,
            knockoutPrice = 100.0,
            isLong = true
        )

        assertEquals(0.0, absoluteDistance, TOLERANCE)
        assertEquals(0.0, percentDistance, TOLERANCE)
    }

    @Test
    fun shortExactlyAtKnockoutReturnsZeroDistances() {
        val absoluteDistance = KoCalculator.calculateKnockoutDistanceAbsolute(
            underlyingPrice = 100.0,
            knockoutPrice = 100.0,
            isLong = false
        )
        val percentDistance = KoCalculator.calculateKnockoutDistancePercent(
            underlyingPrice = 100.0,
            knockoutPrice = 100.0,
            isLong = false
        )

        assertEquals(0.0, absoluteDistance, TOLERANCE)
        assertEquals(0.0, percentDistance, TOLERANCE)
    }

    @Test
    fun longBelowKnockoutPreservesNegativeDistances() {
        val absoluteDistance = KoCalculator.calculateKnockoutDistanceAbsolute(
            underlyingPrice = 90.0,
            knockoutPrice = 100.0,
            isLong = true
        )
        val percentDistance = KoCalculator.calculateKnockoutDistancePercent(
            underlyingPrice = 90.0,
            knockoutPrice = 100.0,
            isLong = true
        )

        assertEquals(-10.0, absoluteDistance, TOLERANCE)
        assertEquals(-10.0 / 90.0 * 100.0, percentDistance, TOLERANCE)
        assertTrue(percentDistance < 0.0)
    }

    @Test
    fun shortAboveKnockoutPreservesNegativeDistances() {
        val absoluteDistance = KoCalculator.calculateKnockoutDistanceAbsolute(
            underlyingPrice = 110.0,
            knockoutPrice = 100.0,
            isLong = false
        )
        val percentDistance = KoCalculator.calculateKnockoutDistancePercent(
            underlyingPrice = 110.0,
            knockoutPrice = 100.0,
            isLong = false
        )

        assertEquals(-10.0, absoluteDistance, TOLERANCE)
        assertEquals(-10.0 / 110.0 * 100.0, percentDistance, TOLERANCE)
        assertTrue(percentDistance < 0.0)
    }

    @Test
    fun precisionCasePreservesMoreThanTwoDecimalPlaces() {
        val absoluteDistance = KoCalculator.calculateKnockoutDistanceAbsolute(
            underlyingPrice = 100.0,
            knockoutPrice = 66.666,
            isLong = true
        )
        val percentDistance = KoCalculator.calculateKnockoutDistancePercent(
            underlyingPrice = 100.0,
            knockoutPrice = 66.666,
            isLong = true
        )

        assertEquals(33.334, absoluteDistance, TOLERANCE)
        assertEquals(33.334, percentDistance, TOLERANCE)
    }

    @Test
    fun currentTransitionalPriceCalculationReturnsRoundedLongKnockoutDifference() {
        val price = KoCalculator.calculateCertificatePrice(
            underlyingPrice = 100.0,
            knockoutPrice = 80.0,
            ratio = 0.01,
            isLong = true
        )

        assertEquals(0.20, price, TOLERANCE)
    }

    @Test
    fun currentTransitionalPriceCalculationReturnsRoundedShortKnockoutDifference() {
        val price = KoCalculator.calculateCertificatePrice(
            underlyingPrice = 100.0,
            knockoutPrice = 120.0,
            ratio = 0.01,
            isLong = false
        )

        assertEquals(0.20, price, TOLERANCE)
    }

    @Test
    fun currentTransitionalPriceCalculationReturnsZeroAtLongBarrier() {
        val price = KoCalculator.calculateCertificatePrice(
            underlyingPrice = 100.0,
            knockoutPrice = 100.0,
            ratio = 0.01,
            isLong = true
        )

        assertEquals(0.0, price, TOLERANCE)
    }

    @Test
    fun currentTransitionalPriceCalculationReturnsZeroAtShortBarrier() {
        val price = KoCalculator.calculateCertificatePrice(
            underlyingPrice = 100.0,
            knockoutPrice = 100.0,
            ratio = 0.01,
            isLong = false
        )

        assertEquals(0.0, price, TOLERANCE)
    }

    @Test
    fun currentTransitionalPriceCalculationReturnsZeroForLongOnKnockedOutSide() {
        val price = KoCalculator.calculateCertificatePrice(
            underlyingPrice = 90.0,
            knockoutPrice = 100.0,
            ratio = 0.01,
            isLong = true
        )

        assertEquals(0.0, price, TOLERANCE)
    }

    @Test
    fun currentTransitionalPriceCalculationReturnsZeroForShortOnKnockedOutSide() {
        val price = KoCalculator.calculateCertificatePrice(
            underlyingPrice = 110.0,
            knockoutPrice = 100.0,
            ratio = 0.01,
            isLong = false
        )

        assertEquals(0.0, price, TOLERANCE)
    }

    @Test
    fun currentTransitionalPriceCalculationRoundsToTwoDecimalPlaces() {
        val price = KoCalculator.calculateCertificatePrice(
            underlyingPrice = 100.0,
            knockoutPrice = 66.666,
            ratio = 0.01,
            isLong = true
        )

        assertEquals(0.33, price, TOLERANCE)
    }

    @Test
    fun currentTransitionalPriceCalculationAppliesRatioExactlyOnce() {
        val price = KoCalculator.calculateCertificatePrice(
            underlyingPrice = 100.0,
            knockoutPrice = 80.0,
            ratio = 0.1,
            isLong = true
        )

        assertEquals(2.0, price, TOLERANCE)
    }

    @Test
    fun longIntrinsicValueInTheMoneyUsesBasePrice() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 100.0,
            basePrice = 80.0,
            ratio = 0.01,
            isLong = true
        )

        assertEquals(0.20, intrinsicValue, TOLERANCE)
    }

    @Test
    fun longIntrinsicValueAtBasePriceIsZero() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 100.0,
            basePrice = 100.0,
            ratio = 0.01,
            isLong = true
        )

        assertEquals(0.0, intrinsicValue, TOLERANCE)
    }

    @Test
    fun longIntrinsicValueBelowBasePriceIsZero() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 90.0,
            basePrice = 100.0,
            ratio = 0.01,
            isLong = true
        )

        assertEquals(0.0, intrinsicValue, TOLERANCE)
    }

    @Test
    fun shortIntrinsicValueInTheMoneyUsesBasePrice() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 100.0,
            basePrice = 120.0,
            ratio = 0.01,
            isLong = false
        )

        assertEquals(0.20, intrinsicValue, TOLERANCE)
    }

    @Test
    fun shortIntrinsicValueAtBasePriceIsZero() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 100.0,
            basePrice = 100.0,
            ratio = 0.01,
            isLong = false
        )

        assertEquals(0.0, intrinsicValue, TOLERANCE)
    }

    @Test
    fun shortIntrinsicValueAboveBasePriceIsZero() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 110.0,
            basePrice = 100.0,
            ratio = 0.01,
            isLong = false
        )

        assertEquals(0.0, intrinsicValue, TOLERANCE)
    }

    @Test
    fun intrinsicValueUsesSeparateBasePriceInsteadOfKnockoutBarrier() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 100.0,
            basePrice = 78.0,
            ratio = 0.1,
            isLong = true
        )

        assertEquals(2.2, intrinsicValue, TOLERANCE)
    }

    @Test
    fun intrinsicValueAppliesRatioZeroPointZeroOne() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 150.0,
            basePrice = 100.0,
            ratio = 0.01,
            isLong = true
        )

        assertEquals(0.5, intrinsicValue, TOLERANCE)
    }

    @Test
    fun intrinsicValueAppliesRatioExactlyOnce() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 100.0,
            basePrice = 80.0,
            ratio = 0.1,
            isLong = true
        )

        assertEquals(2.0, intrinsicValue, TOLERANCE)
    }

    @Test
    fun intrinsicValuePreservesPrecisionWithoutInternalRounding() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 100.0,
            basePrice = 66.6666,
            ratio = 0.01,
            isLong = true
        )

        assertEquals(0.333334, intrinsicValue, TOLERANCE)
        assertTrue(kotlin.math.abs(intrinsicValue - 0.33) > TOLERANCE)
    }

    @Test
    fun intrinsicValueCalculatesVeryLargeFiniteResult() {
        val intrinsicValue = KoCalculator.calculateIntrinsicValue(
            underlyingPrice = 1.0e150,
            basePrice = 5.0e149,
            ratio = 0.1,
            isLong = true
        )

        assertTrue(intrinsicValue.isFinite())
        assertEquals(5.0e148, intrinsicValue, LARGE_VALUE_TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-9
        const val LARGE_VALUE_TOLERANCE = 1e138
    }
}
