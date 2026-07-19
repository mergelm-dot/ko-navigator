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

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
